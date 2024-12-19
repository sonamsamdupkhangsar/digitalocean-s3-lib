package cloud.sonam.s3.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

@Configuration
@ConfigurationProperties(prefix = "aws.s3")

public class S3ClientConfigurationProperties {
    private static final Logger LOG = LoggerFactory.getLogger(S3ClientConfigurationProperties.class);


    private Region region;// = Region.of("https://sfo2.digitaloceanspaces.com");
    private URI endpoint = null;

    private String regionUrl;
    private String accessKeyId;
    private String secretAccessKey;
    private URI subdomain;

    // this resolves to a folder name, not the actual s3 bucket (confusing ?)
    private String bucket;
    private String rootPath;
    private String videoPath;
    private String photoPath;
    private String filePath;

    private String fileAclHeader;
    private String fileAclValue;

    private int presignDurationInMinutes;
    private ThumbnailSize thumbnailSize;

    // AWS S3 requires that file parts must have at least 5MB, except
    // for the last part. This may change for other S3-compatible services, so let't
    // define a configuration property for that
    private int multipartMinPartSize = 5*1024*1024;

    public Region getRegion() {
        if (this.region == null) {
            LOG.info("regionUrl: {}", regionUrl);
            region = Region.of(regionUrl);
        }
        else {
            LOG.info("region set already: {}", region.toString());
        }
        return region;
    }
    public void setRegionUrl(String regionUrl) {
        this.regionUrl = regionUrl;
    }

    public String getRegionUrl() {
        return this.regionUrl;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public URI getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(URI subdomain) {
        this.subdomain = subdomain;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public int getMultipartMinPartSize() {
        return multipartMinPartSize;
    }

    public void setMultipartMinPartSize(int multipartMinPartSize) {
        this.multipartMinPartSize = multipartMinPartSize;
    }

    public String getFileAclHeader() {
        return fileAclHeader;
    }

    public void setFileAclHeader(String fileAclHeader) {
        this.fileAclHeader = fileAclHeader;
    }

    public String getFileAclValue() {
        return fileAclValue;
    }

    public void setFileAclValue(String fileAclValue) {
        this.fileAclValue = fileAclValue;
    }

    public int getPresignDurationInMinutes() {
        return this.presignDurationInMinutes;
    }

    public void setPresignDurationInMinutes(int presignDurationInMinutes) {
        this.presignDurationInMinutes = presignDurationInMinutes;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public ThumbnailSize getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(ThumbnailSize thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public static class ThumbnailSize {
        private int width;
        private int height;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }
}


