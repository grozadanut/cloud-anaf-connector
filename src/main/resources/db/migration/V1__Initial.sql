CREATE TABLE IF NOT EXISTS oauth2_authorized_client (
  client_registration_id varchar(100) NOT NULL,
  principal_name varchar(200) NOT NULL,
  access_token_type varchar(100) NOT NULL,
  access_token_value bytea NOT NULL,
  access_token_issued_at timestamp NOT NULL,
  access_token_expires_at timestamp NOT NULL,
  access_token_scopes varchar(1000) DEFAULT NULL,
  refresh_token_value bytea DEFAULT NULL,
  refresh_token_issued_at timestamp DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (client_registration_id, principal_name)
);

CREATE TABLE IF NOT EXISTS public.reported_invoice
(
    invoice_id bigint NOT NULL,
    state character varying(255) COLLATE pg_catalog."default",
    upload_index character varying(255) COLLATE pg_catalog."default",
    download_id character varying(255) COLLATE pg_catalog."default",
    error_message character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT reported_invoice_pkey PRIMARY KEY (invoice_id)
)
TABLESPACE pg_default;

CREATE TABLE IF NOT EXISTS public.companyoauth_token
(
    company_id integer NOT NULL,
    client_registration_id character varying(255) COLLATE pg_catalog."default" NOT NULL,
    principal_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT companyoauth_token_pkey PRIMARY KEY (company_id)
)
TABLESPACE pg_default;