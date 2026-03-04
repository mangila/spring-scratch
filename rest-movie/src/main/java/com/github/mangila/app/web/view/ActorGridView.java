package com.github.mangila.app.web.view;

import com.github.mangila.app.actor.persistence.projection.ActorProjection;
import com.github.mangila.app.actor.service.ActorService;
import com.github.mangila.app.shared.VaadinEventPublisher;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route("actor")
public class ActorGridView extends VerticalLayout {

	private static final Logger log = LoggerFactory.getLogger(ActorGridView.class);

	private final ActorService actorService;

	private final VaadinEventPublisher vaadinEventPublisher;

	private UI ui;

	private Registration registration;

	public ActorGridView(ActorService actorService, VaadinEventPublisher vaadinEventPublisher) {
		this.actorService = actorService;
		this.vaadinEventPublisher = vaadinEventPublisher;
		Grid<ActorProjection> grid = new Grid<>(ActorProjection.class);
		grid.setItemsPageable(actorService::findAllProjections);
		grid.setPageSize(20);
		add(new H1("Actor Management"), grid);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		this.ui = attachEvent.getUI();
		this.registration = vaadinEventPublisher.register(s -> {
			log.info("Received {}", s);
			ui.access(() -> Notification.show(s).setPosition(Notification.Position.BOTTOM_END));
		});
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		super.onDetach(detachEvent);
		this.registration.remove();
		this.registration = null;
		this.ui = null;
	}

}
