package cloud.sonam.s3.file;

import cloud.sonam.s3.config.S3ClientConfigurationProperties;
import cloud.sonam.s3.file.util.ImageUtil;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.util.List;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    public Mono<String> uploadFile(Flux<ByteBuffer> body, String prefixPath, String fileName, MediaType mediaType,
                                   long length, ObjectCannedACL acl, LocalDateTime localDateTime) {
        LOG.info("uploadFile with fileName: {}", fileName);
        String extension = ImageUtil.getFileFormat(mediaType, fileName);


        String fileKey = prefixPath+localDateTime + "." + extension;
        LOG.info("extension: {}, fileKey: {}, mediaType.toString: {}", extension, fileKey, mediaType.toString());

        LOG.debug("accessKeyId: {}, secretAccessKey: {}, endpoint: {}, region: {}, bucket: {}",
                s3config.getAccessKeyId(), s3config.getSecretAccessKey(),
                s3config.getEndpoint(), s3config.getRegion(), s3config.getBucket());

        LOG.info("length: {}", length);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Length", ""+length);
        metadata.put("Content-Type",mediaType.toString());
        metadata.put("x-amz-acl", acl.toString());

        LOG.info("s3Client: {}", s3client);

        CompletableFuture future = s3client
                .putObject(PutObjectRequest.builder()
                                .bucket(s3config.getBucket())
                                .contentLength(length)
                                .key(fileKey)
                                .contentType(mediaType.toString())
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
                                             final String fileName, MediaType mediaType,
                                             Dimension thumbnail) {
        LOG.info("Create thumbnail for photo presignedUrl: {}", presignedUrl);

        Mono<ByteArrayOutputStream> byteArrayOutputStreamMono = photoThumbnail.getByteArrayOutputStream(presignedUrl, thumbnail, mediaType);

        final String thumbnailPrefixPath = prefixPath + "thumbnail/";

        return byteArrayOutputStreamMono.flatMap(byteArrayOutputStream -> {
            byte[] bytes = byteArrayOutputStream.toByteArray();
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            Flux<ByteBuffer> byteBufferFlux = Flux.just(byteBuffer);
            return uploadFile(byteBufferFlux, thumbnailPrefixPath, fileName, mediaType, byteBuffer.capacity(), acl, localDateTime);
        });
    }


    @Override
    public Mono<String> createGif(LocalDateTime localDateTime, URL presignedUrl, final String prefixPath,
                                 ObjectCannedACL acl, final String fileName, MediaType mediaType,
                                  Dimension dimension) {
        try {
            Mono<ByteArrayOutputStream> byteArrayOutputStreamMono = gifThumbnail.getByteArrayOutputStream(presignedUrl, dimension, mediaType);
            final String thumbnailPrefixPath = prefixPath + "thumbnail/";

            return byteArrayOutputStreamMono.flatMap(byteArrayOutputStream -> {
                byte[] bytes = byteArrayOutputStream.toByteArray();
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                Flux<ByteBuffer> byteBufferFlux = Flux.just(byteBuffer);

                return uploadFile(byteBufferFlux, thumbnailPrefixPath, fileName, MediaType.IMAGE_JPEG, bytes.length, acl, localDateTime);
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

    /**
     * this will delete all objects in a prefix.
     * If the prefix contains path or folder like object then it will also delete them.
     * @param prefix
     * @return
     */
    @Override
    public Mono<String> deleteFolder(String prefix) {
        LOG.info("delete s3 using a prefix {}", prefix);

        CompletableFuture<ListObjectsResponse> listObjectsResponseCompletableFuture =
                s3client.listObjects(ListObjectsRequest.builder().bucket(s3config.getBucket()).prefix(prefix).build());

        return Mono.fromFuture(listObjectsResponseCompletableFuture).map(response -> {
            LOG.info("contents: {}", response.contents());

            List<S3Object> list = response.contents();
            for (S3Object s3Object : list) {
                LOG.info("found s3 object");
                deleteObject(s3Object.key()).doOnNext(s -> LOG.info("response for delete is {}", s));
            }
            return "deleted "+ list.size() + " objects";
        });
    }

    @Override
    public Mono<String> deleteObject(String key) {

        CompletableFuture<DeleteObjectResponse> cf = s3client.deleteObject
                (DeleteObjectRequest.builder().bucket(s3config.getBucket()).key(key).build());


        return Mono.fromFuture(cf).map(response -> {
            try {
                DeleteObjectResponse deleteObjectResponse = cf.get();
                LOG.debug("deleteObjectResponse status is {}", deleteObjectResponse.sdkHttpResponse().statusCode());

                if (deleteObjectResponse.sdkHttpResponse() == null || !deleteObjectResponse.sdkHttpResponse().isSuccessful()) {
                    LOG.error("failed to delete object with key: {}, status: {}",
                            key, deleteObjectResponse.sdkHttpResponse().statusCode());
                    return "Failed to delete object with key: " + key;
                }
                LOG.info("object deleted successfully using key: {}", key);
                return key;
            }
            catch (InterruptedException | ExecutionException e) {
                LOG.info("exception occured on deleting with key: "+ key, e);
                LOG.error("failed to delete object with key {}, exception occured: {}", key, e.getMessage());
                return "Failed to to delete object with key: "+ e.getMessage();
            }
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
