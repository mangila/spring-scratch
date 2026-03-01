package com.github.mangila.movie.web.view;

import com.github.mangila.movie.persistence.director.projection.DirectorProjection;
import com.github.mangila.movie.service.DirectorService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("director")
public class DirectorGridView extends VerticalLayout {

	public DirectorGridView(DirectorService directorService) {
		Grid<DirectorProjection> grid = new Grid<>(DirectorProjection.class);
		grid.setColumns("id", "name", "picture", "bio");
		grid.setItems(directorService.findAllProjections());
		add(new H1("Director Management"), grid);
	}

}
