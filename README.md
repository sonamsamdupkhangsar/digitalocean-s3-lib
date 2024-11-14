# digitalocean-s3-lib
This is a s3 library for uploading objects to s3 bucket, get presigned url and create thumbnail.  This Java library uses Spring WebFlux and is built and tested for the DigitalOcean Spaces ("S3 Compatible").

## How to use this library

Clone this repository or get a tagged release.

To build

```shell
./gradlew clean build
```

Publish to local Maven repository:
```shell
./gradlew publishToMavenLocal
```


### Add this project as dependency
For Gradle projects add to your settings file
```
implementation("cloud.sonam:digitalocean-s3-lib:1.0.0-SNAPSHOT")
```

or for Maven add dependency as:
```
 <groupId>cloud.sonam</groupId>
 <artifactId>digitalocean-s3-lib</artifactId>
 <version>1.0.0-SNAPSHOT</version>
```
### Define the properties for the S3 storage
```
aws:
  s3:
    region: nycregion
    accessKeyId: my_key
    secretAccessKey: my_secretkey
    bucket: mybucket
    endpoint: https://sonam.cloud/endpoint
    subdomain: https://www.sonam.cloud
    videoPath: videos/
    photoPath: photos/
    filePath: files/
    imageAclHeader: x-amz-acl
    imageAclValue: public-read
    presignDurationInMinutes: 60
```

The main properties are:

    `region` where the bucket is hosted
    `accessKeyId` access key id secret
    `secretAccessKey` secret access key value
    `bucket` this resolves to a folder name that you can add as a path
    `endpoint` this would be the s3 endpoint
    `subdomain` this can be your custom subdomain name like `https://spaces.yourdomain.com` as example

### Java Api usage
You can directly use the `S3ServiceHandler.class` or use the `S3WebRequestHandler.class` interface.  Like the name implies the S3WebRequestHandler can take the ServerRequest object and extract the required fields to upload file. 

The required fields for uploading using a web request can be seen in the `S3RestServiceMockTest.class`:
```
   client.post().uri("/upload?uploadType=video")
                .header("filename", video.getFilename())
                .header("format", "video/mp4")
                .header(HttpHeaders.CONTENT_LENGTH, ""+video.contentLength())
                .bodyValue(video)
```