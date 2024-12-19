package cloud.sonam.s3;

import cloud.sonam.s3.config.S3ClientConfigurationProperties;
import cloud.sonam.s3.file.S3FileUploadService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * This will test the Router endpoints for uploading files, create thumbnail and get presign url
 */

@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class S3RestServiceMockTest {
    private static final Logger LOG = LoggerFactory.getLogger(S3RestServiceMockTest.class);

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

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", video);

        client.post().uri("/upload?uploadType=video")
                .header("acl", "private")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
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

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", langurPhoto);

        client.post().uri("/upload?uploadType=photo")
                .header("acl", "private")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
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

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", langurPhoto);

        client.post().uri("/upload?uploadType=file")
                .header("acl", "private")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
    }


    @Test
    public void getPresignUrl() {
        LOG.info("create presign url");

        client.post().uri("/presignurl").bodyValue("videoapp/1/video/2022-06-13T11:23:44.893698.mp4")
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("presignUrl: {}", stringEntityExchangeResult.getResponseBody()));

    }

    @Test
    public void deleteObject() {
        LOG.info("delete object by key");

        final String key = "videoapp/1/video/2022-06-13T11:23:44.893698.mp4";

        String keyUrlEncoded = URLEncoder.encode("videoapp/1/video/2022-06-13T11:23:44.893698.mp4", Charset.defaultCharset());
        LOG.info("keyUrlEncoded: {}", keyUrlEncoded);

        DeleteObjectResponse deleteObjectResponse = Mockito.mock(DeleteObjectResponse.class);
        SdkHttpResponse sdkHttpResponse = Mockito.mock(SdkHttpResponse.class);
        when(deleteObjectResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);

        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        Mockito.when(s3Client.deleteObject(Mockito.any(DeleteObjectRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteObjectResponse));

        client.delete().uri("/s3/object?key="+key)
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("presignUrl: {}", stringEntityExchangeResult.getResponseBody()));

    }

    @Test
    public void deleteFolder() {
        LOG.info("delete object by key");

        final String prefix = "videoapp/1/video/";

        String keyUrlEncoded = URLEncoder.encode("videoapp/1/video/2022-06-13T11:23:44.893698.mp4", Charset.defaultCharset());
        LOG.info("keyUrlEncoded: {}", keyUrlEncoded);

        ListObjectsResponse listObjectsResponse = Mockito.mock(ListObjectsResponse.class);
        SdkHttpResponse sdkHttpResponse = Mockito.mock(SdkHttpResponse.class);
        when(listObjectsResponse.sdkHttpResponse()).thenReturn(sdkHttpResponse);

        when(sdkHttpResponse.isSuccessful()).thenReturn(true);

        Mockito.when(s3Client.listObjects(Mockito.any(ListObjectsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(listObjectsResponse));

        client.delete().uri("/s3/folder?prefix="+prefix)
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("presignUrl: {}", stringEntityExchangeResult.getResponseBody()));

    }
}
