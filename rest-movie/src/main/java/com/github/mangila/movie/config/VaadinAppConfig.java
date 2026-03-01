package com.github.mangila.movie.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;

import static com.vaadin.flow.component.page.Viewport.DEFAULT;

@PWA(name = "Movie Vaadin App", shortName = "Movie App")
@ColorScheme(ColorScheme.Value.SYSTEM)
@Viewport(DEFAULT)
public class VaadinAppConfig implements AppShellConfigurator {
}
