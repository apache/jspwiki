<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->


# jspwiki-oidc

Logs users in with OpenID Connect, using the pac4j filters, and presents them to the wiki as users authenticated by the
container. It works with any OpenID Connect provider: it has been tried with Keycloak, see
[JSPWIKI-94](https://issues.apache.org/jira/browse/JSPWIKI-94) and
[JSPWIKI-1375](https://issues.apache.org/jira/browse/JSPWIKI-1375). The configurations for Google and Microsoft Entra ID (Azure)
below follow what these providers document, and have not been tried with them.

## Enabling it

1. Build the war with `mvn package -Poidc`, or add this module and its dependencies to `WEB-INF/lib`.
2. Create `WEB-INF/classes/jspwiki-oidc.properties`:

   ```
   oidc.clientId=the client id
   oidc.secret=the client secret
   oidc.discoveryUri=https://provider.example.org/.well-known/openid-configuration
   oidc.callbackUrl=/oidc/callback
   ```

   The optional settings are:

   - `oidc.scope`, `openid email profile` by default.
   - `oidc.usernameClaim`, the claim with the name of the user in the wiki, `preferred_username` by default. When the claim is
     missing, the wiki uses the e-mail address, and then the identifier of the user at the provider.
   - `oidc.rolesClaim`, the claim with the roles of the user. It is searched in the profile and in the access token when this is a JWT,
     and it may be a dotted path through nested claims. Without it, the users have no roles from the provider.
   - `oidc.roleMapping`, to turn the values of the roles claim into the names of the roles of the wiki when the provider sends
     identifiers instead of names, written `value=Role,value2=Role2`.

3. Add to `WEB-INF/classes/jspwiki-custom.properties`:

   ```
   jspwiki.authorizer = org.apache.wiki.oidc.OidcAuthorizer
   ```

   The roles the wiki takes from the provider are `Admin` and `Authenticated`. Change them with the property
   `jspwiki.oidc.roles`, a comma separated list.
4. In `WEB-INF/web.xml`, remove the comment marks around the OpenID Connect filters.
5. In the provider, register the address `https://your.wiki/oidc/callback` as redirect URI, and the address of the wiki as post logout
   redirect URI if the provider has that setting.

The links to log in and out of the wiki then use the provider: `Login.jsp` sends the user to it, and `Logout.jsp` ends the session of the
wiki and of the provider. The wiki is not asked for passwords.

The first time a user logs in, the wiki creates a profile in its user database with the full name (the claim `name`, or else
`given_name` and `family_name`, or else the login name) and the e-mail address (the claim `email`) that the provider sends, so that
the wiki shows the name of the person and can write to them. The profile has no password. A profile that exists is never changed by
the login, and none is created if another user already has the same full name or wiki name.

## Providers

Keycloak: the roles are in the access token, in the nested claim `realm_access.roles`.

```
oidc.discoveryUri=https://keycloak.example.org/realms/jspwiki/.well-known/openid-configuration
oidc.rolesClaim=realm_access.roles
```

Microsoft Entra ID (Azure): register the application with the redirect URI, create a client secret and define application roles
(App roles) for the administrators, which Entra sends in the claim `roles`. Use the tenant in the discovery address.

```
oidc.discoveryUri=https://login.microsoftonline.com/TENANT-ID/v2.0/.well-known/openid-configuration
oidc.usernameClaim=preferred_username
oidc.rolesClaim=roles
```

Google: create an OAuth client of type web application. Google does not send roles, so all its users are authenticated users of the wiki,
and the administrators can be defined with the groups of the wiki.

```
oidc.discoveryUri=https://accounts.google.com/.well-known/openid-configuration
oidc.usernameClaim=email
```
