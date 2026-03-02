package com.github.mangila.movie.config;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;

import static com.vaadin.flow.component.page.Viewport.DEFAULT;

@PWA(name = "Movie Spring/Vaadin App", shortName = "Movie App")
@StyleSheet(Lumo.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
@Viewport(DEFAULT)
public class VaadinAppConfig implements AppShellConfigurator {

	public static final String PAGE_TITLE_PREFIX = "Movie App| ";

}
