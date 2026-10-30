-- The identity provider is no longer Keycloak-only: the stable user id can come from Entra (oid) too.
alter table if exists Member rename column keycloak_user_id to subject;
alter table if exists Member rename constraint member_keycloak_user_id_key to member_subject_key;
