package com.github.mangila.movie.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OutboxRelayService {

	@Scheduled(fixedDelay = 1000)
	public void relay() {
//		UI.getCurrent().access(() -> {
//			Notification notification = Notification.show("Financial report generated");
//		});
	}

}
