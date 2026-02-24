package io.github.mangila.spring2;

import org.springframework.boot.SpringApplication;

public class TestSpring2Application {

  public static void main(String[] args) {
    SpringApplication.from(Spring2Application::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
