package com.github.mangila.movie.persistence.outbox.type;

public enum Status {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}
