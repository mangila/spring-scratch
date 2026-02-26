package com.github.mangila.movie.web.view;

import com.github.mangila.movie.persistence.actor.ActorProjection;
import com.github.mangila.movie.service.ActorService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class ActorView extends VerticalLayout {

	public ActorView(ActorService actorService) {
		Grid<ActorProjection> grid = new Grid<>(ActorProjection.class);
		grid.setColumns("id", "name", "picture", "bio");
		grid.setItems(actorService.findAllProjections());
		add(new H1("Actor Management"), grid);
	}

}
