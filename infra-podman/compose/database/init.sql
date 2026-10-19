\c db_reservation;

-- Required for UUID generation with gen_random_uuid().
create extension if not exists pgcrypto;
-- ============================================================
-- PRODUCER API
-- Main business table: product reservations.
-- ============================================================
create table if not exists tb_reservation (
    id uuid primary key default gen_random_uuid(),
    customer_name varchar(150) not null,
    external_product_id integer not null,
    product_title varchar(255) not null,
    quantity integer not null,
    status varchar(20) not null default 'CREATED',

    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,

    constraint chk_reservation_quantity_positive
        check (quantity > 0),
    constraint chk_reservation_status
        check (status in ('CREATED', 'CANCELLED'))
);

create index if not exists idx_reservation_external_product
    on public.tb_reservation (external_product_id);

create index if not exists idx_reservation_status
    on public.tb_reservation (status);

create index if not exists idx_reservation_created_at
    on public.tb_reservation (created_at desc);
-- ============================================================
-- CONSUMER API
-- Audit table: one row for every Kafka event processed.
-- event_id is unique to prevent duplicate event processing.
-- ============================================================
create table if not exists tb_reservation_event_log (
    id uuid primary key default gen_random_uuid(),
    event_id uuid not null unique,
    reservation_id uuid not null,
    event_type varchar(50) not null default 'RESERVATION_CREATED',
    event_status varchar(20) not null default 'PROCESSED',
    customer_name varchar(150) not null,
    external_product_id integer not null,
    product_title varchar(255) not null,
    quantity integer not null,
    reservation_status varchar(20) not null,
    kafka_topic varchar(150) not null,
    kafka_partition integer,
    kafka_offset bigint,
    error_message varchar(500),
    received_at timestamp with time zone not null default current_timestamp,
    processed_at timestamp with time zone,

    constraint chk_event_quantity_positive
        check (quantity > 0),
    constraint chk_event_status
        check (event_status in ('PROCESSED', 'FAILED')),
    constraint chk_event_reservation_status
        check (reservation_status in ('CREATED', 'CANCELLED'))
);

create index if not exists idx_reservation_event_log_reservation_id
    on public.tb_reservation_event_log (reservation_id);

create index if not exists idx_reservation_event_log_external_product
    on public.tb_reservation_event_log (external_product_id);

create index if not exists idx_reservation_event_log_processed_at
    on public.tb_reservation_event_log (processed_at desc);

create index if not exists idx_reservation_event_log_event_status
    on public.tb_reservation_event_log (event_status);
-- ============================================================
-- CONSUMER API
-- Read model / summary table for administrative reporting.
-- It is updated by the Kafka consumer after processing events.
-- ============================================================

create table if not exists tb_product_reservation_summary (
    id uuid primary key default gen_random_uuid(),
    external_product_id integer not null unique,
    product_title varchar(255) not null,
    total_reservations integer not null default 0,
    total_quantity integer not null default 0,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,

    constraint chk_summary_total_reservations_non_negative
        check (total_reservations >= 0),
    constraint chk_summary_total_quantity_non_negative
        check (total_quantity >= 0)
);

create index if not exists idx_reservation_summary_product
    on public.tb_product_reservation_summary (external_product_id);
