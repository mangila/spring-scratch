package com.github.mangila.movie.web.api;

import com.github.mangila.movie.persistence.director.DirectorProjection;
import com.github.mangila.movie.service.DirectorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/directors")
public class DirectorController {

    private final DirectorService directorService;

    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @GetMapping
    public List<DirectorProjection> findAllProjections() {
        return directorService.findAllProjections();
    }
}
