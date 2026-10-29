-- Inventoco: every row belongs to one user (owner_id), except the shared catalog and the
-- system suppliers (owner_id null: store chains whose prices are seeded for everyone).
-- Times are timestamptz (instants); quantities are in the base unit of their dimension
-- (unit, g, ml); money is numeric with an ISO-4217 currency next to it.

CREATE TABLE user_profile (
    id                      uuid PRIMARY KEY,
    authco_sub              varchar(255) NOT NULL UNIQUE,
    email                   varchar(255),
    name                    varchar(160),
    country                 varchar(2)      NOT NULL,
    currency                varchar(3)      NOT NULL,
    time_zone               varchar(60)  NOT NULL,
    share_prices_by_default boolean      NOT NULL DEFAULT false,
    onboarded               boolean      NOT NULL DEFAULT false,
    created_at              timestamptz  NOT NULL
);

CREATE TABLE catalog_product (
    id              uuid PRIMARY KEY,
    name            varchar(200) NOT NULL,
    normalized_name varchar(200) NOT NULL,
    brand           varchar(120),
    gtin            varchar(14) UNIQUE,
    dimension       varchar(10)  NOT NULL,
    created_at      timestamptz  NOT NULL
);
CREATE INDEX catalog_product_name_idx ON catalog_product (normalized_name);

CREATE TABLE item (
    id                 uuid PRIMARY KEY,
    owner_id           uuid          NOT NULL REFERENCES user_profile (id),
    name               varchar(160)  NOT NULL,
    category           varchar(80),
    dimension          varchar(10)   NOT NULL,
    display_unit       varchar(10)   NOT NULL,
    min_stock          numeric(19, 6) NOT NULL DEFAULT 0,
    current_stock      numeric(19, 6) NOT NULL DEFAULT 0,
    barcode            varchar(14),
    catalog_product_id uuid REFERENCES catalog_product (id),
    archived           boolean       NOT NULL DEFAULT false,
    created_at         timestamptz   NOT NULL,
    updated_at         timestamptz   NOT NULL,
    CONSTRAINT item_stock_not_negative CHECK (current_stock >= 0),
    CONSTRAINT item_min_stock_not_negative CHECK (min_stock >= 0)
);
CREATE INDEX item_owner_idx ON item (owner_id);

CREATE TABLE supplier (
    id         uuid PRIMARY KEY,
    owner_id   uuid REFERENCES user_profile (id),
    name       varchar(160) NOT NULL,
    country    varchar(2)      NOT NULL,
    city       varchar(120),
    url        varchar(500),
    archived   boolean      NOT NULL DEFAULT false,
    created_at timestamptz  NOT NULL
);
CREATE INDEX supplier_owner_idx ON supplier (owner_id);
CREATE UNIQUE INDEX supplier_system_name_idx ON supplier (country, lower(name)) WHERE owner_id IS NULL;

CREATE TABLE purchase (
    id           uuid PRIMARY KEY,
    owner_id     uuid        NOT NULL REFERENCES user_profile (id),
    status       varchar(12) NOT NULL,
    currency     varchar(3)     NOT NULL,
    horizon_days int         NOT NULL,
    created_at   timestamptz NOT NULL,
    completed_at timestamptz
);
CREATE INDEX purchase_owner_idx ON purchase (owner_id, created_at);
-- One open shopping list per user
CREATE UNIQUE INDEX purchase_one_draft_idx ON purchase (owner_id) WHERE status = 'DRAFT';

CREATE TABLE purchase_line (
    id                 uuid PRIMARY KEY,
    purchase_id        uuid           NOT NULL REFERENCES purchase (id) ON DELETE CASCADE,
    item_id            uuid           NOT NULL REFERENCES item (id),
    position           int            NOT NULL,
    reason             varchar(12)    NOT NULL,
    suggested_quantity numeric(19, 6) NOT NULL,
    supplier_id        uuid REFERENCES supplier (id),
    supplier_name      varchar(160),
    best_unit_price    numeric(24, 10),
    usual_unit_price   numeric(24, 10)
);
CREATE INDEX purchase_line_purchase_idx ON purchase_line (purchase_id);

CREATE TABLE stock_movement (
    id          uuid PRIMARY KEY,
    owner_id    uuid           NOT NULL REFERENCES user_profile (id),
    item_id     uuid           NOT NULL REFERENCES item (id),
    type        varchar(12)    NOT NULL,
    quantity    numeric(19, 6) NOT NULL,
    occurred_at timestamptz    NOT NULL,
    note        varchar(500),
    purchase_id uuid REFERENCES purchase (id),
    created_at  timestamptz    NOT NULL
);
CREATE INDEX stock_movement_item_idx ON stock_movement (item_id, occurred_at);

CREATE TABLE price_observation (
    id                 uuid PRIMARY KEY,
    owner_id           uuid REFERENCES user_profile (id),
    item_id            uuid REFERENCES item (id),
    supplier_id        uuid            NOT NULL REFERENCES supplier (id),
    catalog_product_id uuid REFERENCES catalog_product (id),
    amount             numeric(19, 4)  NOT NULL,
    currency           varchar(3)         NOT NULL,
    package_quantity   numeric(19, 6)  NOT NULL,
    package_unit       varchar(10)     NOT NULL,
    unit_price         numeric(24, 10) NOT NULL,
    country            varchar(2)         NOT NULL,
    visibility         varchar(8)      NOT NULL,
    observed_at        timestamptz     NOT NULL,
    purchase_id        uuid REFERENCES purchase (id),
    created_at         timestamptz     NOT NULL,
    CONSTRAINT price_amount_not_negative CHECK (amount >= 0),
    CONSTRAINT price_package_positive CHECK (package_quantity > 0)
);
CREATE INDEX price_observation_item_idx ON price_observation (item_id, observed_at);
CREATE INDEX price_observation_public_idx ON price_observation (catalog_product_id, country, currency, observed_at)
    WHERE visibility = 'PUBLIC';
