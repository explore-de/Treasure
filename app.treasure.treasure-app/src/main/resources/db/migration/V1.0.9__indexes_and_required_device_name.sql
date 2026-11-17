-- PostgreSQL does not index foreign keys on its own. These are the columns the
-- lists and the history read by.
create index IDX_device_assigned_to on Device (assignedTo_id);
create index IDX_devicehistory_device on DeviceHistory (device_id);
create index IDX_devicehistory_happened_at on DeviceHistory (happenedAt);
create index IDX_mcptoken_member on McpToken (member_id);

-- The form and the MCP tools have always required a name; make the column say
-- so too. Rows written before that rule get a placeholder instead of failing.
update Device set deviceName = 'Unnamed device' where deviceName is null or trim(deviceName) = '';
alter table Device alter column deviceName set not null;
