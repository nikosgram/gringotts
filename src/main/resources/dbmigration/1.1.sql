-- apply changes
create table gringotts_pending_operation (
  id                            integer not null,
  world                         varchar(255) not null,
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  amount                        integer not null,
  constraint pk_gringotts_pending_operation primary key (id)
);

-- apply alter tables
alter table gringotts_accountchest add column total_value integer default 0 not null;
