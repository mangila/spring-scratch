create table actor
(
    id varchar(255) not null,
    primary key (id)
);

create table actor_movie
(
    actor_id varchar(255) not null,
    movies   varchar(255)
);

create table movie
(
    id varchar(255) not null,
    primary key (id)
);

create table movie_actor
(
    actors   varchar(255),
    movie_id varchar(255) not null
);

alter table if exists actor_movie
    add constraint FKpjjfl93tcc6ix1ugr0bn3so7h
    foreign key (actor_id)
    references actor;

alter table if exists movie_actor
    add constraint FKhedvt8u16luotgyoel4fqy7t1
    foreign key (movie_id)
    references movie;
