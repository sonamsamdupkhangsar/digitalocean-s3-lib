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
    thumbnailSize:
      width: 100
      height: 100    
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

For a video file upload which will also create a small thumbnail (currently the animation is not working :-())
The required fields for uploading using a web request can be seen in the `S3RestServiceTest.class`:
```
MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", video);

client.post().uri("/upload?uploadType=video&folder="+ date+"&thumbnailWidth=200&thumbnailHeight=200")
    .contentType(MediaType.MULTIPART_FORM_DATA)
    .body(BodyInserters.fromMultipartData(body))
    .header("acl", "private")
    .exchange().expectStatus().isOk()
    .expectBody(String.class)
```

The following is an example of an upload for photo with thumbnail dimension in query param:
```
MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", langurPhoto);

client.post().uri("/upload?uploadType=photo&folder="+ LocalDate.now()+"&thumbnailWidth=200&thumbnailHeight=200")
    .header("acl", "private")
    .body(BodyInserters.fromMultipartData(body))
    .exchange().expectStatus().isOk()
    .expectBody(String.class)
```

The `acl` header value will control whether to set the object(photo/video/file) to public read access or private only.  For private objects, you will require a presigned url to access them.
This value is used from the package of `software.amazon.awssdk.services.s3.model`:
```
public enum ObjectCannedACL {
    PRIVATE("private"),
    PUBLIC_READ("public-read"),
    PUBLIC_READ_WRITE("public-read-write"),
    AUTHENTICATED_READ("authenticated-read"),
    AWS_EXEC_READ("aws-exec-read"),
    BUCKET_OWNER_READ("bucket-owner-read"),
    BUCKET_OWNER_FULL_CONTROL("bucket-owner-full-control"),
    UNKNOWN_TO_SDK_VERSION((String)null);
```

I have only tested with `private` and `public-read` only. 