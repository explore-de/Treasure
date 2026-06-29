CREATE TABLE public.device (
    visible boolean,
    bookedby_id bigint,
    id bigint NOT NULL,
    pickuptime timestamp(6) without time zone,
    createdon character varying(255),
    deviceage character varying(255),
    devicedamage character varying(255),
    devicegroup character varying(255),
    devicehddstorage character varying(255),
    devicelocation character varying(255),
    devicemodel character varying(255),
    devicemodeldate character varying(255),
    devicename character varying(255),
    devicenumber character varying(255),
    deviceprozessor character varying(255),
    deviceram character varying(255),
    deviceserialnumber character varying(255),
    extrainfo character varying(255),
    regcompany character varying(255),
    status character varying(255)
);

CREATE SEQUENCE public.device_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.devicehistory (
    actor_id bigint,
    device_id bigint,
    happenedat timestamp(6) without time zone,
    id bigint NOT NULL,
    eventtype character varying(255),
    fieldname character varying(255),
    newvalue character varying(255),
    notes character varying(255),
    oldvalue character varying(255)
);

CREATE SEQUENCE public.devicehistory_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.member (
    id bigint NOT NULL,
    invited_by_member_id bigint,
    joined_at timestamp(6) with time zone,
    organization_id bigint NOT NULL,
    invitetype character varying(20),
    email character varying(255),
    firstname character varying(255) NOT NULL,
    keycloak_user_id character varying(255),
    lastname character varying(255) NOT NULL,
    phone character varying(255),
    username character varying(255)
);

CREATE SEQUENCE public.member_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.organization (
    active boolean NOT NULL,
    createdat timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    description character varying(255),
    displayname character varying(255),
    name character varying(255) NOT NULL,
    slug character varying(255) NOT NULL
);

CREATE SEQUENCE public.organization_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.tag (
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE SEQUENCE public.tag_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY public.device
    ADD CONSTRAINT device_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.devicehistory
    ADD CONSTRAINT devicehistory_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_keycloak_user_id_key UNIQUE (keycloak_user_id);

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_username_key UNIQUE (username);

ALTER TABLE ONLY public.organization
    ADD CONSTRAINT organization_name_key UNIQUE (name);

ALTER TABLE ONLY public.organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.organization
    ADD CONSTRAINT organization_slug_key UNIQUE (slug);

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT tag_organization_id_name_key UNIQUE (organization_id, name);

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT tag_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member
    ADD CONSTRAINT fk3b1t801owp4vu2nas842k0f5w FOREIGN KEY (organization_id) REFERENCES public.organization(id);

ALTER TABLE ONLY public.devicehistory
    ADD CONSTRAINT fk61py6ss6c1p12ao3t1d4kmte9 FOREIGN KEY (actor_id) REFERENCES public.member(id);

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT fka4b2l4rgucqpk5egsavclg80j FOREIGN KEY (organization_id) REFERENCES public.organization(id);

ALTER TABLE ONLY public.device
    ADD CONSTRAINT fkcyf9bcqikfqcm9ui87ihck2tx FOREIGN KEY (bookedby_id) REFERENCES public.member(id);

ALTER TABLE ONLY public.devicehistory
    ADD CONSTRAINT fkqlbdu8oyd1trus0noeef081qn FOREIGN KEY (device_id) REFERENCES public.device(id);
