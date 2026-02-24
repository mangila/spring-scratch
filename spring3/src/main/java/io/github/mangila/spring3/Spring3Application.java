package io.github.mangila.spring3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class Spring3Application {

  public static void main(String[] args) {
    SpringApplication.run(Spring3Application.class, args);
  }
}
