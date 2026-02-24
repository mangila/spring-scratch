package io.github.mangila.spring1.pokemon.internal.integration;

import io.github.mangila.spring1.pokemon.internal.config.RustFsConfig;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3Integration {

  private final String bucket;
  private final S3Client s3Client;

  public S3Integration(RustFsConfig rustFsConfig) {
    this.bucket = rustFsConfig.getBucket();
    final AwsBasicCredentials credentials =
        AwsBasicCredentials.create(
            rustFsConfig.getUsername(), rustFsConfig.getPassword().getValue());
    final S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build();
    this.s3Client =
        S3Client.builder()
            .region(Region.EU_CENTRAL_1)
            .endpointOverride(rustFsConfig.getUrl())
            .serviceConfiguration(s3Configuration)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
  }

  public boolean bucketExists() {
    final HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucket).build();
    try {
      s3Client.headBucket(headBucketRequest);
      return true;
    } catch (final S3Exception e) {
      return false;
    }
  }

  public CreateBucketResponse createBucket() {
    final CreateBucketRequest createBucketRequest =
        CreateBucketRequest.builder().bucket(bucket).build();
    return s3Client.createBucket(createBucketRequest);
  }

  public byte[] getObject(String key) {
    final GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucket).key(key).build();
    return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
  }

  public PutObjectResponse putObject(String key, RequestBody body) {
    final String contentDisposition = "attachment; filename=\"%s\"".formatted(key);
    final PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(MediaType.IMAGE_PNG_VALUE)
            .contentDisposition(contentDisposition)
            .build();
    final PutObjectResponse response = s3Client.putObject(putObjectRequest, body);
    return response;
  }
}
