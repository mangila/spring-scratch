CREATE TABLE pokemon
(
    id         UUID PRIMARY KEY,
    pokedex_id INT  NOT NULL UNIQUE,
    name       TEXT NOT NULL UNIQUE,
    image_url  TEXT NOT NULL,
    generation TEXT NOT NULL,
    version    BIGINT
);