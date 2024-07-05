CREATE extension IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS currency (
    code        varchar(3) primary key,
    number_code varchar(3),
    name        varchar
);

CREATE TABLE IF NOT EXISTS bitcoin_quotes (
    id            uuid primary key default uuid_generate_v4(),
    quote_time    timestamp,
    currency_code varchar(3),
    rate          decimal(16, 8)
);

CREATE INDEX bitcoin_quotes_currency_code_idx ON public.bitcoin_quotes USING btree (currency_code);
CREATE INDEX bitcoin_quotes_quote_time_idx ON public.bitcoin_quotes USING btree (quote_time);