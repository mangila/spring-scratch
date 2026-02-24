package io.github.mangila.spring1.pokemon.internal.data;

import io.github.mangila.spring1.pokemon.internal.integration.S3Integration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

@Component
@Order(value = 1)
public class BucketSeed implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(BucketSeed.class);

  private final S3Integration s3Integration;

  public BucketSeed(S3Integration s3Integration) {
    this.s3Integration = s3Integration;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!s3Integration.bucketExists()) {
      final CreateBucketResponse response = s3Integration.createBucket();
      LOGGER.info("Bucket created - {}", response);
    }
  }
}
