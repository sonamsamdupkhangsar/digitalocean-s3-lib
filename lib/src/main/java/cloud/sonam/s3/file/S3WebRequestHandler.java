package cloud.sonam.s3.file;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * This interface is for handling web request
 */
public interface S3WebRequestHandler {
    Mono<ServerResponse> upload(ServerRequest serverRequest);
    Mono<ServerResponse> getPresignUrl(ServerRequest serverRequest);
}
