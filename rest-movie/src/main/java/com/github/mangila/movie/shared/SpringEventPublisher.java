package com.github.mangila.movie.shared;

import com.github.mangila.movie.persistence.actor.projection.ActorProjection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class SpringEventPublisher {

	private final ApplicationEventPublisher publisher;

	public SpringEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void publish() {
		publisher.publishEvent(new ActorProjection(UUID.randomUUID(), "John Doe", URI.create("http://localhost:8080"),
				"", LocalDate.EPOCH));
	}

}
