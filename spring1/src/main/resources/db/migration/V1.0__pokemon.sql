CREATE TABLE pokemon
(
    id         UUID PRIMARY KEY,
    pokedex_id INT          NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    version BIGINT
);