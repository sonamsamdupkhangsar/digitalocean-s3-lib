package cloud.sonam.s3.file;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import jakarta.annotation.PreDestroy;
import cloud.sonam.s3.config.S3ClientConfigurationProperties;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handler
 */
@Component
public class S3FileUploadService implements S3Service {
    private static final Logger LOG = LoggerFactory.getLogger(S3FileUploadService.class);

    private S3AsyncClient s3client;

    private S3ClientConfigurationProperties s3config;

    private AwsCredentialsProvider awsCredentialsProvider;

    private S3Presigner s3Presigner;

    private GifThumbnail gifThumbnail;
    private PhotoThumbnail photoThumbnail;

    public S3FileUploadService(S3AsyncClient s3client, S3ClientConfigurationProperties s3config,
                               AwsCredentialsProvider awsCredentialsProvider, S3Presigner s3Presigner,
                               GifThumbnail gifThumbnail, PhotoThumbnail photoThumbnail) {
        this.s3client = s3client;
        this.s3config = s3config;
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.s3Presigner = s3Presigner;
        this.gifThumbnail = gifThumbnail;
        this.photoThumbnail = photoThumbnail;
    }

    @PreDestroy
    public void closePresigner() {
        LOG.info("close s3Presigner");
        s3Presigner.close();
    }

    public Mono<String> uploadFile(Flux<ByteBuffer> body, String prefixPath, String fileName, String format,
                                   long length, ObjectCannedACL acl, LocalDateTime localDateTime) {
        LOG.info("uploadFile with fileName: {}", fileName);
        String extension = "";

        if (fileName.contains(".")) {
            extension = fileName
                    .substring(fileName.lastIndexOf(".") + 1);
        }

        String fileKey = prefixPath+localDateTime + "." + extension;

        LOG.debug("accessKeyId: {}, secretAccessKey: {}, endpoint: {}, region: {}, bucket: {}",
                s3config.getAccessKeyId(), s3config.getSecretAccessKey(),
                s3config.getEndpoint(), s3config.getRegion(), s3config.getBucket());

        LOG.info("length: {}", length);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Length", ""+length);
        metadata.put("Content-Type",format);
        metadata.put("x-amz-acl", acl.toString());

        LOG.info("s3Client: {}", s3client);

        CompletableFuture future = s3client
                .putObject(PutObjectRequest.builder()
                                .bucket(s3config.getBucket())
                                .contentLength(length)
                                .key(fileKey)
                                .contentType(format)
                                .metadata(metadata)
                                .acl(acl)
                                .build(),
                        AsyncRequestBody.fromPublisher(body));
        return Mono.fromFuture(future).map(response -> {
            checkResult(response);

            LOG.info("check response and returning fileKey: {}", fileKey);
            return fileKey;
        });
    }

    @Override
    public Mono<String> createPhotoThumbnail(LocalDateTime localDateTime, final URL presignedUrl,
                                             final String prefixPath, ObjectCannedACL acl,
                                             final String fileName, String format,
                                             Dimension thumbnail) {
        LOG.info("Create thumbnail for photo presignedUrl: {}", presignedUrl);

        Mono<ByteArrayOutputStream> byteArrayOutputStreamMono = photoThumbnail.getByteArrayOutputStream(presignedUrl, thumbnail, format);

        final String thumbnailPrefixPath = prefixPath + "thumbnail/";

        return byteArrayOutputStreamMono.flatMap(byteArrayOutputStream -> {
            byte[] bytes = byteArrayOutputStream.toByteArray();
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            Flux<ByteBuffer> byteBufferFlux = Flux.just(byteBuffer);
            return uploadFile(byteBufferFlux, thumbnailPrefixPath, fileName, format, byteBuffer.capacity(), acl, localDateTime);
        });
    }


    @Override
    public Mono<String> createGif(LocalDateTime localDateTime, URL presignedUrl, final String prefixPath,
                                 ObjectCannedACL acl, final String fileName, String format,
                                  Dimension dimension) {
        try {
            Mono<ByteArrayOutputStream> byteArrayOutputStreamMono = gifThumbnail.getByteArrayOutputStream(presignedUrl, dimension, format);
            final String thumbnailPrefixPath = prefixPath + "thumbnail/";

            return byteArrayOutputStreamMono.flatMap(byteArrayOutputStream -> {
                byte[] bytes = byteArrayOutputStream.toByteArray();
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                Flux<ByteBuffer> byteBufferFlux = Flux.just(byteBuffer);
                return uploadFile(byteBufferFlux, thumbnailPrefixPath, fileName, format, bytes.length, acl, localDateTime);
            });
        }
        catch (Exception e) {
            LOG.error("exception occured", e);
            return Mono.just(e.getLocalizedMessage());
        }



    }

    @Override
    public Mono<URL> createPresignedUrl(Mono<String> fileKeyMono) {
        LOG.info("create presignurl for key");
        LOG.info("s3Client: {}", s3client);

        return fileKeyMono.flatMap(fileKey -> {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(s3config.getBucket()).key(fileKey).build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder().
                    signatureDuration(Duration.ofMinutes(s3config.getPresignDurationInMinutes()))
                    .getObjectRequest(getObjectRequest).build();

            PresignedGetObjectRequest presignedGetObjectRequest =
                    s3Presigner.presignGetObject(getObjectPresignRequest);

            LOG.info("Presigned URL: {}", presignedGetObjectRequest.url());
            return Mono.just(presignedGetObjectRequest.url());
        });
    }


    private PutObjectResponse checkResult(Object result1) {
        PutObjectResponse result = (PutObjectResponse) result1;
        LOG.info("response.sdkHttpResponse: {}", result.sdkHttpResponse().isSuccessful());

        if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
            LOG.error("response is un successful");
            throw new RuntimeException("sdkHttpResponse fail");
        }
        return result;
    }


}
