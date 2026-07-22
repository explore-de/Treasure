-- Device.bookedBy was renamed to Device.assignedTo in the JPA model.
alter table Device rename column bookedBy_id to assignedTo_id;
alter table Device rename constraint FK_device_booked_by to FK_device_assigned_to;
