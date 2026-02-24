CREATE TABLE pokemon
(
    id         UUID PRIMARY KEY,
    pokedex_id INT  NOT NULL UNIQUE,
    name       TEXT NOT NULL UNIQUE,
    generation TEXT NOT NULL,
    version    BIGINT
);