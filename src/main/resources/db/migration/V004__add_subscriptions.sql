CREATE TABLE IF NOT EXISTS subscriptions (
    user_id int8 PRIMARY KEY,
    need_send boolean,
    constraint subscriptions_user_id_fkey foreign key (user_id) references users (id)
);