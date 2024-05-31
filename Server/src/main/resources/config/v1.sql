create table game_history
(
    id        int auto_increment
        primary key,
    user_id   int                                 not null,
    word_id   int                                 not null,
    played_at timestamp default CURRENT_TIMESTAMP null
);

create index user_id
    on game_history (user_id);

create index word_id
    on game_history (word_id);

create table sessions
(
    id      int auto_increment
        primary key,
    user_id int                  not null,
    active  tinyint(1) default 1 null
);

create index user_id
    on sessions (user_id);

create table users
(
    id       int auto_increment
        primary key,
    email    varchar(255) not null,
    password varchar(255) not null,
    constraint email
        unique (email)
);

create table words
(
    id    int auto_increment
        primary key,
    word  varchar(255) not null,
    topic varchar(255) not null
);

