package com.github.mangila.movie.persistence.outbox.type;

public enum Destination {
    KAFKA,
    RABBITMQ,
    SQS
}
