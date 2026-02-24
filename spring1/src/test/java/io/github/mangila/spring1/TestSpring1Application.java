package io.github.mangila.spring1;

import org.springframework.boot.SpringApplication;

public class TestSpring1Application {

  public static void main(String[] args) {
    SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(args);
  }
}
