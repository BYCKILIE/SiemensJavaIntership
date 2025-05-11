CREATE SEQUENCE item_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE item
(
    id          BIGINT DEFAULT NEXT VALUE FOR item_seq,
    name        VARCHAR(255),
    description VARCHAR(1000),
    status      VARCHAR(100),
    email       VARCHAR(255),
    PRIMARY KEY (id)
);