package cloud.sonam.s3.file;

import cloud.sonam.s3.file.util.Thumbnail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Service
public class PhotoThumbnail implements Thumbnail {
    private static final Logger LOG = LoggerFactory.getLogger(PhotoThumbnail.class);

    @Override
    public Mono<ByteArrayOutputStream> getByteArrayOutputStream(final URL presignedUrl, Dimension thumbnail, final String format) {
        try {
            String[] formatArray = format.split("/");
            String formatName   = "jpg";

            if (formatArray.length == 2) {
                LOG.info("file extension used will be {}", formatArray[1]);
                formatName = formatArray[1];
            }
            else {
                LOG.info("invalid file extension, assuming jpg");
            }

            InputStream inputStream = presignedUrl.openStream();

            // Load the original image
            BufferedImage originalImage = ImageIO.read(inputStream);

            Dimension dimension = Thumbnail.getScaledDimension(new Dimension(originalImage.getWidth(), originalImage.getHeight()),
                    thumbnail);

            int width = (int) dimension.getWidth();
            int height = (int) dimension.getHeight();
            LOG.info("scaled width: {} height: {}", width, height);

            // Create a new image for the thumbnail
            BufferedImage thumbnailImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // Get the graphics context of the thumbnail image
            Graphics2D graphics = thumbnailImage.createGraphics();

            // Set rendering hints for better quality
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the original image onto the thumbnail image, scaling it to fit
            graphics.drawImage(originalImage, 0, 0, width, height, null);
            graphics.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnailImage, formatName, baos);

            LOG.info("Thumbnail created successfully.");

            return Mono.just(baos);

        } catch (IOException e) {
            LOG.error("failed to create thumbnail for photo", e);
            return Mono.error(e);
        }
    }

}
