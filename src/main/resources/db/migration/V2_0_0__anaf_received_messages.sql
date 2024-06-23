CREATE TABLE IF NOT EXISTS public.received_message
(
    id bigint NOT NULL,
    creation_date timestamp(6) without time zone,
    details text COLLATE pg_catalog."default",
    message_type character varying(255) COLLATE pg_catalog."default",
    tax_id character varying(255) COLLATE pg_catalog."default",
    upload_index character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT received_message_pkey PRIMARY KEY (id)
)
TABLESPACE pg_default;

ALTER TABLE public.companyoauth_token ADD COLUMN tax_id character varying(255) COLLATE pg_catalog."default";
UPDATE public.companyoauth_token SET tax_id = '14998343';

CREATE TABLE IF NOT EXISTS public.received_invoice
(
    id bigint NOT NULL,
    upload_index character varying(255) COLLATE pg_catalog."default",
    download_id character varying(255) COLLATE pg_catalog."default",
    xml_raw text COLLATE pg_catalog."default",
    issue_date date,
    invoice_id bigint,
    CONSTRAINT received_invoice_pkey PRIMARY KEY (id)
)
TABLESPACE pg_default;
ALTER TABLE IF EXISTS public.received_invoice
    ADD CONSTRAINT "UK_invoice_id" UNIQUE (invoice_id);