-- Dates were stored as free text: createdOn as the ISO text of a LocalDateTime,
-- deviceAge ("Purchased on") as "19.07.2023", deviceModelDate ("Model year") as
-- "2023". Give them real types so they can be validated, sorted and compared.
-- The original text is kept in *_legacy columns so nothing is lost; a value that
-- does not parse leaves the typed column null. Drop the *_legacy columns in a
-- later migration once the data on the stand has been checked.

alter table Device rename column createdOn to createdOn_legacy;
alter table Device rename column deviceAge to deviceAge_legacy;
alter table Device rename column deviceModelDate to deviceModelDate_legacy;

alter table Device add column createdOn timestamp(6);
alter table Device add column deviceAge date;
alter table Device add column deviceModelDate integer;

-- Every row is converted on its own: one unreadable value (such as 31.02.2023)
-- only leaves that row's typed column null instead of failing the migration.
do $$
declare
    r record;
begin
    for r in select id, createdOn_legacy, deviceAge_legacy, deviceModelDate_legacy from Device loop
        begin
            update Device set createdOn = case
                when r.createdOn_legacy ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}' then r.createdOn_legacy::timestamp
                when r.createdOn_legacy ~ '^\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}$' then to_timestamp(r.createdOn_legacy, 'DD.MM.YYYY HH24:MI')::timestamp
                when r.createdOn_legacy ~ '^\d{2}\.\d{2}\.\d{4}$' then to_timestamp(r.createdOn_legacy, 'DD.MM.YYYY')::timestamp
            end where id = r.id;
        exception when others then
            null;
        end;
        begin
            update Device set deviceAge = case
                when trim(r.deviceAge_legacy) ~ '^\d{1,2}\.\d{1,2}\.\d{4}$' then to_date(trim(r.deviceAge_legacy), 'DD.MM.YYYY')
                when trim(r.deviceAge_legacy) ~ '^\d{4}-\d{2}-\d{2}' then left(trim(r.deviceAge_legacy), 10)::date
                when trim(r.deviceAge_legacy) ~ '^\d{1,2}/\d{1,2}/\d{4}$' then to_date(trim(r.deviceAge_legacy), 'DD/MM/YYYY')
            end where id = r.id;
        exception when others then
            null;
        end;
        begin
            update Device set deviceModelDate = case
                when trim(r.deviceModelDate_legacy) ~ '^\d{4}$' then trim(r.deviceModelDate_legacy)::integer
            end where id = r.id;
        exception when others then
            null;
        end;
    end loop;
end $$;
