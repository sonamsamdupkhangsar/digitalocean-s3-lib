package cloud.sonam.s3.file;

import cloud.sonam.s3.config.S3ClientConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

@Service
public class S3Handler implements S3WebRequestHandler, S3ServiceHandler {
    private static final Logger LOG = LoggerFactory.getLogger(S3Handler.class);

    private final S3Service s3Service;

    private final S3ClientConfigurationProperties s3ClientConfigurationProperties;

    public S3Handler(S3Service s3Service, S3ClientConfigurationProperties s3ClientConfigurationProperties) {
        this.s3Service = s3Service;
        this.s3ClientConfigurationProperties = s3ClientConfigurationProperties;
    }


    public Mono<ServerResponse> upload(ServerRequest serverRequest) {
        String folder = "";

        if (serverRequest.queryParam("folder").isPresent()) {
            folder = serverRequest.queryParam("folder").get() + "/";
            LOG.info("user specified a additional path/folder name: {}", folder);
        }
        Flux<ByteBuffer> byteBufferFlux = serverRequest.body(BodyExtractors.toFlux(ByteBuffer.class));
        final String fileName = serverRequest.headers().firstHeader("filename");
        final String format = serverRequest.headers().firstHeader("format");
        final OptionalLong optionalLong = serverRequest.headers().contentLength();
        final Optional<String> optionalUploadType = serverRequest.queryParam("uploadType");
        if (optionalUploadType.isEmpty()) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).bodyValue("upload type not specified");
        }
        final String uploadType = optionalUploadType.get();

        ObjectCannedACL acl = ObjectCannedACL.PRIVATE;

        String aclValue = serverRequest.headers().firstHeader("acl");
        if (aclValue == null) {
            LOG.warn("no acl value supplied, set to private by default");
        }
        else {
            acl = ObjectCannedACL.fromValue(aclValue);
        }

        Dimension thumbnailDimension = new Dimension(100, 100);

        if (serverRequest.queryParam("thumbnailWidth").isPresent()) {
            int width = Integer.parseInt(serverRequest.queryParam("thumbnailWidth").get());
            int height =  Integer.parseInt(serverRequest.queryParam("thumbnailHeight").get());
            thumbnailDimension.setSize(width, height);
            LOG.info("use width and height from query param, width: {}, height: {}", width, height);
        }

        return upload( byteBufferFlux, uploadType, fileName, format, optionalLong, folder, acl, thumbnailDimension);
    }

    public Mono<ServerResponse> upload(Flux<ByteBuffer> byteBufferFlux, final String uploadType, final String fileName,
                                       final String format, final OptionalLong fileContentLength, final String folder,
                                       ObjectCannedACL acl, Dimension thumbnail) {
        LOG.info("upload file of type: {}", uploadType);

        if (uploadType.equalsIgnoreCase("video") || uploadType.equalsIgnoreCase("photo")) {

            if (uploadType.equals("video")) {
                final String prefixPath = s3ClientConfigurationProperties.getVideoPath() + folder;

                return s3Service.uploadFile(byteBufferFlux, prefixPath, fileName, format, fileContentLength, acl)
                    .doOnNext(s -> LOG.info("Video upload done, creating video thumbnail next."))
                        .flatMap(fileKey -> s3Service.createPresignedUrl(Mono.just(fileKey)))
                        .doOnNext(presignedUrl -> LOG.info("presigned url: {}", presignedUrl))
                        .flatMap(presigneUrl -> s3Service.createGif(presigneUrl, prefixPath, acl))
                        .doOnNext(s -> LOG.info("Video thumbnail done."))
                        .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                        .onErrorResume(throwable -> ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(throwable.getMessage()));
            }
            else {
                final String prefixPath = s3ClientConfigurationProperties.getPhotoPath() + folder;

                return s3Service.uploadFile(byteBufferFlux, prefixPath, fileName, format, fileContentLength, acl)
                        .doOnNext(s -> LOG.info("photo upload done, creating photo thumbnail next."))
                        .flatMap(fileKey -> s3Service.createPresignedUrl(Mono.just(fileKey)))
                        .doOnNext(presignedUrl -> LOG.info("presigned url: {}", presignedUrl))
                        .flatMap(fileKey -> s3Service.createPhotoThumbnail(fileKey, prefixPath, acl, thumbnail))
                        .doOnNext(s -> LOG.info("Photo thumbnail done."))
                        .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                        .onErrorResume(throwable -> ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(throwable.getMessage()));
            }
        }
        else if (uploadType.equalsIgnoreCase("file")) {
            String prefixPath = s3ClientConfigurationProperties.getFilePath();

            return s3Service.uploadFile(byteBufferFlux, prefixPath, fileName, format, fileContentLength, acl)
                    .doOnNext(s -> LOG.info("file upload done."))
                    .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                    .onErrorResume(throwable -> ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(throwable.getMessage()));
        }
        else {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).bodyValue("upload type invalid '"+ uploadType+"'");
        }
    }

    /**
     * this is a wrapper method to extract the filekey using a serverRequest object.
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getPresignUrl(ServerRequest serverRequest) {
        LOG.info("get presignurl");

        return getPresignUrl(serverRequest.body(BodyExtractors.toMono(String.class)));
    }

    public Mono<ServerResponse> getPresignUrl(Mono<String> fileKeyMono) {
        LOG.info("get presignurl");

        return s3Service.createPresignedUrl(fileKeyMono)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(throwable.getMessage()));
    }

}
