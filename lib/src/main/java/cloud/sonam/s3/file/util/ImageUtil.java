package cloud.sonam.s3.file.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.awt.*;

public class ImageUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ImageUtil.class);

    public static String getFileFormat(MediaType contentType, String filename) {
        // Use contentType or filename to determine the file format
        // For example:
        if (contentType != null && contentType.getType().equals("image")) {
            return contentType.getSubtype(); // e.g., "jpeg", "png", etc.
        } else {
            // Use filename extension if contentType is not reliable
            String[] parts = filename.split("\\.");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        return "unknown"; // Default if format can't be determined
    }


    public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
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
