package com.github.mangila.movie.web.view;

import com.github.mangila.movie.persistence.director.projection.DirectorProjection;
import com.github.mangila.movie.service.DirectorService;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.IconRenderer;
import com.vaadin.flow.router.Route;

@Route("director")
public class DirectorGridView extends VerticalLayout {

    public DirectorGridView(DirectorService directorService) {
        Grid<DirectorProjection> grid = new Grid<>();
        grid.addColumn(iconRenderer())
                .setHeader("Director")
                .setFlexGrow(0);
        grid.addColumn(DirectorProjection::name)
                .setHeader("Name")
                .setSortable(true);
        grid.addColumn(DirectorProjection::name)
                .setHeader("Date of Birth")
                .setSortable(true);
        grid.addColumn(DirectorProjection::bio)
                .setHeader("Bio");


        grid.setItems(directorService.findAllProjections());
        add(new H1("Director Management"), grid);
    }

    public IconRenderer<DirectorProjection> iconRenderer() {
        return new IconRenderer<>(
                data -> {
                    var avatar = new Avatar(data.name(), data.picture().toString());
                    avatar.setTooltipEnabled(true);
                    return avatar;
                }, _ -> ""
        );
    }


}
