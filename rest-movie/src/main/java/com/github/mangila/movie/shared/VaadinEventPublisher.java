package com.github.mangila.movie.shared;

import com.vaadin.flow.shared.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Component
public class VaadinEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(VaadinEventPublisher.class);

	private final ConcurrentLinkedQueue<Consumer<String>> listeners = new ConcurrentLinkedQueue<>();

	private final SimpleAsyncTaskExecutor executor;

	public VaadinEventPublisher(@Qualifier("applicationTaskExecutor") SimpleAsyncTaskExecutor executor) {
		this.executor = executor;
	}

	public Registration register(Consumer<String> listener) {
		listeners.add(listener);
		return () -> listeners.remove(listener);
	}

	public void broadcast(String message) {
		for (Consumer<String> listener : listeners) {
			executor.execute(() -> listener.accept(message));
		}
	}

}
