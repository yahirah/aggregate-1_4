
create table acl_class(
    id bigserial not null primary key,
    class varchar(100) not null,
    constraint unique_uk_2 unique(class)
);


create table acl_entry(
  id bigserial primary key,
  object_id_class bigint not null,
  object_id_identity bigint not null,
  sid bigint,
  granted boolean not null,
  constraint unique_uk_3 unique(object_id_class,object_id_identity),
  constraint foreign_fk_1 foreign key(object_id_class) references acl_class(id));
  