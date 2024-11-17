package cloud.sonam.s3;

import cloud.sonam.s3.config.S3ClientConfigurationProperties;
import org.junit.Assert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


/**
 * This will run the actual s3 service that will upload a file to a live s3 bucket.
 * The tests are commented out for that reason.
 */

@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class S3RestServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(S3RestServiceTest.class);

    @Autowired
    private WebTestClient client;

    @Value("classpath:mydogsleeping.mp4")
    private Resource video;

    @Value("classpath:langur.jpg")
    private Resource langurPhoto;


    private S3AsyncClient s3Client;

    @Autowired
    private S3ClientConfigurationProperties s3ClientConfigurationProperties;

    @Test
    public void hello() {
        assertThat("hello").isEqualTo("hello");
    }

   // @Test
    public void uploadVideoFile() throws IOException, InterruptedException {
        LOG.info("video: {}", video);
        Assert.assertNotNull(video);
        LOG.info("video contentLength: {}, video: {}", video.contentLength(), video);
        Assert.assertTrue(video.getFile().exists());

        client = client.mutate().responseTimeout(Duration.ofSeconds(10)).build();
        final String date = LocalDate.now().toString();

        client.post().uri("/upload?uploadType=video&folder="+ date)
                .header("filename", video.getFilename())
                .header("format", "video/mp4")
                .header("acl", "private")
                .header(HttpHeaders.CONTENT_LENGTH, ""+video.contentLength())
                .bodyValue(video)
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
    }

    //@Test
    public void uploadPhotoFile() throws IOException, InterruptedException {
        LOG.info("photo: {}", langurPhoto);
        Assert.assertNotNull(langurPhoto);

        LOG.info("langur photo contentLength: {}", langurPhoto.contentLength());
        Assert.assertTrue(langurPhoto.getFile().exists());

        client = client.mutate().responseTimeout(Duration.ofSeconds(10)).build();

        client.post().uri("/upload?uploadType=photo&folder="+ LocalDate.now()+"&thumbnailWidth=200&thumbnailHeight=200")
                .header("filename", langurPhoto.getFilename())
                .header("format", "image/jpg")
                .header("acl", "private")
                .header(HttpHeaders.CONTENT_LENGTH, ""+langurPhoto.contentLength())
                .bodyValue(langurPhoto)
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
    }

    //@Test
    public void uploadFile() throws IOException, InterruptedException {
        LOG.info("photo: {}", langurPhoto);
        Assert.assertNotNull(langurPhoto);

        LOG.info("langur photo contentLength: {}", langurPhoto.contentLength());
        Assert.assertTrue(langurPhoto.getFile().exists());

        client = client.mutate().responseTimeout(Duration.ofSeconds(10)).build();

        client.post().uri("/upload?uploadType=file&folder="+ LocalDate.now())
                .header("filename", langurPhoto.getFilename())
                .header("format", "image/jpg")
                .header("acl", "private")
                .header(HttpHeaders.CONTENT_LENGTH, ""+langurPhoto.contentLength())
                .bodyValue(langurPhoto)
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
    }

    //@Test
    public void getPresignUrl() {
        LOG.info("create presign url");

        client.post().uri("/presignurl").bodyValue("videoapp/1/video/2022-06-13T11:23:44.893698.mp4")
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("presignUrl: {}", stringEntityExchangeResult.getResponseBody()));

    }
}
