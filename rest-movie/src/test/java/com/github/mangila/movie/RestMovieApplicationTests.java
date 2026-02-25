package com.github.mangila.movie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RestMovieApplicationTests {

	@Test
	void contextLoads() {
	}

}
