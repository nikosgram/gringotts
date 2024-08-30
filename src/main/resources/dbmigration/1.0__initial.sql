-- apply changes
create table gringotts_account (
  id                            integer not null,
  type                          varchar(255) not null,
  owner                         varchar(255) not null,
  cents                         integer not null,
  constraint uq_gringotts_account_type_owner unique (type,owner),
  constraint pk_gringotts_account primary key (id)
);

create table gringotts_accountchest (
  id                            integer not null,
  world                         varchar(255) not null,
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  account                       integer not null,
  constraint uq_gringotts_accountchest_world_x_y_z unique (world,x,y,z),
  constraint pk_gringotts_accountchest primary key (id)
);

