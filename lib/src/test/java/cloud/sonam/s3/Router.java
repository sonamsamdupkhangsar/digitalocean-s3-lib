package cloud.sonam.s3;

import cloud.sonam.s3.file.S3Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Router for s3 service
 */
@Configuration
public class Router {
    private static final Logger LOG = LoggerFactory.getLogger(Router.class);

    @Bean
    public RouterFunction<ServerResponse> route(S3Handler handler) {
        LOG.info("building router function");
        return RouterFunctions.route(POST("/upload").and(accept(MediaType.APPLICATION_JSON)),
                handler::upload)
                .andRoute(POST("/presignurl").and(accept(MediaType.APPLICATION_JSON)),
                        handler::getPresignUrl)
                .andRoute(DELETE("/s3/object"), handler::deleteObject)
                .andRoute(DELETE("/s3/folder"), handler::deleteByPrefix);
    }
}
