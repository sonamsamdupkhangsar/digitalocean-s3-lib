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

    public S3FileUploadService(S3AsyncClient s3client, S3ClientConfigurationProperties s3config, AwsCredentialsProvider awsCredentialsProvider, S3Presigner s3Presigner) {
        this.s3client = s3client;
        this.s3config = s3config;
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.s3Presigner = s3Presigner;
    }

    @PreDestroy
    public void closePresigner() {
        LOG.info("close s3Presigner");
        s3Presigner.close();
    }

    public Mono<String> uploadFile(Flux<ByteBuffer> body, String prefixPath, String fileName, String format, OptionalLong optionalLong) {
            LOG.info("uploadVideo with filePart");
            LocalDateTime localDateTime = LocalDateTime.now();
            String extension = "";

            if (fileName.contains(".")) {
                extension = fileName
                        .substring(fileName.lastIndexOf(".") + 1);
            }
            LOG.info("header.filename: {}", fileName);


            String fileKey = prefixPath+localDateTime + "." + extension;

            LOG.debug("accessKeyId: {}, secretAccessKey: {}, endpoint: {}, region: {}, bucket: {}",
                    s3config.getAccessKeyId(), s3config.getSecretAccessKey(),
                    s3config.getEndpoint(), s3config.getRegion(), s3config.getBucket());

            long length = optionalLong.getAsLong();
            LOG.info("length: {}", length);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Length", ""+length);
            metadata.put("Content-Type",format);
            metadata.put("x-amz-acl", "public-read");

            LOG.info("s3Client: {}", s3client);

            CompletableFuture future = s3client
                    .putObject(PutObjectRequest.builder()
                                    .bucket(s3config.getBucket())
                                    .contentLength(length)
                                    .key(fileKey)
                                    .contentType(format)
                                    .metadata(metadata)
                                    .acl(ObjectCannedACL.PUBLIC_READ)
                                    .build(),
                            AsyncRequestBody.fromPublisher(body));
            return Mono.fromFuture(future).map(response -> {
                checkResult(response);

                LOG.info("check response and returning fileKey: {}", fileKey);
                return fileKey;
            });
    }

    @Override
    public Mono<String> createPhotoThumbnail(final URL presignedUrl, final String prefixPath) {
        LOG.info("Create thumbnail for photo presignedUrl: {}", presignedUrl);
        LocalDateTime localDateTime = LocalDateTime.now();

        ByteArrayOutputStream byteArrayOutputStream = getPhotoByteArrayOutputStream(presignedUrl);

        if (byteArrayOutputStream == null) {
            LOG.error("byteArrayOutputStream is null from getPhotoByteArrayOutputStream call");
            return Mono.just("failed to create thumbnail for photo");
        }

        String thumbKey = prefixPath + "thumbnail/" + localDateTime + "." + "png";
        return saveContentBytesToS3(byteArrayOutputStream, thumbKey);

    }

    private Mono<String> saveContentBytesToS3(ByteArrayOutputStream byteArrayOutputStream, final String fileKey) {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Length", "" + bytes.length);
        metadata.put("Content-Type", "image/png");
        metadata.put("x-amz-acl", "public-read");

        LOG.info("saving thumbnail with key: {}", fileKey);
        CompletableFuture future = s3client
                .putObject(PutObjectRequest.builder()
                                .bucket(s3config.getBucket())
                                .contentLength((long) bytes.length)
                                .key(fileKey)
                                .contentType("image/png")
                                .metadata(metadata)
                                .acl(ObjectCannedACL.PUBLIC_READ)
                                .build(),
                        AsyncRequestBody.fromPublisher(Flux.just(byteBuffer)));

        return Mono.fromFuture(future).map(response -> {
            checkResult(response);

            LOG.info("checked thumbnail response and returning fileKey: {}", response.toString());

            return fileKey;
        });
    }

    @Override
    public Mono<String> createGif(URL presignedUrl, final String prefixPath) {
        try {
            InputStream inputStream = presignedUrl.openStream();//new URL(s3config.getSubdomain() + fileKey).openStream();
            File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".gif");

            getGifBytes(inputStream, 0, 2, 2, 2, new FileOutputStream(tempFile));//);

            LocalDateTime localDateTime = LocalDateTime.now();

            String gifKey = prefixPath + "gif/" + localDateTime + "." + "gif";
            Map<String, String> metadata2 = new HashMap<>();
            metadata2.put("Content-Length", "" + tempFile.length());
            metadata2.put("Content-Type", "image/gif");
            metadata2.put("x-amz-acl", "public-read");

            LOG.info("saving thumbnail with key: {}", gifKey);

            CompletableFuture future = s3client
                    .putObject(PutObjectRequest.builder()
                                    .bucket(s3config.getBucket())
                                    .contentLength(tempFile.length())
                                    .key(gifKey)
                                    .contentType("image/gif")
                                    .metadata(metadata2)
                                    .acl(ObjectCannedACL.PUBLIC_READ)
                                    .build(),
                           tempFile.toPath());
                           // AsyncRequestBody.fromPublisher(Flux.just(byteBuffer)));
            LOG.info("future: {}", future);

            if (future == null) {
                LOG.warn("future is null, happens during test code because we are using a tempFile.toPath instead.");
                return Mono.just(gifKey);
            }
            else {
                return Mono.fromFuture(future).map(response -> {
                    checkResult(response);

                    LOG.info("checked gifKey response and returning gifKey: {}", response.toString());

                    return gifKey;
                });
            }

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

    public ByteArrayOutputStream getVideoByteArrayOutputStream(String fileKey, String imageFormat)  {

        try {
            URI uri = s3config.getSubdomain().resolve("/" + fileKey);

            InputStream inputStream = uri.toURL().openStream();
            return getThumbnailBytes(inputStream, imageFormat);
        }
        catch (Exception e) {
            LOG.error("exception occured", e);
            return null;
        }
    }

    private ByteArrayOutputStream getThumbnailBytes(InputStream inputStream, String imageFormat) {
        try {
            LOG.info("getting ByteArrayOutputStream for inpustreamm imageFormat: {}", imageFormat);

            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(inputStream);
            frameGrabber.start();
            Java2DFrameConverter fc = new Java2DFrameConverter();
            Frame frame = frameGrabber.grabKeyFrame();
            LOG.info("frame: {}", frame);

            BufferedImage bufferedImage = fc.convert(frame);
            LOG.info("bufferedImage: {}", bufferedImage);

            int i = 0;
            if (bufferedImage != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, imageFormat, baos);

                frame = frameGrabber.grabKeyFrame();
                bufferedImage = fc.convert(frame);

                frameGrabber.stop();
                frameGrabber.close();

                LOG.info("i: {}, bytearray.length: {}", i++, baos.toByteArray().length);
                return baos;

            }
            else {
                frameGrabber.stop();
                frameGrabber.close();
            }

            LOG.info("thumbnail done");
        } catch (Exception e) {
            LOG.error("failed to create thumbnail for video", e);
        }

        return null;
    }

    private ByteArrayOutputStream getPhotoByteArrayOutputStream(final URL presignedUrl) {
        try {
            InputStream inputStream = presignedUrl.openStream();
            // Load the original image
            BufferedImage originalImage = ImageIO.read(inputStream);

            // Set the thumbnail size
            int thumbnailWidth = 100;
            int thumbnailHeight = 100;

            // Create a new image for the thumbnail
            BufferedImage thumbnailImage = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);

            // Get the graphics context of the thumbnail image
            Graphics2D graphics = thumbnailImage.createGraphics();

            // Set rendering hints for better quality
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the original image onto the thumbnail image, scaling it to fit
            graphics.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
            graphics.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnailImage, "jpg", baos);

            LOG.info("Thumbnail created successfully.");

            return baos;

        } catch (IOException e) {
            LOG.error("failed to create thumbnail for photo", e);
            return null;
        }
    }

    private void getGifBytes(InputStream inputStream, int startFrame, int frameCount, Integer frameRate, Integer margin, OutputStream outputStream) {
        try {
            LOG.info("create gif");

            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(inputStream);
            frameGrabber.start();
            Java2DFrameConverter fc = new Java2DFrameConverter();

            Integer videoLength = frameGrabber.getLengthInFrames();
            // If the user uploads the video to be extremely short and does not meet the value interval defined by the user, the acquisition starts at 1/5 and ends at 1/2
            if (startFrame > videoLength || (startFrame + frameCount) > videoLength) {
                startFrame = videoLength / 5;
                frameCount = videoLength / 2;
            }
            LOG.info("startFrame: {}, frameRate: {}", startFrame, frameRate);

            frameGrabber.setFrameNumber(startFrame);
            AnimatedGifEncoder en = new AnimatedGifEncoder();
            en.setFrameRate(frameRate);
            en.start(outputStream);

           for (int i = 0; i < frameCount; i++) {
                Frame frame = frameGrabber.grabFrame(false, true, true, false);
                LOG.info("frame: {}", frame);

                BufferedImage bufferedImage = fc.convert(frame);
                LOG.info("bufferedImage: {}", bufferedImage);

                if (bufferedImage != null) {
                    en.addFrame(bufferedImage);
                    frameGrabber.setFrameNumber(frameGrabber.getFrameNumber() + margin);

                }
            }
            en.finish();

            frameGrabber.stop();
            frameGrabber.close();
        } catch (Exception e) {
            LOG.error("failed to create gif for video", e);
        }
    }

    public Mono<String> createVideoThumbnail(String fileKey, final String prefixPath) {
        LOG.info("Create thumbnail for video fileKey: {}", fileKey);
        LocalDateTime localDateTime = LocalDateTime.now();

        ByteArrayOutputStream byteArrayOutputStream = getVideoByteArrayOutputStream(fileKey, "png");

        String thumbKey = prefixPath + "thumbnail/" + localDateTime + "." + "png";
        return saveContentBytesToS3(byteArrayOutputStream, thumbKey);
    }

}
