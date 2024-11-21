package cloud.sonam.s3.file;

import cloud.sonam.s3.file.util.ImageUtil;
import com.madgag.gif.fmsware.AnimatedGifEncoder;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

@Service
public class GifThumbnail implements Thumbnail {
    private static final Logger LOG = LoggerFactory.getLogger(GifThumbnail.class);

    @Override
    public Mono<ByteArrayOutputStream> getByteArrayOutputStream(URL presignedUrl, Dimension dimension, MediaType mediaType) {
        try {
            LOG.info("open pre-signed url stream");
            InputStream inputStream = presignedUrl.openStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            getGifBytes(inputStream, 0, 2, 2, 2, byteArrayOutputStream, dimension);//);
            LOG.info("returning byteArrayOutputStream for gif");
            return Mono.just(byteArrayOutputStream);

        }
        catch (Exception e) {
            LOG.error("exception occurred", e);
            return Mono.error(e);
        }
    }

    private void getGifBytes(InputStream inputStream, int startFrame, int frameCount, Integer frameRate, Integer margin,
                             OutputStream outputStream, Dimension thumbnail) {
        try {
            LOG.info("create gif");

            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(inputStream);
            frameGrabber.start();
            Java2DFrameConverter fc = new Java2DFrameConverter();

            Integer videoLength = frameGrabber.getLengthInFrames();
            // If the user uploads the video to be extremely short and does not meet the value interval defined by the user, the acquisition starts at 1/5 and ends at 1/2
            if (startFrame > videoLength || (startFrame + frameCount) > videoLength) {
                startFrame = videoLength / 5;
                frameCount = videoLength / 2;
            }
            LOG.debug("startFrame: {}, frameRate: {}", startFrame, frameRate);

            LOG.debug("frameGrabber.width: {} frameGrabber.height: {}", frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
            Dimension dimension = ImageUtil.getScaledDimension(new Dimension(frameGrabber.getImageWidth(),
                            frameGrabber.getImageHeight()),
                    thumbnail);

            int width = (int) dimension.getWidth();
            int height = (int) dimension.getHeight();

            frameGrabber.setFrameNumber(startFrame);
            frameGrabber.setImageHeight(height);
            frameGrabber.setImageWidth(width);

            LOG.info("thumbnail.height: {}, thumbnail.width: {}", thumbnail.getHeight(), thumbnail.getWidth());
            AnimatedGifEncoder en = new AnimatedGifEncoder();
            en.setFrameRate(frameRate);
            en.start(outputStream);

            for (int i = 0; i < frameCount; i++) {
                Frame frame = frameGrabber.grabFrame(false, true, true, false);
                LOG.info("frame: {}", frame);

                BufferedImage bufferedImage = fc.convert(frame);
                LOG.info("bufferedImage: {}", bufferedImage);

                if (bufferedImage != null) {
                    en.addFrame(bufferedImage);
                    frameGrabber.setFrameNumber(frameGrabber.getFrameNumber() + margin);

                }
            }
            en.finish();

            frameGrabber.stop();
            frameGrabber.close();
        } catch (Exception e) {
            LOG.error("failed to create gif for video", e);
        }
    }

}
