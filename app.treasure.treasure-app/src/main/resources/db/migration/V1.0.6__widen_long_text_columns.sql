-- The device form accepts up to 500 characters for location and extra info,
-- but the columns held only 255, and the CREATED history entry joins every
-- field into one notes value. Saving a device with long text failed with a
-- database error. 2000 characters covers the longest possible notes value.

alter table Device alter column deviceLocation type varchar(2000);
alter table Device alter column extraInfo type varchar(2000);

alter table DeviceHistory alter column oldValue type varchar(2000);
alter table DeviceHistory alter column newValue type varchar(2000);
alter table DeviceHistory alter column notes type varchar(2000);
