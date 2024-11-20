package cloud.sonam.s3.file.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public interface Thumbnail {
    static final Logger LOG = LoggerFactory.getLogger(Thumbnail.class);

    Mono<ByteArrayOutputStream> getByteArrayOutputStream(final URL presignedUrl, Dimension dimension, final String format);

    static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
        LOG.debug("get scaled dimension for thumbnail");

        int originalWidth = imgSize.width;
        int originalHeight = imgSize.height;
        int bound_width = boundary.width;
        int bound_height = boundary.height;
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // first check if we need to scale width
        if (originalWidth > bound_width) {
            //scale width to fit
            newWidth = bound_width;
            //scale height to maintain aspect ratio
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        // then check if we need to scale even with the new height
        if (newHeight > bound_height) {
            //scale height to fit instead
            newHeight = bound_height;
            //scale width to maintain aspect ratio
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new Dimension(newWidth, newHeight);
    }
}
