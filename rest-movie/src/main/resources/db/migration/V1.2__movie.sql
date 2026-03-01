create table movie
(
    id           UUID NOT NULL,
    title        TEXT UNIQUE,
    genres       JSONB,
    actors       JSONB,
    directors    JSONB,
    budget       NUMERIC(38, 2),
    release_date DATE,
    version      INTEGER,
    created_at   TIMESTAMP(6) WITH TIME ZONE,
    updated_at   TIMESTAMP(6) WITH TIME ZONE,
    primary key (id)
);
create table movie_history
(
    id           UUID DEFAULT gen_random_uuid() NOT NULL,
    aggregate_id UUID                           NOT NULL,
    version      INTEGER,
    operation    TEXT                           NOT NULL,
    payload      JSONB                          NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    primary key (id)
);