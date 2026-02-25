package com.github.mangila.movie;

import org.springframework.boot.SpringApplication;

public class TestRestMovieApplication {

	public static void main(String[] args) {
		SpringApplication.from(RestMovieApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
