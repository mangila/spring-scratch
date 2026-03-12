package com.github.mangila.app.web.view;

import com.github.mangila.app.config.VaadinAppConfig;
import com.github.mangila.app.movie.persistance.projection.MovieDetailsProjection;
import com.github.mangila.app.movie.service.MovieService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle(VaadinAppConfig.PAGE_TITLE_PREFIX + "Movie")
@Route("movie")
public class MovieGridView extends VerticalLayout {

	public MovieGridView(MovieService movieService) {
		Grid<MovieDetailsProjection> grid = new Grid<>(MovieDetailsProjection.class);
		grid.setItems(movieService.findAllProjections());
		add(new H1("Movie Management"), grid);
	}

}
