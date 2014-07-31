/*
 * Licensed under the Apache License, Version 2.0 (the "License")
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

drop table users if exists;
drop table roles if exists;
drop table groups if exists;
drop table group_members if exists;
--drop user "jspwiki";

create table users (
  uid varchar(100),
  email varchar_ignorecase(100),
  full_name varchar(100),
  login_name varchar(100) not null,
  password varchar(100),
  wiki_name varchar(100),
  created timestamp,
  modified timestamp,
  lock_expiry timestamp,
  attributes longvarchar,
  constraint users primary key (uid)
);

create table roles (
  login_name varchar(100) not null,
  role varchar(100) not null
);

create table groups (
  name varchar(100) not null,
  creator varchar(100),
  created timestamp,
  modifier varchar(100),
  modified timestamp,
  constraint groups primary key (name)
);

create table group_members (
  name varchar(100) not null,
  member varchar(100) not null,
  constraint group_members primary key
    (name,member)
);

create user "jspwiki" password "password";

grant select, insert, update, delete on users to "jspwiki";
grant select, insert, update, delete on roles to "jspwiki";
grant select, insert, update, delete on groups to "jspwiki";
grant select, insert, update, delete on group_members to "jspwiki";

insert into users (
  uid,
  email,
  full_name,
  login_name,
  password,
  wiki_name,
  attributes
) values (
  '-7739839977499061014',
  'janne@ecyrd.com',
  'Janne Jalkanen',
  'janne',
  '{SSHA}1WFv9OV11pD5IySgVH3sFa2VlCyYjbLrcVT/qw==',
  'JanneJalkanen',
  'attribute1=some random value\nattribute2=another value'
);
  
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
  'janne',
  'Authenticated'
);

insert into roles (
  login_name,
  role
) values (  
  'admin',
  'Authenticated'
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
  'TV',
  '2006-06-20 14:50:54.00000000',
  '2006-06-20 14:50:54.00000000'
);
insert into group_members (
  name,
  member
) values (  
  'TV',
  'Archie Bunker'
);
insert into group_members (
  name,
  member
) values (  
  'TV',
  'BullwinkleMoose'
);
insert into group_members (
  name,
  member
) values (  
  'TV',
  'Fred Friendly'
);

insert into groups (
  name,
  created,
  modified
) values (
  'Literature',
  '2006-06-20 14:50:54.00000000',
  '2006-06-20 14:50:54.00000000'
);
insert into group_members (
  name,
  member
) values (  
  'Literature',
  'Charles Dickens'
);

insert into group_members (
  name,
  member
) values (  
  'Literature',
  'Homer'
);

insert into groups (
  name,
  created,
  modified
) values (
  'Art',
  '2006-06-20 14:50:54.00000000',
  '2006-06-20 14:50:54.00000000'
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
