create table movie
(
    id           UUID NOT NULL,
    name         varchar(255),
    genre        varchar(255),
    actors       jsonb,
    directors    jsonb,
    budget       numeric(38, 2),
    release_date date,
    version      integer,
    created_at   timestamp(6) with time zone,
    updated_at   timestamp(6) with time zone,
    primary key (id)
);
create table movie_history
(
    id           UUID DEFAULT gen_random_uuid() NOT NULL,
    aggregate_id UUID                           NOT NULL,
    version      INTEGER,
    operation    VARCHAR(255)                   NOT NULL,
    payload      JSONB                          NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    primary key (id)
);