package io.github.mangila.spring3;

import io.github.mangila.spring3.sse.model.SseSubscription;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@RegisterReflectionForBinding({SseSubscription.class})
@ConfigurationPropertiesScan
@SpringBootApplication
public class Spring3Application {

  public static void main(String[] args) {
    SpringApplication.run(Spring3Application.class, args);
  }
}
