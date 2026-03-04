package com.github.mangila.app.shared.chaos;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.Random;

@Aspect
public class ChaosAspect {

	private final Random random = new Random();

	@Before("@annotation(chaos)")
	public void causeChaos(Chaos chaos) {
		if (random.nextDouble() < chaos.probability()) {
			throw new RuntimeException(chaos.message());
		}
	}

}
