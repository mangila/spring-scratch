package com.github.mangila.movie.web.view;

import com.github.mangila.movie.persistence.movie.projection.MovieProjection;
import com.github.mangila.movie.service.MovieService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class MovieGridView extends VerticalLayout {

	public MovieGridView(MovieService movieService) {
		Grid<MovieProjection> grid = new Grid<>(MovieProjection.class);
		grid.setColumns("id", "name", "genres", "releaseDate", "budget");
		grid.setItems(movieService.findAllProjections());
		add(new H1("Movie Management"), grid);
	}

}
