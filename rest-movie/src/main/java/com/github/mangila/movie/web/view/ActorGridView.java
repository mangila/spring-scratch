package com.github.mangila.movie.web.view;

import com.github.mangila.movie.persistence.actor.projection.ActorProjection;
import com.github.mangila.movie.service.ActorService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("actor")
public class ActorGridView extends VerticalLayout {

    public ActorGridView(ActorService actorService) {
        Grid<ActorProjection> grid = new Grid<>(ActorProjection.class);
        grid.setColumns("id", "name", "picture", "bio");
        grid.setItemsPageable(actorService::findAllProjections);
        grid.setPageSize(20);
        add(new H1("Actor Management"), grid);
    }

}
