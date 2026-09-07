-- Store the selected city on the food, without creating a region record.
ALTER TABLE food
    ADD COLUMN province VARCHAR(100) NULL,
    ADD COLUMN city VARCHAR(100) NULL;
