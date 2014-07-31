/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

drop table users;
drop table roles;
drop table groups;
drop table group_members;
drop user jspwiki;

create table users (
  uid varchar(100),
  email varchar_ignorecase(100),
  full_name varchar(100),
  login_name varchar(100) not null primary key,
  password varchar(100),
  wiki_name varchar(100),
  created timestamp,
  modified timestamp,
  lock_expiry timestamp,
  attributes longvarchar,
);

create table roles (
  login_name varchar(100) not null,
  role varchar(100) not null
);

create table groups (
  name varchar(100) not null primary key,
  creator varchar(100),
  created timestamp,
  modifier varchar(100),
  modified timestamp
);

create table group_members (
  name varchar(100) not null,
  member varchar(100) not null,
  constraint group_members_pk
    primary key (name,member)
);

create user jspwiki with encrypted password 'password' nocreatedb nocreateuser;

grant select, insert, update, delete on users to jspwiki;
grant select, insert, update, delete on roles to jspwiki;
grant select, insert, update, delete on groups to jspwiki;
grant select, insert, update, delete on group_members to jspwiki;

insert into users (
  uid,
  email,
  full_name,
  login_name,
  password,
  wiki_name
) values (
  '-6852820166199419346',
  'admin@locahost',
  'Administrator',
  'admin',
  '{SSHA}6YNKYMwXICUf5pMvYUZumgbFCxZMT2njtUQtJw==',
  'Administrator'
);

insert into roles (
  login_name,
  role
) values (  
  'admin',
  'Admin'
);

insert into groups (
  name,
  created,
  modified
) values (
  'Admin',
  '2006-06-20 14:50:54.00000000',
  '2006-06-20 14:50:54.00000000'
);

insert into group_members (
  name,
  member
) values (  
  'Admin',
  'Administrator'
);

#drop table users;
#drop table roles;
#drop table groups;
#drop table group_members;
#drop user jspwiki;
