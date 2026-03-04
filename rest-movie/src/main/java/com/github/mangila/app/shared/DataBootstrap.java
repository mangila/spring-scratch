package com.github.mangila.app.shared;

import com.github.mangila.app.actor.persistence.ActorEntity;
import com.github.mangila.app.actor.persistence.ActorJpaRepository;
import com.github.mangila.app.actor.shared.ActorMapper;
import com.github.mangila.app.director.persistance.DirectorEntity;
import com.github.mangila.app.director.persistance.DirectorJpaRepository;
import com.github.mangila.app.director.shared.DirectorMapper;
import com.github.mangila.app.movie.persistance.MovieJpaRepository;
import com.github.mangila.app.movie.persistance.projection.MovieProjection;
import com.github.mangila.app.movie.service.MovieService;
import com.github.mangila.app.movie.shared.MovieMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile("dev")
public class DataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    private final ClassPathResource actorResource;

    private final ClassPathResource directorResource;

    private final ClassPathResource movieResource;

    private final ActorMapper actorMapper;

    private final DirectorMapper directorMapper;

    private final MovieMapper movieMapper;

    private final ActorJpaRepository actorJpaRepository;

    private final DirectorJpaRepository directorJpaRepository;

    private final MovieJpaRepository movieJpaRepository;

    private final TransactionTemplate transactionTemplate;

    private final MovieService movieService;

    public DataBootstrap(@Value("data/actors.csv") ClassPathResource actorResource,
                         @Value("data/directors.csv") ClassPathResource directorResource,
                         @Value("data/movies.csv") ClassPathResource movieResource, ActorMapper actorMapper,
                         DirectorMapper directorMapper, MovieMapper movieMapper, ActorJpaRepository actorJpaRepository,
                         DirectorJpaRepository directorJpaRepository, MovieJpaRepository movieJpaRepository,
                         TransactionTemplate transactionTemplate, MovieService movieService) {
        this.actorResource = actorResource;
        this.directorResource = directorResource;
        this.movieResource = movieResource;
        this.actorMapper = actorMapper;
        this.directorMapper = directorMapper;
        this.movieMapper = movieMapper;
        this.actorJpaRepository = actorJpaRepository;
        this.directorJpaRepository = directorJpaRepository;
        this.movieJpaRepository = movieJpaRepository;
        this.transactionTemplate = transactionTemplate;
        this.movieService = movieService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        final List<ActorEntity> actorEntities = readActors(csvFormat);
        final List<DirectorEntity> directorEntities = readDirectors(csvFormat);
        final List<MovieProjection> movieProjectionEntities = readMovies(csvFormat);

        for (var actor : actorEntities) {
            for (int i = 0; i < ThreadLocalRandom.current().nextInt(0, movieProjectionEntities.size()); i++) {
                var rn = ThreadLocalRandom.current().nextInt(0, movieProjectionEntities.size());
                var movie = movieProjectionEntities.get(rn);
                movie.actors().add(actor.getId());
                actor.getMovies().add(movie.id());
            }
        }

        for (int i = 0; i < movieProjectionEntities.size(); i++) {
            var director = directorEntities.get(i);
            var movie = movieProjectionEntities.get(i);
            movie.directors().add(director.getId());
            director.getMovies().add(movie.id());
        }

        transactionTemplate.executeWithoutResult(_ -> {
            actorJpaRepository.persistAll(actorEntities);
            directorJpaRepository.persistAll(directorEntities);
            movieService.persistAll(movieProjectionEntities);
        });
    }

    private List<ActorEntity> readActors(CSVFormat csvFormat) throws IOException {
        final List<ActorEntity> actorEntities;
        try (var reader = getReader(actorResource.getInputStream()); CSVParser csvParser = csvFormat.parse(reader)) {
            actorEntities = csvParser.stream()
                    .peek(record -> log.info("{}", record))
                    .map(actorMapper::toEntity)
                    .peek(actorEntity -> log.info("{}", actorEntity))
                    .toList();
        }
        return actorEntities;
    }

    private List<DirectorEntity> readDirectors(CSVFormat csvFormat) throws IOException {
        final List<DirectorEntity> directorEntities;
        try (var reader = getReader(directorResource.getInputStream()); CSVParser csvParser = csvFormat.parse(reader)) {
            directorEntities = csvParser.stream()
                    .peek(record -> log.info("{}", record))
                    .map(directorMapper::toEntity)
                    .peek(directorEntity -> log.info("{}", directorEntity))
                    .toList();
        }
        return directorEntities;
    }

    private List<MovieProjection> readMovies(CSVFormat csvFormat) throws IOException {
        final List<MovieProjection> movieProjectionEntities;
        try (var reader = getReader(movieResource.getInputStream()); CSVParser csvParser = csvFormat.parse(reader)) {
            movieProjectionEntities = csvParser.stream()
                    .peek(record -> log.info("{}", record))
                    .map(movieMapper::toDomain)
                    .peek(movieProjection -> log.info("{}", movieProjection))
                    .toList();
        }
        return movieProjectionEntities;
    }

    private BufferedReader getReader(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream));
    }

}
