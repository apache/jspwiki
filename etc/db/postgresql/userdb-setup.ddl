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

insert into @jspwiki.userdatabase.table@ (
  @jspwiki.userdatabase.email@,
  @jspwiki.userdatabase.fullName@,
  @jspwiki.userdatabase.loginName@,
  @jspwiki.userdatabase.password@,
  @jspwiki.userdatabase.wikiName@
) values (
  'janne@ecyrd.com',
  'Janne Jalkanen',
  'janne',
  '457b08e825da547c3b77fbc1ff906a1d00a7daee',
  'JanneJalkanen'
);
  
insert into @jspwiki.userdatabase.roleTable@ (
  @jspwiki.userdatabase.loginName@,
  @jspwiki.userdatabase.role@
) values (  
  'janne',
  'Authenticated'
);