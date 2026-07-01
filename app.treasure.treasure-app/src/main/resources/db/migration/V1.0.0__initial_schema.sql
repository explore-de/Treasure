create sequence Device_SEQ start with 1 increment by 50;
create sequence DeviceHistory_SEQ start with 1 increment by 50;
create sequence Member_SEQ start with 1 increment by 50;
create sequence Organization_SEQ start with 1 increment by 50;
create sequence Tag_SEQ start with 1 increment by 50;

create table Device (
    visible boolean,
    bookedBy_id bigint,
    id bigint not null,
    pickupTime timestamp(6),
    createdOn varchar(255),
    deviceAge varchar(255),
    deviceDamage varchar(255),
    deviceGroup varchar(255),
    deviceHDDStorage varchar(255),
    deviceLocation varchar(255),
    deviceModel varchar(255),
    deviceModelDate varchar(255),
    deviceName varchar(255),
    deviceNumber varchar(255),
    deviceProzessor varchar(255),
    deviceRAM varchar(255),
    deviceSerialNumber varchar(255),
    extraInfo varchar(255),
    regCompany varchar(255),
    status varchar(255),
    primary key (id)
);

create table DeviceHistory (
    actor_id bigint,
    device_id bigint,
    happenedAt timestamp(6),
    id bigint not null,
    eventType varchar(255),
    fieldName varchar(255),
    newValue varchar(255),
    notes varchar(255),
    oldValue varchar(255),
    primary key (id)
);

create table Member (
    id bigint not null,
    invited_by_member_id bigint,
    joined_at timestamp(6) with time zone,
    organization_id bigint not null,
    inviteType varchar(20),
    email varchar(255),
    firstName varchar(255) not null,
    keycloak_user_id varchar(255) unique,
    lastName varchar(255) not null,
    phone varchar(255),
    userName varchar(255) unique,
    primary key (id)
);

create table Organization (
    active boolean not null,
    createdAt timestamp(6) with time zone not null,
    id bigint not null,
    description varchar(255),
    displayName varchar(255),
    name varchar(255) not null unique,
    slug varchar(255) not null unique,
    primary key (id)
);

create table Tag (
    id bigint not null,
    organization_id bigint not null,
    name varchar(255) not null,
    primary key (id),
    unique (organization_id, name)
);

alter table if exists Device add constraint FK_device_booked_by foreign key (bookedBy_id) references Member;
alter table if exists DeviceHistory add constraint FK_devicehistory_actor foreign key (actor_id) references Member;
alter table if exists DeviceHistory add constraint FK_devicehistory_device foreign key (device_id) references Device;
alter table if exists Member add constraint FK_member_organization foreign key (organization_id) references Organization;
alter table if exists Tag add constraint FK_tag_organization foreign key (organization_id) references Organization;
