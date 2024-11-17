package cloud.sonam.s3.file;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.awt.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.OptionalLong;

public interface S3Service {
    // generic upload file
    //This method will upload video file to s3 bucket
    //Inbound: file in byteBuffer flux with filename, format and length
    //Outbound: This method will return the filekey after storing the file into s3 bucket
    //example outbound: s3-rest-service/videos/2022-05-20T21:22:56.184297.mp4
    Mono<String> uploadFile(Flux<ByteBuffer> byteBufferFlux, String prefixPath, String fileName, String format,
                            OptionalLong length, ObjectCannedACL acl);
    Mono<String> createPhotoThumbnail(URL presignedUrl, final String uploadType, ObjectCannedACL acl, Dimension thumbnail);
    Mono<String> createGif(URL presignedUrl, final String prefixPath, ObjectCannedACL acl);
    Mono<URL> createPresignedUrl(Mono<String> fileKeyMono);
}
