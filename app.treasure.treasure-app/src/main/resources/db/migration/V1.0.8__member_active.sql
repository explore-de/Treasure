-- An admin can deactivate a member, for example when they leave. The row stays
-- because devices' history refers to it; a deactivated member cannot be given
-- devices and their API keys stop working.
alter table Member add column active boolean not null default true;
