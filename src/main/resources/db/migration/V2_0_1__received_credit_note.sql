CREATE TABLE IF NOT EXISTS public.received_credit_note
(
    id bigint NOT NULL,
    upload_index character varying(255) COLLATE pg_catalog."default",
    download_id character varying(255) COLLATE pg_catalog."default",
    xml_raw text COLLATE pg_catalog."default",
    issue_date date,
    CONSTRAINT received_credit_note_pkey PRIMARY KEY (id)
)
TABLESPACE pg_default;