drop table @jspwiki.userdatabase.table@;
drop table @jspwiki.userdatabase.roleTable@;
drop user @jdbc.user.id@;

create table @jspwiki.userdatabase.table@ (
  @jspwiki.userdatabase.email@ varchar(100),
  @jspwiki.userdatabase.fullName@ varchar(100),
  @jspwiki.userdatabase.loginName@ varchar(100) not null primary key,
  @jspwiki.userdatabase.password@ varchar(100),
  @jspwiki.userdatabase.wikiName@ varchar(100),
  @jspwiki.userdatabase.created@ timestamp,
  @jspwiki.userdatabase.modified@ timestamp
);

create table @jspwiki.userdatabase.roleTable@ (
  @jspwiki.userdatabase.loginName@ varchar(100) not null,
  @jspwiki.userdatabase.role@ varchar(100) not null
);

create user @jdbc.user.id@ with encrypted password '@jdbc.user.password@' nocreatedb nocreateuser;

grant select, insert, update, delete on @jspwiki.userdatabase.table@ to @jdbc.user.id@;
grant select, insert on @jspwiki.userdatabase.roleTable@ to @jdbc.user.id@;
