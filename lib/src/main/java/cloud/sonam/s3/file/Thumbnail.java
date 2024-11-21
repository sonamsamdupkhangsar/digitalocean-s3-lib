package cloud.sonam.s3.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public interface Thumbnail {
    static final Logger LOG = LoggerFactory.getLogger(Thumbnail.class);

    Mono<ByteArrayOutputStream> getByteArrayOutputStream(final URL presignedUrl, Dimension dimension, final MediaType format);

}
