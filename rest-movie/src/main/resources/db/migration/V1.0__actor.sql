create table actor
(
    id            UUID                        NOT NULL,
    name          TEXT                        NOT NULL,
    picture       TEXT                        NOT NULL,
    biography     TEXT                        NOT NULL,
    date_of_birth DATE                        NOT NULL,
    movies        JSONB,
    version       INTEGER,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    primary key (id)
);
create table actor_history
(
    id           UUID DEFAULT gen_random_uuid() NOT NULL,
    aggregate_id UUID                           NOT NULL,
    version      INTEGER,
    operation    TEXT                           NOT NULL,
    payload      JSONB                          NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    primary key (id)
);