package cloud.sonam.s3;

import cloud.sonam.s3.config.S3ClientConfigurationProperties;
import cloud.sonam.s3.file.S3FileUploadService;
import cloud.sonam.s3.file.S3Handler;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * This will test the S3Handler methods for uploading file, creating thumbnail and get presign url.
 */

@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class S3HandlerTest {
    private static final Logger LOG = LoggerFactory.getLogger(S3HandlerTest.class);

    @Autowired
    private WebTestClient client;

    @Value("classpath:mydogsleeping.mp4")
    private Resource video;

    @Value("classpath:langur.jpg")
    private Resource langurPhoto;

    @MockBean
    private S3AsyncClient s3Client;

    @Autowired
    private S3ClientConfigurationProperties s3ClientConfigurationProperties;

    @SpyBean
    private S3FileUploadService s3Service;

    @Autowired
    private S3Handler s3Handler;

    @Test
    public void uploadVideoFile() throws IOException, InterruptedException {
        LOG.info("video: {}", video);
        Assert.assertNotNull(video);
        LOG.info("video contentLength: {}, video: {}", video.contentLength(), video);
        Assert.assertTrue(video.getFile().exists());

        client = client.mutate().responseTimeout(Duration.ofSeconds(10)).build();

        PutObjectResponse putObjectResponse = Mockito.mock(PutObjectResponse.class);
        SdkHttpResponse sdkHttpResponse = Mockito.mock(SdkHttpResponse.class);
        when(putObjectResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);

        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class),
                AsyncRequestBody.fromPublisher(Mockito.any())))
                .thenReturn(CompletableFuture.completedFuture(putObjectResponse));


        URI mockUri = Mockito.mock(URI.class);
        s3ClientConfigurationProperties.setSubdomain(mockUri);
        when(mockUri.resolve(any(String.class))).thenReturn(mockUri);
        URL mockUrl = Mockito.mock(URL.class);

        //send the video inputstream when inputStream is request from mockUrl
        when(mockUrl.openStream()).thenReturn(this.video.getInputStream());

        Mockito.doReturn(Mono.just(video.getURL())).when(s3Service).createPresignedUrl(Mockito.any(Mono.class));

        Flux<ByteBuffer> byteBufferFlux = Flux.just(ByteBuffer.wrap(video.getContentAsByteArray()));

        final String folder = ""; // no specific folder just use default one
        Mono<ServerResponse> serverResponseMono = s3Handler.upload(byteBufferFlux, "video", video.getFilename(), MediaType.valueOf("video/mp4"),
                video.contentLength(), folder, ObjectCannedACL.PRIVATE, new Dimension(100, 100));

        StepVerifier.create(serverResponseMono).expectNextMatches(serverResponse -> {
            assertTrue(serverResponse.statusCode().is2xxSuccessful());
            LOG.info("verified response is ok");
            return true;
        }).verifyComplete();
    }

    @Test
    public void uploadPhotoFile() throws IOException, InterruptedException {
        LOG.info("photo: {}", langurPhoto);
        Assert.assertNotNull(langurPhoto);

        LOG.info("langur photo contentLength: {}", langurPhoto.contentLength());
        Assert.assertTrue(langurPhoto.getFile().exists());

        client = client.mutate().responseTimeout(Duration.ofSeconds(10)).build();

        PutObjectResponse putObjectResponse = Mockito.mock(PutObjectResponse.class);
        SdkHttpResponse sdkHttpResponse = Mockito.mock(SdkHttpResponse.class);
        when(putObjectResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);

        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class),
                        AsyncRequestBody.fromPublisher(Mockito.any())))
                .thenReturn(CompletableFuture.completedFuture(putObjectResponse));


        URI mockUri = Mockito.mock(URI.class);
        s3ClientConfigurationProperties.setSubdomain(mockUri);
        when(mockUri.resolve(any(String.class))).thenReturn(mockUri);
        URL mockUrl = Mockito.mock(URL.class);

        when(mockUri.toURL()).thenReturn(mockUrl);

        //send the langurPhoto inputstream when inputStream is request from mockUrl
        //when(mockUrl.openStream()).thenReturn(this.langurPhoto.getInputStream());

        Mockito.doReturn(Mono.just(langurPhoto.getURL())).when(s3Service).createPresignedUrl(Mockito.any(Mono.class));


        Flux<ByteBuffer> byteBufferFlux = Flux.just(ByteBuffer.wrap(langurPhoto.getContentAsByteArray()));

        final String folder = ""; // no specific folder just use default one
        Mono<ServerResponse> serverResponseMono = s3Handler.upload(byteBufferFlux, "photo", langurPhoto.getFilename(),
                MediaType.valueOf("image/jpg"), langurPhoto.contentLength(), folder, ObjectCannedACL.PRIVATE,new Dimension(100, 100));

        StepVerifier.create(serverResponseMono).expectNextMatches(serverResponse -> {
            assertTrue(serverResponse.statusCode().is2xxSuccessful());
            LOG.info("verified response is ok");
            return true;
        }).verifyComplete();
    }

    @Test
    public void uploadFile() throws IOException, InterruptedException {
        LOG.info("photo: {}", langurPhoto);
        Assert.assertNotNull(langurPhoto);

        LOG.info("langur photo contentLength: {}", langurPhoto.contentLength());
        Assert.assertTrue(langurPhoto.getFile().exists());

        client = client.mutate().responseTimeout(Duration.ofSeconds(10)).build();

        PutObjectResponse putObjectResponse = Mockito.mock(PutObjectResponse.class);
        SdkHttpResponse sdkHttpResponse = Mockito.mock(SdkHttpResponse.class);
        when(putObjectResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);

        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class),
                        AsyncRequestBody.fromPublisher(Mockito.any())))
                .thenReturn(CompletableFuture.completedFuture(putObjectResponse));


        URI mockUri = Mockito.mock(URI.class);
        s3ClientConfigurationProperties.setSubdomain(mockUri);
        when(mockUri.resolve(any(String.class))).thenReturn(mockUri);
        URL mockUrl = Mockito.mock(URL.class);

        when(mockUri.toURL()).thenReturn(mockUrl);

        //send the langurPhoto inputstream when inputStream is request from mockUrl
        when(mockUrl.openStream()).thenReturn(this.langurPhoto.getInputStream());


        Flux<ByteBuffer> byteBufferFlux = Flux.just(ByteBuffer.wrap(langurPhoto.getContentAsByteArray()));

        final String folder = ""; // no specific folder just use default one
        Mono<ServerResponse> serverResponseMono = s3Handler.upload(byteBufferFlux, "file", langurPhoto.getFilename(),
                MediaType.valueOf("image/jpg"), langurPhoto.contentLength(), folder, ObjectCannedACL.PRIVATE, null);

        StepVerifier.create(serverResponseMono).expectNextMatches(serverResponse -> {
            assertTrue(serverResponse.statusCode().is2xxSuccessful());
            LOG.info("verified response is ok");
            return true;
        }).verifyComplete();
    }


    @Test
    public void getPresignUrl() {
        LOG.info("create presign url");

        LOG.info("thumbnailHeight: {}, thumbnailWidth: {}",
                s3ClientConfigurationProperties.getThumbnailSize().getHeight(),
                s3ClientConfigurationProperties.getThumbnailSize().getWidth());
        Mono<ServerResponse> serverResponseMono = s3Handler.getPresignUrl(Mono.just("videoapp/1/video/2022-06-13T11:23:44.893698.mp4"));

        StepVerifier.create(serverResponseMono).expectNextMatches(serverResponse -> {
            assertTrue(serverResponse.statusCode().is2xxSuccessful());
            LOG.info("verified response is ok");
            return true;
        }).verifyComplete();
    }

}
