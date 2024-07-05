INSERT INTO currency(code, number_code, name) VALUES
    ('RUB', '643', 'Российский рубль'),
    ('GBP', '826', 'Фунт стерлингов'),
    ('USD', '840', 'Доллар США'),
    ('EUR', '978', 'Евро')
    ON CONFLICT DO NOTHING;