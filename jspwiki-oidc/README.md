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
container. It has been tried with Keycloak, see [JSPWIKI-94](https://issues.apache.org/jira/browse/JSPWIKI-94).

## Enabling it

1. Build the war with `mvn package -Poidc`, or add this module and its dependencies to `WEB-INF/lib`.
2. Create `WEB-INF/classes/jspwiki-oidc.properties`:

   ```
   oidc.clientId=jspwiki-client
   oidc.secret=the client secret
   oidc.scope=openid email profile
   oidc.realm=jspwiki
   oidc.baseUri=https://keycloak.example.org
   oidc.discoveryUri=https://keycloak.example.org/realms/jspwiki/.well-known/openid-configuration
   oidc.callbackUrl=/oidc/callback
   ```

3. Add to `WEB-INF/classes/jspwiki-custom.properties`:

   ```
   jspwiki.authorizer = org.apache.wiki.oidc.OidcAuthorizer
   ```

   The roles the wiki takes from the identity provider are `Admin` and `Authenticated`. Change them with the property
   `jspwiki.oidc.roles`, a comma separated list.
4. In `WEB-INF/web.xml`, remove the comment marks around the OpenID Connect filters.
5. In Keycloak, create a confidential client whose valid redirect URIs and post logout redirect URIs include the address of the wiki,
   and the realm roles `Admin` and `Authenticated`, which are assigned to the users.

The links to log in and out of the wiki then use the identity provider: `Login.jsp` sends the user to it, and `Logout.jsp` ends the
session of the wiki and of the identity provider. The wiki is not asked for passwords, and its own user profiles are not used.
