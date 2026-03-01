package com.github.mangila.movie.web.view;

import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@PageTitle("Movie App | Home")
@Route("")
public class HomeView extends VerticalLayout {

    public HomeView(@Value("markdown/hej.md") ClassPathResource resource) throws IOException {
        var md = new Markdown(resource.getContentAsString(StandardCharsets.UTF_8));
        add(md);
    }
}
