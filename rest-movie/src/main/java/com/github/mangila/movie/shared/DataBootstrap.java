package com.github.mangila.movie.shared;

import com.github.mangila.movie.persistence.actor.ActorEntity;
import com.github.mangila.movie.persistence.actor.ActorJpaRepository;
import com.github.mangila.movie.persistence.director.DirectorEntity;
import com.github.mangila.movie.persistence.director.DirectorJpaRepository;
import com.github.mangila.movie.persistence.movie.MovieEntity;
import com.github.mangila.movie.persistence.movie.MovieJpaRepository;
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

    public DataBootstrap(@Value("data/actors.csv") ClassPathResource actorResource,
                         @Value("data/directors.csv") ClassPathResource directorResource,
                         @Value("data/movies.csv") ClassPathResource movieResource, ActorMapper actorMapper,
                         DirectorMapper directorMapper, MovieMapper movieMapper, ActorJpaRepository actorJpaRepository,
                         DirectorJpaRepository directorJpaRepository, MovieJpaRepository movieJpaRepository, TransactionTemplate transactionTemplate) {
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
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedData();
    }

    public void seedData() throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        final List<ActorEntity> actorEntities;
        final List<DirectorEntity> directorEntities;
        final List<MovieEntity> movieEntities;
        try (var reader = getReader(actorResource.getInputStream()); CSVParser csvParser = csvFormat.parse(reader)) {
            actorEntities = csvParser.stream()
                    .peek(record -> log.info("{}", record))
                    .map(actorMapper::toEntity)
                    .peek(actorEntity -> log.info("{}", actorEntity))
                    .toList();
        }

        try (var reader = getReader(directorResource.getInputStream()); CSVParser csvParser = csvFormat.parse(reader)) {
            directorEntities = csvParser.stream()
                    .peek(record -> log.info("{}", record))
                    .map(directorMapper::toEntity)
                    .peek(directorEntity -> log.info("{}", directorEntity))
                    .toList();
        }

        try (var reader = getReader(movieResource.getInputStream()); CSVParser csvParser = csvFormat.parse(reader)) {
            movieEntities = csvParser.stream()
                    .peek(record -> log.info("{}", record))
                    .map(movieMapper::toEntity)
                    .peek(movieEntity -> log.info("{}", movieEntity))
                    .toList();
        }

        for (var actor : actorEntities) {
            for (int i = 0; i < ThreadLocalRandom.current().nextInt(0, movieEntities.size()); i++) {
                var rn = ThreadLocalRandom.current().nextInt(0, movieEntities.size());
                var movie = movieEntities.get(rn);
                movie.getActors().add(actor.getId());
                actor.getMovies().add(movie.getId());
            }
        }

        for (int i = 0; i < movieEntities.size(); i++) {
            var director = directorEntities.get(i);
            var movie = movieEntities.get(i);
            movie.getDirectors().add(director.getId());
            director.getMovies().add(movie.getId());
        }

        transactionTemplate.executeWithoutResult(_ -> {
            actorJpaRepository.persistAll(actorEntities);
            directorJpaRepository.persistAll(directorEntities);
            movieJpaRepository.persistAll(movieEntities);
        });
    }

    private BufferedReader getReader(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream));
    }
}
