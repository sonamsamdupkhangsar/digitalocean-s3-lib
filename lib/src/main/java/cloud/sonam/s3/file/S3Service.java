package cloud.sonam.s3.file;

import org.springframework.cglib.core.Local;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.awt.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.OptionalLong;

public interface S3Service {
    // generic upload file
    //This method will upload video file to s3 bucket
    //Inbound: file in byteBuffer flux with filename, format and length
    //Outbound: This method will return the filekey after storing the file into s3 bucket
    //example outbound: s3-rest-service/videos/2022-05-20T21:22:56.184297.mp4
    Mono<String> uploadFile(Flux<ByteBuffer> byteBufferFlux, String prefixPath, String fileName, String format,
                            long length, ObjectCannedACL acl, LocalDateTime localDateTime);
    Mono<String> createPhotoThumbnail(LocalDateTime localDateTime, final URL presignedUrl,
                                      final String prefixPath, ObjectCannedACL acl,
                                      final String fileName, String format,
                                      Dimension thumbnail);
    //LocalDateTime localDateTime, URL presignedUrl, final String uploadType, ObjectCannedACL acl, Dimension thumbnail);
    Mono<String> createGif(LocalDateTime localDateTime, final URL presignedUrl,
                           final String prefixPath, ObjectCannedACL acl,
                           final String fileName, String format,
                           Dimension thumbnail);//LocalDateTime localDateTime, URL presignedUrl, final String prefixPath, ObjectCannedACL acl, Dimension thumbnail);
    Mono<URL> createPresignedUrl(Mono<String> fileKeyMono);
}
