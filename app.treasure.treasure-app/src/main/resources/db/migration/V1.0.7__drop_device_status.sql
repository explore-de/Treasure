-- Device.status only repeated what assignedTo already says: a device is
-- available exactly when nobody holds it. The two could disagree on rows written
-- before assignment set both, so the column is dropped and the status is derived.
alter table Device drop column if exists status;
