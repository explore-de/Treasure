-- Organizations are not part of the product. Members and tags stand on their own.
alter table if exists Member drop constraint if exists FK_member_organization;
alter table if exists Tag drop constraint if exists FK_tag_organization;
alter table if exists Tag drop constraint if exists tag_organization_id_name_key;
alter table if exists Member drop column if exists organization_id;
alter table if exists Tag drop column if exists organization_id;
alter table if exists Tag add constraint UK_tag_name unique (name);
drop table if exists Organization;
drop sequence if exists Organization_SEQ;

-- Device.visible was only written by a search endpoint that no longer exists.
alter table if exists Device drop column if exists visible;
