drop table if exists @jspwiki.userdatabase.table@;
drop user @jdbc.user.id@;

create table if not exists @jspwiki.userdatabase.table@ (
  @jspwiki.userdatabase.email@ varchar(100),
  @jspwiki.userdatabase.fullName@ varchar(100),
  @jspwiki.userdatabase.loginName@ varchar(100) not null,
  @jspwiki.userdatabase.password@ varchar(100),
  @jspwiki.userdatabase.wikiName@ varchar(100),
  @jspwiki.userdatabase.created@ timestamp,
  @jspwiki.userdatabase.modified@ timestamp,
  constraint @jspwiki.userdatabase.table@ primary key (@jspwiki.userdatabase.loginName@)
);

create user @jdbc.user.id@ set password '@jdbc.user.password@';

grant select, insert, update, delete on @jspwiki.userdatabase.table@ to @jdbc.user.id@;
