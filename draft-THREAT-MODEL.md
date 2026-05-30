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

# Apache JSPWiki Security Threat Model (draft)

## §1 Header

- **Project**: Apache JSPWiki — a Java/JSP wiki engine packaged as a servlet
  webapp (WAR), deployed inside an operator-supplied servlet container
  (Apache Tomcat 10.1+ recommended) on JDK 17+ *(documented: `README.md`)*.
- **Repository scope**: `apache/jspwiki` only. `jspwiki-site` and
  `jspwiki-asf-docs` are dormant and **out of scope** (confirmed by PMC chair
  Juan Pablo Santos, 2026-05-30).
- **Version / commit**: drafted against the default branch (`master`),
  HEAD `be4ed49` ("use full artifact for javadocs doclet", 2026-05-28),
  corresponding to development series `3.0.0-git-23`. A report against
  released JSPWiki version *N* should be triaged against the model as it
  stood at *N*, not at HEAD.
- **Date**: 2026-05-30.
- **Authors**: ASF Security team draft, awaiting JSPWiki PMC review.
- **Status**: draft — under maintainer review.
- **Reporting**: vulnerabilities that fall under §8 (claimed properties)
  should be reported per the Apache Security Team disclosure channel
  (<security@jspwiki.apache.org>); reports that fall under §3 (out of
  scope), §9 (properties not provided), or §11a (known non-findings)
  will be closed by JSPWiki triagers citing this document. JSPWiki
  additionally maintains a public CVE log at
  `https://jspwiki-wiki.apache.org/Wiki.jsp?page=CVE` *(maintainer-published
  artefact — not mined into this draft, see §14 Q1)*.
- **Provenance legend** —
  *(documented)* = drawn from in-repo docs, code, or website docs, with
  citation;
  *(maintainer)* = stated by a JSPWiki maintainer in response to this draft;
  *(inferred)* = synthesized by the producer from code structure or domain
  knowledge, awaiting PMC ratification (every *(inferred)* tag has a matching
  §14 question).
- **Draft confidence**: 38 documented / 1 maintainer / 27 inferred.

JSPWiki is a long-lived wiki engine (origins in 2001) whose core abstractions
are: a **WikiSession** (a long-running per-HTTP-session principal/Subject
construct, see `org.apache.wiki.api.core.Session`); a **Engine** (process-
global, holds page/attachment providers, plugin manager, authn/authz
managers); a **markup parser** that translates JSPWiki syntax (or, optionally,
Markdown) into XHTML; a **plugin/page-filter** subsystem that runs Java code
inline as wiki pages are rendered or saved; and a JAAS-based AAA stack
layered on top of either the servlet container's authentication or JSPWiki's
own UserDatabase. Authorization is expressed as **Java 2-style permissions**
(`PagePermission`, `WikiPermission`, `GroupPermission`, `AllPermission`)
matched against a `WEB-INF/jspwiki.policy` security-policy file *and*, on a
per-page basis, an inline **ACL** embedded in the wiki text as
`[{ALLOW <action> <principal>...}]`. The wiki ships its own canonical
security documentation **on the wiki itself** at
`https://jspwiki-wiki.apache.org/Wiki.jsp?page=Security` (and its CVE log at
`?page=CVE`); these pages were not fetched into this draft (§14 Q1) so the
PMC should expect to compare this document against them.

## §2 Scope and intended use

### Intended use

- A **multi-user collaborative-editing wiki**, deployed by an operator inside
  a servlet container, accessed by end users over HTTP/HTTPS — most commonly
  for documentation, knowledge base, or intranet collaboration *(documented:
  `README.md`, `jspwiki-main/src/main/resources/ini/jspwiki.properties`
  lines 31–36)*.
- **Multi-tenancy is "soft"**: a single JSPWiki deployment serves a single
  logical wiki (the `jspwiki.applicationName`); cross-wiki isolation is not a
  property the engine claims *(inferred — §14 Q2)*. The wildcard wiki-prefix
  syntax in `PagePermission` (`*:PageName`) is a permission scope, not a
  tenancy boundary.
- **The wiki content is itself trusted to express security policy**: ACL
  directives, `[{ALLOW edit Charlie}]`, are wiki-text constructs parsed out
  of page content by `DefaultAclManager` (regex
  `\[\{\s*ALLOW\s+...\}\]` —
  `jspwiki-main/src/main/java/org/apache/wiki/auth/acl/DefaultAclManager.java`
  line 73). An attacker who can edit a page can rewrite that page's ACL *if
  and only if* the existing ACL grants them `edit` *(documented: same file)*.

### Deployment shape — three supported postures

JSPWiki is **always** a servlet-container-hosted webapp; there is no
embedded/library mode. Within that shape there are three deployment
postures with materially different threat envelopes — call them P1, P2, P3:

| Posture | Authentication source | Anonymous can edit? | Notes |
| --- | --- | --- | --- |
| **P1 — Open intranet wiki** | Cookie-asserted identity, or JSPWiki UserDatabase | yes, by default *(documented: `jspwiki.policy` "Anonymous" block lines 68–71 grants `modify` and `createPages` to `Role "Anonymous"`)* | This is the "Internet-facing wikis, you are strongly advised to remove the lines containing the 'modify' and 'createPages' permissions" warning in `jspwiki.policy` line 60. Out-of-box default. |
| **P2 — Custom auth via JAAS UserDatabase** | `jspwiki.loginModule.class` (default `UserDatabaseLoginModule`; XML or JDBC) | configurable | The mainstream production posture; the engine's own form-based login screen, throttled per `jspwiki.login.throttling=true`. |
| **P3 — Container-managed authentication** | Servlet container (Tomcat, etc.) via `web.xml` `<security-constraint>` + JAAS realm | configurable | Operator uncomments the relevant `<security-constraint>` blocks in `WEB-INF/web.xml` (lines noted in the shipped file: "REMOVE ME TO ENABLE CONTAINER-MANAGED AUTH"). |

A finding is in-model only if it cleanly applies under at least one of
these postures (with the posture identified). A finding that requires P1 +
the explicit guideline warning being ignored is `OUT-OF-MODEL:
non-default-build` against the operator, **not** a JSPWiki bug
*(inferred — §14 Q3)*.

### Caller roles (per output-structure §2, network-service split)

| Role | Trust level | Notes |
| --- | --- | --- |
| **Anonymous network peer** | untrusted | TCP/HTTP to the wiki's port; identity Role `Anonymous` *(documented: `Role.java` line 39)*; permissions are whatever the local `jspwiki.policy` grants `Role "Anonymous"` |
| **Asserted user** | untrusted-but-named | Identity asserted by a self-supplied cookie; Role `Asserted` *(documented: `Role.java` line 42, `jspwiki.properties` `jspwiki.cookieAssertions`)*. JSPWiki's own docs label this "still considered to be unsafe, just like no login at all" *(documented: `jspwiki.properties` lines 547–553)* |
| **Authenticated end user** | low-trust | Authenticated via P2 form login or P3 container login; Role `Authenticated` *(documented: `Role.java` line 45)* |
| **Group member** | low-trust + group privileges | Member of a wiki-managed `Group` (`GroupPrincipal`) defined by other Authenticated users *(documented: `jspwiki.properties` `jspwiki.groupdatabase`, `Group.java`)* |
| **Container-defined role** | configurable | Roles surfaced by `WebContainerAuthorizer.isUserInRole()` via `web.xml` `<security-role>` *(documented: `AuthenticationManager.java` lines 84–90)* |
| **Admin** | trusted within wiki | Member of the wiki `Admin` group **or** of container role `Admin`; granted `AllPermission` by default `jspwiki.policy` lines 107–112 |
| **Operator** | fully trusted | Has filesystem access to `jspwiki[-custom].properties`, `jspwiki.policy`, the page-store directory, the user/group XML databases, the cookie-login store under `$jspwiki.workDir/logincookies`, container deployment, and the JVM |
| **Plugin / page-filter author** | fully trusted | Plugins run **in-process inside the wiki webapp**; see Component-family table below |
| **PageFilter chain (SpamFilter etc.)** | trusted code, untrusted input | Operates on wiki-text on save; same in-process trust as the engine |

### Component-family table

| Family | Representative entry | Touches outside the process? | In this model? |
| --- | --- | --- | --- |
| **JSPWiki markup parser** (default) — `JSPWikiMarkupParser` *(documented: `MarkupParser.java` line 74)* | wiki text → XHTML | no | yes |
| **Markdown parser** (optional) — switched via `jspwiki.syntax=markdown` *(documented: `jspwiki.properties` line 476–478)* | wiki text → XHTML | no | yes |
| **Plugin subsystem** — `DefaultPluginManager` *(documented: `DefaultPluginManager.java`)* | `[{INSERT FunnyPlugin foo='bar'}]` runs arbitrary Java in-process | runs operator-/admin-permitted Java code | yes; admission is the engine's job, plugin behaviour is **out of model** for the plugin author's code |
| **External plugin JARs** — `jspwiki.plugin.externalJars` *(documented: `jspwiki.properties` lines 401–405)* | added to classpath at runtime | reads operator-configured JAR paths | yes for the loading mechanism; **out of model** for the loaded code itself *(see §3)* |
| **Page-filter chain** (`PageFilter`/`BasePageFilter`) — runs on save/load | wiki text in-flight | no | yes |
| **SpamFilter / Akismet integration** *(documented: `SpamFilter.java`)* | egress HTTPS to Akismet when configured | outbound HTTPS | in-model for credential handling; **out of model** for Akismet's behaviour |
| **AttachmentServlet** *(documented: `AttachmentServlet.java`)* | HTTP `POST` multipart upload, `GET` download | filesystem (`BasicAttachmentProvider` storage dir) | yes |
| **`BasicAttachmentProvider`** + alternative providers | storage of attachment bytes | filesystem | yes |
| **Page store providers** (`FileSystemProvider`, `VersioningFileProvider`, JDBC, …) | persistence of wiki text | filesystem or JDBC | yes |
| **AuthenticationManager + JAAS LoginModules** (`UserDatabaseLoginModule`, `WebContainerLoginModule`, `CookieAssertionLoginModule`, `CookieAuthenticationLoginModule`, `AnonymousLoginModule`) *(documented: `auth/login/`)* | credential check | reads user database, optionally cookie store | yes |
| **AuthorizationManager** + JAAS `Policy` *(documented: `AuthorizationManager.java`, `jspwiki.policy`)* | `checkPermission(Session, Permission)` | no | yes |
| **DefaultAclManager** — inline `[{ALLOW ...}]` directive parsing *(documented: `DefaultAclManager.java`)* | reads wiki text | no | yes |
| **WikiSession / SessionMonitor** | servlet `HttpSession` + JAAS `Subject` | no | yes |
| **Install.jsp + admin/SecurityConfig.jsp** | first-run setup + ongoing security audit UI *(documented: `Installer.java`, `SecurityVerifier.java`)* | reads/writes policy, user DB | yes |
| **Boot-time `SecurityVerificationUtility`** — runs at startup, optionally emails admins on misconfiguration *(documented: `SecurityVerificationUtility.java`)* | sends mail | yes (SMTP) | yes |
| **AuditLogger** *(documented: `org.apache.wiki.security.AuditLogger`, `jspwiki.properties` `audit.enabled=true`)* | structured security events | optionally outbound mail (`audit.alert.to`) | yes |
| **External login backends (LDAP/Keycloak)** *(documented: `jspwiki.properties` lines 1112–1115 — "container managed scenarios does not apply here, nor does external configurations, like keycloak")* | delegated via JAAS | network egress | in-model for credential / role-mapping handling; **out of model** for the IdP itself |
| **Markdown WYSIWYG editor** *(documented: `jspwiki-wysiwyg`)* | HTML ↔ wiki syntax converter | no | yes |
| **WebDAV support** | HTTP — separate URL pattern | no | **conditionally in model**: `ReleaseNotes` line 394 states "WebDAV does not yet support the new authentication/permissions scheme. Therefore, if you have very sensitive data in your wiki, you might not want to enable it." → **OUT OF MODEL** for production security unless the PMC overrides *(inferred — §14 Q4)* |
| **XML-RPC interface** (`jspwiki-xmlrpc/`) | alternative wire protocol | no | in-model if shipped enabled *(inferred — §14 Q5)* |
| **Examples / portable / docker-files / it-tests / wikipages** (`jspwiki-portable`, `jspwiki-it-tests`, `docker-files/`, `jspwiki-wikipages`) | tooling and seed content | varies | **out of model** *(§3 item 7)* |
| **210/211 legacy templates and the 2.10 adapters** (`jspwiki-210-adapters`, `jspwiki-210-test-adaptees`, legacy template dirs) | backward-compat code | n/a | in-model only for currently-supported entry points *(inferred — §14 Q6)* |

A finding is in-model only if it lands in a row marked "yes". The plugin /
filter / Markdown WYSIWYG rows are all "in-process inside the wiki webapp"
— they share the wiki's authority entirely, which materially shapes §3,
§7, and §9.

## §3 Out of scope (explicit non-goals)

JSPWiki is not, and does not aim to be, the following — reports requiring
any of these will be closed with the cited disposition:

1. **A defender against the operator.** Anyone with filesystem access to
   `jspwiki[-custom].properties`, `WEB-INF/jspwiki.policy`, the page
   storage directory, the user/group XML databases, the cookie-login store
   under `$jspwiki.workDir/logincookies`, the servlet container's
   `conf/`/`webapps/`, or the JVM itself has unbounded authority over the
   wiki. "The operator misconfigured X" is not a JSPWiki vulnerability
   *(documented: `jspwiki.properties` `logincookies` warning lines 562–566;
   inferred — §14 Q7)*. → `OUT-OF-MODEL: adversary-not-in-scope`.
2. **A defender against the wiki Admin.** Anyone in the wiki `Admin` group or
   the container `Admin` role is granted `AllPermission` by default
   *(documented: `jspwiki.policy` lines 107–112)* and can also create
   wiki pages, install plugins via uploaded attachments (under appropriate
   permissions), and edit ACLs on any page. They are part of the wiki's TCB.
   *(inferred — §14 Q7)*. → `OUT-OF-MODEL: adversary-not-in-scope`.
3. **A sandbox for plugins, page-filters, page providers, attachment
   providers, search providers, syntax decorators, user/group databases, or
   any other operator-installed or admin-installed Java module.** All of
   these run in-process with the JSPWiki webapp's privileges. Admission to
   the classpath is the operator's call (default search package
   `org.apache.wiki.plugin`, plus `jspwiki.plugin.searchPath`, plus
   `jspwiki.plugin.externalJars`). Once admitted, the plugin has the full
   Java authority of the webapp *(documented: `DefaultPluginManager.java`,
   `jspwiki.properties` lines 376–405; inferred — §14 Q8)*. →
   `BY-DESIGN: property-disclaimed` (§9).
4. **A SecurityManager-based sandbox for wiki content.** Although JSPWiki's
   authorization model is built on Java 2 permissions (`PagePermission`,
   `WikiPermission`, `AllPermission`) and the AuthorizationManager javadoc
   describes it using `AccessController#checkPermission`, the
   `ReleaseNotes` line 410 states "Running with a security manager isn't
   yet supported (see JSPWIKI-129)". This means: the policy file
   `WEB-INF/jspwiki.policy` is read and enforced **by JSPWiki's own
   AuthorizationManager** for wiki-level permissions, **not** by a JVM
   `SecurityManager` for general Java permissions. There is therefore no
   defence against a plugin that simply calls
   `System.exec("rm -rf /")` — and on JDK 17+ this defence is anyway
   unavailable from the JVM *(documented: `ReleaseNotes` line 410,
   `AuthorizationManager.java` lines 95–99 "If security not set to JAAS,
   will return true"; inferred — §14 Q9)*. → `BY-DESIGN: property-disclaimed`
   (§9). **This is a high-priority §14 question to confirm.**
5. **A defender against an authorized editor of a page changing that
   page's ACL.** An ACL is embedded in the wiki text as `[{ALLOW ...}]`.
   By construction, anyone who has `edit` on a page can rewrite the ACL —
   that is the wiki's editorial model, not a privilege-escalation bug
   *(documented: `DefaultAclManager.java` lines 73, 174–176)*. → `BY-DESIGN:
   property-disclaimed`.
6. **A defender against malformed-but-parseable wiki text or Markdown.**
   The markup parser is robust against malformed input in the sense that
   it should not corrupt JVM memory or escalate trust, but rendering
   slow paths, infinite loops, exception-throwing pages, or OOM on
   pathological input are robustness work, not security issues, unless
   they cross a trust boundary *(inferred — §14 Q10)*. → `VALID-HARDENING`
   at most.
7. **Code that ships but is not part of the supported product:**
   `jspwiki-it-tests/`, `jspwiki-portable/`, `docker-files/`, `Dockerfile`,
   `jspwiki-bootstrap/` (build tooling), `jspwiki-bom/` (BOM only),
   `jspwiki-wikipages/` (seed content), test resources under any module's
   `src/test/`, the 2.10/211 legacy templates and adapters
   (`jspwiki-210-adapters`, `jspwiki-210-test-adaptees`) when not the
   selected `jspwiki.templateDir`, `Jenkinsfile`, and `mvn_cheat-sheet.md`
   *(inferred — §14 Q6, Q11)*. → `OUT-OF-MODEL: unsupported-component`.
8. **WebDAV in any deployment that holds "very sensitive data".** Per the
   project's own `ReleaseNotes` line 394, WebDAV "does not yet support the
   new authentication/permissions scheme". JSPWiki ships it for convenience
   but does not extend its security claims to it *(documented; inferred —
   §14 Q4)*. → `BY-DESIGN: property-disclaimed`.
9. **Upstream Java libraries Apache JSPWiki vendors or depends on**
   (Tomcat, log4j, jdom2, oro, Apache Commons FileUpload, Akismet client,
   Lucene, Tika, JavaMail, jakarta.* libraries, etc.). Where JSPWiki
   wraps these, the wrapper boundary is in-model; intrinsic upstream
   vulnerabilities should be reported upstream *(inferred — §14 Q12)*.
   → `OUT-OF-MODEL: unsupported-component` (with an upstream pointer).
10. **The Docker image at Docker Hub** (`apache/jspwiki`). The README
    explicitly states: *"Docker images are not official ASF releases but
    provided for convenience. Recommended usage is always to build the
    source."* *(documented: `README.md` line 118)*. Findings in the Docker
    overlay (`docker-files/`) are `OUT-OF-MODEL: unsupported-component`.
11. **The published wiki site** (`jspwiki-wiki.apache.org`) is itself a
    JSPWiki instance — but a finding that requires logging in to *that*
    specific deployment with specific privileges is a deployment finding
    against the ASF infra-run wiki, not against the JSPWiki software
    *(inferred — §14 Q13)*. → `OUT-OF-MODEL: adversary-not-in-scope`.
12. **The JSPWiki Site repos** (`jspwiki-site`, `jspwiki-asf-docs`).
    Dormant; out of scope per the chair's confirmation.

## §4 Trust boundaries and data flow

JSPWiki has at least the following trust transitions; a finding is in-model
only when it cleanly maps to one of them.

| # | Transition | Authentication | Authorization |
| --- | --- | --- | --- |
| B1 | Network peer → wiki HTTP endpoint | Servlet container (P3) or JSPWiki form/cookie (P1/P2) — driven by `AuthenticationManager.login(HttpServletRequest)` *(documented: `AuthenticationManager.java` lines 80–105)* | `AuthorizationManager.checkPermission(Session, Permission)` *(documented: `AuthorizationManager.java`)* |
| B2 | Network peer → `AttachmentServlet` `POST` (upload) | same as B1 | `PagePermission(att, "upload")` *(documented: `AttachmentServlet.java` line 589–590)*, plus filename allow/forbid lists *(documented: `AttachmentServlet.isTypeAllowed`)*, plus optional `jspwiki.attachment.maxsize` |
| B3 | Network peer → `AttachmentServlet` `GET` (download) | same as B1 | `PagePermission(att, "view")` *(documented: `AttachmentServlet.java` line 209–214)*, plus `Content-Disposition: inline` vs `attachment` per `jspwiki.attachment.forceDownload` |
| B4 | Wiki text → MarkupParser → XHTML | wiki text is fully attacker-controlled (any editor's bytes) | `jspwiki.translatorReader.allowHTML` (default `false`) is the only switch; the parser HTML-escapes everything else *(documented: `MarkupParser.java` line 74, `JSPWikiMarkupParser.java` lines 141–142, 254–255, 300–301, 425–426)* |
| B5 | Wiki text → ACL parser (`DefaultAclManager`) | wiki text is attacker-controlled, but only by an editor who already has `edit` on the page | regex extraction, then `AuthorizationManager.resolvePrincipal()` *(documented: `DefaultAclManager.java` lines 73, 174)* |
| B6 | Wiki text → Plugin invocation (`[{INSERT Plugin ...}]`) | wiki text is attacker-controlled | `jspwiki.translatorReader.runPlugins` is the master switch *(documented: `MarkupParser.java` line 77)*; admission of *which* plugin classes are discoverable is operator config (search path + external JARs); per-invocation argument validation is the plugin's responsibility |
| B7 | Cookie / form credentials → JAAS LoginModule | LoginModule per `jspwiki.loginModule.class` (default `UserDatabaseLoginModule` — password against XML/JDBC) *(documented: `jspwiki.properties` lines 521–529)*; container-managed in P3 | Login is throttled by `AuthenticationManager` with exponential delay capped at 20 s *(documented: `AuthenticationManager.java` lines 38–48)* |
| B8 | Cookie-asserted identity → `Asserted` Role | the cookie itself is the only credential — JSPWiki's docs label this "still considered to be unsafe, just like no login at all" *(documented: `jspwiki.properties` lines 547–553)* | per `jspwiki.policy` `Role "Asserted"` block lines 79–83 |
| B9 | Cookie-authenticated identity → `Authenticated` Role | filesystem mapping under `$jspwiki.workDir/logincookies` *(documented: `jspwiki.properties` lines 561–566)* | per `jspwiki.policy` `Role "Authenticated"` |
| B10 | Page save → `PageFilter` chain → `SpamFilter` → egress | wiki text in flight; SpamFilter optionally calls Akismet | `SpamFilter` may bypass authenticated users (`ignoreauthenticated`) or operator-listed groups (`jspwiki.filters.spamfilter.allowedgroups`) |
| B11 | Page save → Page provider → storage | provider is operator-configured | filesystem permissions on the page-store dir |
| B12 | Attachment upload → Attachment provider → storage | as above | `jspwiki.attachment.allowed` / `.forbidden` allow/forbid lists; **not enforced for users with `AllPermission`** *(documented: `jspwiki.properties` lines 162–163, 169–173)* |
| B13 | Operator → `WEB-INF/jspwiki.policy` (security policy file) | filesystem permissions on the WAR/lib | reloaded at startup; no live-reload defense |
| B14 | Operator → `jspwiki[-custom].properties` | filesystem permissions on container's `lib/` | as above |
| B15 | Boot → `SecurityVerificationUtility` → optional SMTP email to admins | configured by `jspwiki.securitycheck.enableEmailOfBootCheck` + `jspwiki.securitycheck.destination` *(documented: `jspwiki.properties` lines 1183–1187, `SecurityVerificationUtility.java`)* | startup-time check only |

### Reachability preconditions per family

- **Markup parser / Markdown parser**: reachable from any input that becomes
  wiki text — page save (B11, possibly via attacker-controlled body if
  `Anonymous` has `edit`), or page render with attacker-influenced query
  params if any. A flat "the parser produces XHTML that, when fed to a
  browser, executes JavaScript" finding is in-model **only if**
  `jspwiki.translatorReader.allowHTML = false` (the default) is set —
  otherwise it is `OUT-OF-MODEL: non-default-build` against the
  documented dangerous-option warning *(documented: `jspwiki.properties`
  lines 239–259, `MarkupParser.java` line 72–74)*.
- **Plugin invocation**: reachable from any wiki text the parser sees.
  Findings about *plugin execution* are out of model per §3 item 3
  (`BY-DESIGN`); findings about the plugin-invocation parser itself
  (the `[{INSERT ... }]` regex) are in-model.
- **AttachmentServlet `POST`**: reachable from any caller who can pass
  the `PagePermission(att, "upload")` check, modulo allow/forbid extension
  filter and max-size limit. **For users with `AllPermission`, the
  allow/forbid filter and the maxsize limit are not enforced**
  *(documented: `jspwiki.properties` lines 162–163, 169–173)*.
- **AttachmentServlet `GET`**: reachable from any caller who passes
  `PagePermission(att, "view")`. The `Content-Disposition` header is
  `inline` by default unless `jspwiki.attachment.forceDownload` matches —
  the default forced-download list is `.html .htm .js .pdf .svg .xml`
  *(documented: `jspwiki.properties` line 185)*.
- **ACL parser**: reachable from any editor of the page (the ACL is wiki
  text); see §3 item 5 for the equivalent-harm framing.
- **WikiSession / SessionMonitor**: tied to `HttpSession`; the servlet
  container manages session-cookie security (`Secure`, `HttpOnly`).
  JSPWiki sets its own auth cookie with `Secure` + `HttpOnly` when
  `jspwiki.securecookie=true` (default `true`) *(documented:
  `jspwiki.properties` lines 668–670)*.

## §5 Assumptions about the environment

- **Java runtime**: JDK 17+ *(documented: `README.md` line 46)*.
- **Servlet container**: Servlet API 6.0; Tomcat 10.1+ recommended
  *(documented: `README.md` lines 38–39)*.
- **Operating system**: any OS supported by the chosen JVM and servlet
  container; no OS-specific assumptions *(inferred — §14 Q14)*.
- **Filesystem**: operator-controlled, read+write by the JVM process for
  page storage, attachment storage, user/group XML DBs, the cookie-login
  store under `$jspwiki.workDir/logincookies`, lucene index, work dir,
  and log files. Permissions on these directories are operator-managed
  *(documented: `jspwiki.properties` lines 91–101, 130–141, 562–566)*.
- **Network**: operator-controlled; TLS termination is the servlet
  container's responsibility. The `<security-constraint>` in `web.xml`
  shipped with the WAR includes a block requesting `CONFIDENTIAL`
  transport for the whole webapp ("this block will require TLS across
  all the board" — `web.xml`) *(documented)*.
- **Time**: cookie expiry, session expiry, lock expiry
  (`jspwiki.lockExpiryTime = 60` minutes), cookie-auth expiry
  (`jspwiki.cookieAuthentication.expiry = 14` days default) all assume a
  reasonable wall clock; no monotonic-clock claim *(inferred — §14 Q14)*.
- **JVM SecurityManager**: **not** assumed to be active. JSPWiki does
  **not** support being run under a Java SecurityManager *(documented:
  `ReleaseNotes` line 410)*. This is the single highest-impact
  environmental claim and is captured separately in §9.
- **SMTP**: required only if `audit.alert.to`,
  `jspwiki.securitycheck.enableEmailOfBootCheck`, or user-profile
  password-reset email is enabled *(documented: `jspwiki.properties`
  lines 1152–1187)*.
- **External LDAP / Keycloak / SAML IdP**: assumed honest when configured
  *(inferred — §14 Q15)*.
- **Akismet** (SpamFilter): assumed honest when configured *(inferred —
  §14 Q15)*.
- **What JSPWiki does NOT do to its host** (negative claims, awaiting
  maintainer ratification):
  - does **not** spawn child processes from the core *(inferred —
    §14 Q16)*;
  - does **not** install JVM-wide signal handlers *(inferred —
    §14 Q16)*;
  - does **not** mutate the global Locale or default charset for
    security-sensitive operations *(inferred — §14 Q16)*;
  - does **not** require any specific UID under which the servlet
    container runs;
  - does **not** persist credentials in cleartext on disk — the XML
    UserDatabase hashes passwords (SHA-1 per the comment at
    `jspwiki.properties` lines 628–630, which is itself flagged —
    §14 Q17), the cookie-auth store stores opaque cookie-to-user
    mappings *(documented: `jspwiki.properties` lines 561–566)*.

## §5a Build-time and configuration variants

JSPWiki ships as a single WAR but a sizable number of runtime properties
materially change the security envelope. The maintainer-confirmed list is
in `jspwiki-main/src/main/resources/ini/jspwiki.properties`; the
security-relevant subset:

| Knob | Default | Maintainer stance | Effect |
| --- | --- | --- | --- |
| `jspwiki.translatorReader.allowHTML` | `false` *(documented)* | "THIS IS A DANGEROUS OPTION!" / "Most probably you want to use this on Intranets, or personal servers" *(documented: `jspwiki.properties` lines 242–253)* | when `true`, raw HTML in page text is rendered without escaping → XSS vector; per `ChangeLog.md` line 374, the recent fix for "XBOW-024-109 XSS in JSPWiki Header Link Name" ensured "proper HTML escaping when `jspwiki.translatorReader.allowHTML` is disabled" — so a finding *with* `allowHTML=true` is `OUT-OF-MODEL: non-default-build` |
| `jspwiki.translatorReader.runPlugins` | `true` *(inferred — §14 Q18)* | the default *(inferred)* | when `false`, `[{INSERT ...}]` plugin syntax is not executed |
| `jspwiki.loginModule.class` | `UserDatabaseLoginModule` *(documented)* | the P2-posture default | swapping for a non-throttled custom module voids §8 P3 |
| `jspwiki.cookieAssertions` | `true` *(documented — `jspwiki.properties` line 554)* | dev/intranet convenience; "considered to be unsafe, just like no login at all" *(documented: lines 547–553)* | flipping off forces real login |
| `jspwiki.cookieAuthentication` | `false` *(documented)* | "comes with important security caveats" *(documented: lines 558–566)* | when `true`, 14-day persistent login via filesystem-stored cookie store; access to that store == cluster compromise |
| `jspwiki.login.throttling` | `true` *(documented: line 543)* | the documented default | turning off voids the exponential-delay defense (§8 P3 violation symptom) |
| `jspwiki.securecookie` | `true` *(documented: line 670)* | the documented default | when `false`, JSPWiki's own auth cookies lose `Secure`+`HttpOnly` — explicitly intended for "non-SSL/TLS connections" *(documented)* — `OUT-OF-MODEL: non-default-build` for a finding that relies on the cookie travelling cleartext |
| `jspwiki.attachment.allowed` | empty (= "all types allowed") *(documented: lines 171–178)* | the documented default | configuration of attachment extension allowlist; **NOT enforced for `AllPermission` users** |
| `jspwiki.attachment.forbidden` | unset *(documented: line 181)* | dev default | as above; **NOT enforced for `AllPermission` users** |
| `jspwiki.attachment.maxsize` | unbounded (`Integer.MAX_VALUE`) *(documented: lines 160–165, `AttachmentServlet.java` line 95)* | dev default | similarly **NOT enforced for `AllPermission` users** |
| `jspwiki.attachment.forceDownload` | `.html .htm .js .pdf .svg .xml` *(documented: line 185)* | the documented default | `Content-Disposition: attachment` instead of `inline` for matching extensions; cuts off XSS via attachment view |
| `jspwiki.authorizer` | `WebContainerAuthorizer` *(documented: line 598)* | the documented default | swapping forces a custom Authorizer; B3 changes |
| `jspwiki.aclManager` | `DefaultAclManager` *(documented: line 665)* | the documented default | swapping changes the ACL syntax |
| `jspwiki.userdatabase` | `XMLUserDatabase` *(documented: line 626)* | the documented default for P2 | passwords SHA-1 hashed per docs (lines 628–630) — **see §14 Q17** |
| `jspwiki.groupdatabase` | `XMLGroupDatabase` *(documented: line 606)* | the documented default | as above |
| `jspwiki.credentials.length.min` / `minUpper` / `minLower` / `minDigits` / `minSymbols` / `repeatingCharacters` / `minChanged` / `reuseCount` | 8 / 1 / 1 / 1 / 1 / 1 / 1 / -1 *(documented: lines 1117–1151)* | "8 is the default. 15 or more is recommended for high security systems"; reuseCount default `-1` (disabled) | password-complexity defaults; do not apply in P3 (container) or external IdP postures |
| `jspwiki.role.admin`, `jspwiki.role.authenticated`, `jspwiki.role.extraRoles` | unset *(documented: lines 1140–1146)* | optional external-role mapping (3.0.0+) | maps external IdP roles to JSPWiki built-in roles |
| `jspwiki.policy.file` | `jspwiki.policy` in `WEB-INF/` *(documented: `AuthorizationManager.java` line 67–70)* | the documented default | the file referenced contains `AllPermission` for `Admin` |
| `web.xml` `<security-constraint>` blocks for "Administrative Area" and "Authenticated area" | **commented out by default** *(documented: `web.xml`, marked "REMOVE ME TO ENABLE CONTAINER-MANAGED AUTH")* | P3 enablement | when uncommented, switches to container-managed auth and enables CONFIDENTIAL transport requirement |
| `audit.enabled` | `true` *(documented: line 1154)* | the documented default | audit logging via log4j2 (`SecurityLog` logger) |
| `jspwiki.securitycheck.enableEmailOfBootCheck` | `true` *(documented: line 1186)* | the documented default | boot-time security check results emailed to `jspwiki.securitycheck.destination` |
| `jspwiki.plugin.externalJars` | empty *(documented: lines 401–405)* | the documented default | when non-empty, named JARs added to the wiki's classpath → arbitrary code under operator's control |

### The insecure-default cases

JSPWiki ships with several settings where the *default* is the
**less-secure** value (`Anonymous` can modify pages; `cookieAssertions=true`;
empty allow/forbid attachment lists; no max attachment size; container
`<security-constraint>` blocks commented out). The maintainer ruling on
each is captured in §14 Q19; the text of §3 item 1, §10, and §11
assumes the answer is **"this is the dev / open-intranet default; operator
must lock down per §10 for production"**.

## §6 Assumptions about inputs

### Per-endpoint trust table (network surfaces)

| Surface / route | Parameter | Attacker-controllable? | Caller must enforce |
| --- | --- | --- | --- |
| All wiki HTTP endpoints | session cookie | **yes** (forgeable only with cookie-secret-equivalent access) | per §10, set `jspwiki.securecookie=true` (default), terminate TLS at the container |
| `POST` edit page | wiki text body | **yes** | nothing — JSPWiki parses, escapes per `allowHTML`, runs ACL parser, runs SpamFilter |
| `POST` edit page | ACL directive inside body | **yes** if the caller has `edit` on the page | by-design: any editor can rewrite the ACL (§3 item 5) |
| `POST` edit page | plugin invocation inside body | **yes** if the caller has `edit` AND `jspwiki.translatorReader.runPlugins=true` | which plugin classes are discoverable is operator config |
| `POST` `AttachmentServlet` (upload) | multipart filename | **yes** | per §10, set `jspwiki.attachment.allowed`/`.forbidden`/`.maxsize` for the deployment; **note these do not apply to `AllPermission` users** *(documented: `jspwiki.properties` lines 163, 173)* |
| `POST` `AttachmentServlet` (upload) | multipart bytes | **yes** | content scanning, if any, is the operator's job |
| `GET` `AttachmentServlet` (download) | URL parameters (`wikiname`, `version`) | **yes** | `forceDownload` extension list cuts off inline rendering |
| `POST` login form | username / password | **yes** | login throttling per `AuthenticationManager.java` provides exponential-delay defence |
| `Install.jsp` first-run endpoint | install form | **yes** to any reachable network peer until run | per §10, this endpoint must be reached **only once on a fresh install** and then access disabled at the container level *(inferred — §14 Q20)*; `Installer.adminExists()` check is the only guard |
| `admin/SecurityConfig.jsp` | session of an admin user | **yes** by admin | per §10, restrict `/admin/*` to admin role at the container level |
| `web.xml` `<role-name>` `Authenticated` / `Admin` | container assertion that the principal has the role | trusted | role mapping is the container's responsibility |
| Cookie-auth store path under `$jspwiki.workDir/logincookies` | cookie ↔ user mapping | **trusted** filesystem path | operator must restrict OS-level read access |
| `jspwiki.policy` | policy syntax | trusted | filesystem permissions; not live-reloaded |
| `jspwiki-custom.properties` (in `$TOMCAT_HOME/lib`) | property overrides | trusted | filesystem permissions |
| Akismet (when configured) | API response | trusted only as far as Akismet is trusted | per §10, model Akismet as data crossing a trust boundary |
| LDAP / Keycloak / SAML IdP (when configured) | role / identity assertions | trusted only as far as the IdP is trusted | per §10, treat IdP failure as a wiki failure |

### Size / shape / rate

- Page text is **unbounded** at the engine level; the markup parser pushback
  buffer is 10 KB per line *(documented: `MarkupParser.java` line 50)* but
  there is no overall page-size cap *(inferred — §14 Q21)*.
- Attachment size: capped by `jspwiki.attachment.maxsize` (default unset →
  `Integer.MAX_VALUE`) *(documented: `AttachmentServlet.java` line 95)*.
  **Not enforced for `AllPermission` users.**
- Login attempts per principal: throttled by `AuthenticationManager` with
  `2^n ms` delay, capped at 20 s, decaying over 10 minutes *(documented:
  `AuthenticationManager.java` lines 38–48)*. The throttling is **per
  account, not per source IP** *(inferred — §14 Q22)* — so credential
  stuffing from a single IP attempting many accounts is not throttled at
  this layer.
- Spam-filter rate limits: `pagechangesinminute` (default 5),
  `similarchanges` (default 2), `bantime` (default 60 min) — but only
  active if `SpamFilter` is enabled in the page-filter chain
  *(documented: `SpamFilter.java` lines 91–98, 200–212)*.
- Number of plugin invocations per page: not bounded at the engine
  level *(inferred — §14 Q23)*.

## §7 Adversary model

### Actors

| Actor | In scope? | Capabilities granted |
| --- | --- | --- |
| Unauthenticated network peer | **yes** | TCP/HTTP to the wiki port; may attempt login; may issue all anonymous-allowed operations (which by default include `modify` and `createPages` per shipped `jspwiki.policy`) |
| `Asserted` user (self-cookie identity) | **yes** | as above, plus whatever the `jspwiki.policy` `Role "Asserted"` block grants (default: `modify` `*:*`, `createPages`, `GroupPermission view`) — JSPWiki's own docs label this trust level "unsafe, just like no login at all" |
| `Authenticated` end user (low Ranger-equivalent) | **yes** | execute editorial actions per their `PagePermission`/`WikiPermission`/`GroupPermission` set |
| Group member of a wiki Group | **yes** | as above, plus group-mediated permissions |
| Authenticated user with `edit` on a page | **yes** | rewrite the ACL of that page, embed plugin invocations, embed wiki-syntax that produces inline images / external links / etc. |
| Anyone who can `upload` an attachment | **yes** | land arbitrary bytes; if filename matches `forceDownload`, browser downloads; otherwise browser may inline; **for `AllPermission` users the extension allow/forbid filter and max size do not apply** |
| Plugin author whose plugin is on the discoverable search path | **out of scope** as an adversary — they are TCB by definition |
| Wiki Admin (`AllPermission`) | **out of scope** — see §3 item 2 |
| Operator (filesystem / container / JVM) | **out of scope** — see §3 item 1 |
| Akismet operator / LDAP / Keycloak / IdP | **out of scope** — see §5 |
| Side-channel observer (timing, cache) | **out of scope** *(inferred — §14 Q24)* |
| Same-host non-JVM-process attacker | **partial** *(inferred — §14 Q25)*: same-host process with the JVM's UID has effectively `AllPermission`; with a different UID, the OS protects the page store / cookie store |
| Quantum adversary | **out of scope** |

### Authenticated-but-Byzantine internal peer

JSPWiki is a single-process webapp, not a clustered consensus system; there
is no notion of "internal peer" that needs Byzantine modelling. Multi-
node clustering, where it exists, is via the underlying servlet container's
session replication and the operator's choice of page provider —
inter-node behaviour is the operator's problem *(inferred — §14 Q26)*.

## §8 Security properties the project provides

For each property: condition, violation symptom, severity tier, provenance.

### P1 — Authentication of users via the configured JAAS LoginModule

- **Condition**: `jspwiki.loginModule.class` is set to a real LoginModule
  (default `UserDatabaseLoginModule`) and the user database is properly
  populated *(documented: `jspwiki.properties` lines 521–529,
  `AuthenticationManager.java`)*; OR `web.xml` `<security-constraint>` is
  uncommented (P3).
- **Violation symptom**: a peer with no valid credential successfully
  becomes `Role.AUTHENTICATED`.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P2 — Authorization of every wiki-level action via `AuthorizationManager.checkPermission`

- **Condition**: every protected entry point obtains the relevant
  `PagePermission`/`WikiPermission`/`GroupPermission`/`AllPermission` and
  calls `checkPermission(session, permission)` before performing the
  action *(documented: `AuthorizationManager.java`,
  `AttachmentServlet.java` lines 209, 589)*.
- **Violation symptom**: a session performs an action without
  `AuthorizationManager.checkPermission` having returned true for the
  appropriate permission.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P3 — Login throttling against brute-force password guessing

- **Condition**: `jspwiki.login.throttling=true` (default) and the active
  LoginModule routes through `AuthenticationManager.login(Session, request,
  username, password)` *(documented: `AuthenticationManager.java` lines
  38–48, `jspwiki.properties` line 543)*.
- **Violation symptom**: an attacker can make N login attempts against the
  same account in less than `sum(2^k ms for k in 0..N-1)`, capped at 20 s
  per attempt, decaying over 10 minutes.
- **Severity**: **security-relevant**, `VALID` per §13.
- *(documented)*
- **Caveat**: the throttle is per *account*, not per source IP — see
  §14 Q22.

### P4 — XHTML escaping of wiki text by default (XSS prevention)

- **Condition**: `jspwiki.translatorReader.allowHTML = false` (default)
  *(documented: `jspwiki.properties` lines 239–259, `MarkupParser.java`
  line 74)*. `JSPWikiMarkupParser` HTML-escapes raw text via
  `TextUtil.escapeHTMLEntities` for the default path *(documented:
  `JSPWikiMarkupParser.java` lines 254–255, 300–301, 425–426)*. The
  Markdown module similarly respects `allowHTML` per `ChangeLog.md`
  line 375.
- **Violation symptom**: an editor-supplied bytes string in page text or in
  a page parameter results in attacker-controlled JavaScript executing in
  another reader's browser context.
- **Severity**: **security-critical**, `VALID` per §13. This is the
  single most-recurring class in JSPWiki's history (see §11 / §11a).
- *(documented)*

### P5 — Per-page ACL enforcement on top of policy permissions

- **Condition**: ACL is embedded as `[{ALLOW <action> <principal>...}]` in
  the page text *(documented: `jspwiki.properties` lines 660–664,
  `DefaultAclManager.java` line 73)*. The decision is the **union** of
  the ACL and the local policy *(documented: `AuthorizationManager.java`
  lines 50–55: "the user must be named in the ACL ... and be granted (at
  least) the same permission in the security policy. We do this to prevent
  a user from gaining more permissions than they already have, based on
  the security policy."*).
- **Violation symptom**: an action is permitted that the union of
  policy + ACL denies, OR a denied action succeeds.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P6 — Attachment extension allow/forbid filtering, when configured

- **Condition**: `jspwiki.attachment.allowed` and/or
  `jspwiki.attachment.forbidden` are set; the uploading principal does
  **not** have `AllPermission` *(documented: `jspwiki.properties` lines
  162–183, `AttachmentServlet.isTypeAllowed`)*.
- **Violation symptom**: a non-Admin uploads an attachment with a
  forbidden extension or outside the allowed-extension set.
- **Severity**: **security-relevant**, `VALID` per §13.
- *(documented)*
- **Note**: the allow/forbid list is a *filename suffix* match, lowercased,
  and runs forbid-first then allow *(documented: `AttachmentServlet.java`
  lines 143–160)*. Allow-list-not-set defaults to "all allowed".

### P7 — Attachment force-download for browser-dangerous types

- **Condition**: `jspwiki.attachment.forceDownload` lists the extension
  (default `.html .htm .js .pdf .svg .xml` — `*` forces all)
  *(documented: `jspwiki.properties` line 185)*.
- **Violation symptom**: an attachment whose extension matches the
  forceDownload list is served with `Content-Disposition: inline`
  (allowing browser to render it in-context with the wiki origin).
- **Severity**: **security-critical**, `VALID` per §13 — this is a
  same-origin XSS mitigation.
- *(documented)*

### P8 — Cookie auth/assertion cookies carry `Secure` + `HttpOnly` by default

- **Condition**: `jspwiki.securecookie=true` (default)
  *(documented: `jspwiki.properties` lines 668–670)*.
- **Violation symptom**: JSPWiki's own auth/assertion cookies emitted
  without `Secure` and `HttpOnly` despite the property being true.
- **Severity**: **security-relevant**, `VALID` per §13.
- *(documented)*

### P9 — Audit log of security-relevant events (creation, login, denial)

- **Condition**: `audit.enabled=true` (default)
  *(documented: `jspwiki.properties` line 1154,
  `org.apache.wiki.security.AuditLogger`)*.
- **Violation symptom**: a security-relevant event listed in
  `WikiSecurityEvent` (LOGIN_AUTHENTICATED / LOGIN_FAILED / ACCESS_DENIED
  / GROUP_ADD / GROUP_REMOVE / etc.) is not emitted to the configured
  log appender.
- **Severity**: **security-relevant**; `VALID` per §13 for the
  data-protection deployment that depends on the trail.
- *(documented)*

### P10 — Boot-time security verification

- **Condition**: at engine startup, `SecurityVerificationUtility` runs
  the same checks as `admin/SecurityConfig.jsp` and, if
  `jspwiki.securitycheck.enableEmailOfBootCheck=true` (default), emails
  the results to `jspwiki.securitycheck.destination` *(documented:
  `SecurityVerificationUtility.java`, `jspwiki.properties` lines
  1183–1187)*.
- **Violation symptom**: misconfigurations the verifier knows about
  (policy parse errors, missing user database, group database
  reachability, JAAS configuration errors, roles not resolvable) reach
  production without being surfaced to the operator.
- **Severity**: **security-relevant**, `VALID` per §13.
- *(documented)*

### P11 — Memory safety of the safe-Java core

- **Condition**: input matches the documented contract; the host
  conforms to §5; the JVM is the runtime — JVM bytecode verification +
  GC provides memory safety in the engine itself *(inferred — §14 Q27)*.
  Plugin code, FFI, and JNI in operator-supplied dependencies are out of
  this property's scope.
- **Violation symptom**: heap corruption, OOB, UAF reachable from a §6
  input via JSPWiki's own code.
- **Severity**: **security-critical** when reachable; **`VALID-HARDENING`**
  when reachable only via debug configurations.
- *(inferred — §14 Q27)*

## §9 Security properties the project does *NOT* provide

State each plainly so a triager can route an inbound report to the matching
disclaimer.

- **No JVM-level sandbox for plugins, filters, providers, or any other
  operator-loaded code.** Java SecurityManager support is **not** present
  per `ReleaseNotes` line 410 ("Running with a security manager isn't yet
  supported (see JSPWIKI-129)"), and on JDK 17+ SecurityManager is
  deprecated for removal anyway. Plugin code runs with the full authority
  of the JSPWiki webapp; that authority is delimited only by the
  servlet-container's process posture *(documented + inferred —
  §14 Q9)*.
- **No defence against XSS when `jspwiki.translatorReader.allowHTML=true`.**
  The option is documented as "DANGEROUS" *(documented: `jspwiki.properties`
  line 242)*; turning it on accepts arbitrary HTML/JS/ActiveX in page
  text.
- **No protection against the wiki Admin.** `AllPermission` grants
  arbitrary `PagePermission`/`WikiPermission`/`GroupPermission`; admins
  can also install plugins and change `jspwiki.policy` if they have
  filesystem access. See §3 item 2.
- **No protection against the operator** (filesystem, container, JVM).
  See §3 item 1.
- **No constant-time comparison of authentication secrets** beyond what
  the underlying JAAS LoginModule / JCE library provides *(inferred —
  §14 Q28)*.
- **No defence against decompression / decoding bombs uploaded as
  attachments.** The wiki stores and serves the bytes; rendering is the
  browser's job *(inferred — §14 Q29)*. For attachments whose extension
  matches `jspwiki.attachment.forceDownload`, the browser is told to
  download rather than render, which closes off browser-side
  amplification at the wiki origin.
- **No defence against pathological wiki text causing parser
  super-linearity or OOM** *(inferred — §14 Q21)*. The `PUSHBACK_BUFFER_SIZE`
  limits a single push-back to 10 KB but does not bound total page size or
  total parse cost.
- **No quota / rate limit on operations beyond what SpamFilter or the
  servlet container provides** *(inferred — §14 Q23)*.
- **No data-at-rest encryption.** Pages and attachments are stored as
  files (or DB rows) by the configured providers; encryption is the
  operator's problem *(inferred — §14 Q30)*.
- **No defender stance against an attacker on the same Linux host
  running as the same UID as the servlet container** — that attacker
  already has full wiki authority *(inferred — §14 Q25)*.
- **No cryptographic integrity of stored pages or attachments.** Pages
  are revisioned (via `VersioningFileProvider`) but the version history
  is not signed; an operator-level write can rewrite history *(inferred
  — §14 Q31)*.
- **No defence against credential-stuffing across many accounts from a
  single IP.** Throttling is per-account *(inferred — §14 Q22)*.
- **No claim that the bundled WebDAV endpoint enforces the same
  authentication and authorization** as the rest of the wiki, per
  `ReleaseNotes` line 394.

### False-friend properties (call out separately)

- **The ACL syntax `[{ALLOW edit Charlie}]` is wiki text, not
  out-of-band policy.** Anyone with `edit` on the page can rewrite the
  ACL. See §3 item 5.
- **`Asserted` is *not* authentication.** A user-supplied cookie sets
  the identity; JSPWiki itself docs it as "considered to be unsafe, just
  like no login at all" *(documented: `jspwiki.properties` line 549)*.
- **`Anonymous` having `modify` on `*:*` is the *default*, not a
  hardened posture.** The shipped `jspwiki.policy` warns "For
  Internet-facing wikis, you are strongly advised to remove the lines
  containing the 'modify' and 'createPages' permissions" *(documented:
  `jspwiki.policy` lines 60–62)*.
- **The attachment allow/forbid list and max-size limit do NOT apply to
  `AllPermission` users.** *(documented: `jspwiki.properties` lines 163,
  173)* — so a finding "admin can upload `.exe` despite forbidden list"
  is by design.
- **Login throttling is per account, not per IP.** A single IP can mount
  credential stuffing across many usernames without hitting the
  throttle.
- **`jspwiki.policy` is parsed at startup; ACL changes are live.**
  Operators who think policy changes apply immediately will be
  surprised.
- **The JSPWiki authorization permissions (`PagePermission`, etc.) are
  enforced by JSPWiki itself, NOT by the JVM SecurityManager.** A bare
  reading of `AuthorizationManager.java` suggests a SecurityManager
  interaction; `ReleaseNotes` line 410 explicitly disclaims it.
- **The cookie-auth filesystem store under `$jspwiki.workDir/logincookies`
  is a credential store, not a cache.** Operators who treat it as a
  cache and put it on a shared filesystem give away wiki accounts.
- **The SHA-1 hashing of XMLUserDatabase passwords is not the
  password-storage state of the art.** *(documented: `jspwiki.properties`
  lines 628–630; see §14 Q17)*.
- **The Spam filter's `ignoreauthenticated`/`allowedgroups` bypass is a
  trust elevation.** Listed groups skip *all* spam checks including
  rate-limiting.

### Well-known attack classes the project does not defend against

- **XSS via raw HTML when `allowHTML=true`** — by design.
- **XSS via UI surfaces that historically have been the source of
  recurring CVEs (page rename parameter, breadcrumb trail, plugin
  parameters, profile fields, etc.).** Each individual leak is a
  `VALID` bug, but the *pattern* — "user-supplied text reaches an HTML
  template without escaping" — is a recurring class that the project
  fixes case-by-case rather than by a centralized escaping invariant
  *(documented pattern: `ChangeLog.md` lines 373–375, 750, 937–938,
  984, 1525, 1781, 1794, 1875, 1902, 1961, 1970, 1996)*.
- **Confused-deputy via plugins that take attacker-influenced
  parameters and emit HTML.** Each plugin must do its own escaping.
- **Spam edits when the SpamFilter is not enabled in the page-filter
  chain** — the engine does not enforce spam-defence by default.
- **DoS via large attachments when `jspwiki.attachment.maxsize` is not
  set.**
- **Account-takeover via a stolen cookie when
  `jspwiki.cookieAuthentication=true` and the
  `$jspwiki.workDir/logincookies` file is exposed.**
- **Cross-site request forgery on POST endpoints** — JSPWiki's CSRF
  defence is not enumerated as a §8 property *(inferred — §14 Q32)*.

## §10 Downstream responsibilities

The operator deploying JSPWiki in production **must**:

1. **Lock down `jspwiki.policy` before going public.** The shipped
   `jspwiki.policy` block for `Role "Anonymous"` grants `modify` on
   `*:*` and `createPages`. Per the comments in the file itself, "For
   Internet-facing wikis, you are strongly advised to remove the lines
   containing the 'modify' and 'createPages' permissions" *(documented:
   `jspwiki.policy` lines 60–62)*.
2. **Enable the appropriate authentication posture (P2 or P3).** For
   P3, uncomment the `<security-constraint>` blocks in
   `WEB-INF/web.xml` and configure the container's JAAS realm. For P2,
   confirm `jspwiki.loginModule.class` and the user database location.
3. **Set `jspwiki.cookieAssertions=false` unless an open-intranet
   posture is what is wanted.** *(documented: `jspwiki.properties`
   line 547–553)*.
4. **Set `jspwiki.cookieAuthentication=false` unless persistent
   browser-side login is needed AND the `$jspwiki.workDir/logincookies`
   store is on a filesystem only the wiki process can read.**
5. **Set `jspwiki.securecookie=true` (the default).**
6. **Set `jspwiki.translatorReader.allowHTML=false` (the default) on
   any wiki where untrusted users have `edit`.**
7. **Set `jspwiki.attachment.allowed` / `.forbidden` / `.maxsize`
   appropriately.** Remember that **these limits do not apply to users
   with `AllPermission`** *(documented: `jspwiki.properties` lines 163,
   173)*.
8. **Keep `jspwiki.attachment.forceDownload` set for at least
   `.html .htm .js .svg`.** Trimming this list undoes a same-origin
   XSS mitigation.
9. **Lock down filesystem permissions** on:
   - `WEB-INF/jspwiki.policy`,
   - `jspwiki[-custom].properties` (in `$TOMCAT_HOME/lib`),
   - `jspwiki.xmlUserDatabaseFile` and `jspwiki.xmlGroupDatabaseFile`
     (recommended location: `/etc` or container `conf/`)
     *(documented: `jspwiki.properties` lines 609–614)*,
   - the page storage directory,
   - the attachment storage directory,
   - `$jspwiki.workDir/logincookies` (read-protected at the OS level)
     *(documented: `jspwiki.properties` lines 562–566)*.
10. **Configure SpamFilter** in the page-filter chain if the wiki is
    open to anonymous editing, even if just `Asserted`-level. The
    SpamFilter is the only built-in rate-limit and IP-ban mechanism.
11. **Configure password policy** above the very-defensive defaults
    (`jspwiki.credentials.length.min ≥ 15`, `reuseCount ≥ 5`) for any
    P2-posture wiki with real user accounts.
12. **Restrict access to `Install.jsp`** at the container level once
    initial install is complete. `Installer.adminExists()` is the only
    in-app guard *(inferred — §14 Q20)*.
13. **Restrict access to `/admin/*`** at the container level to the
    `Admin` role.
14. **Configure `audit.alert.to` / `jspwiki.securitycheck.destination`**
    to a monitored mailbox so the boot-time `SecurityVerificationUtility`
    is heard.
15. **Treat WebDAV as out-of-policy** until the project ships
    authn/authz for it (`ReleaseNotes` line 394).
16. **Do not install plugins from untrusted sources.** Plugin code
    runs with the wiki's full Java authority and there is no
    SecurityManager sandbox.
17. **TLS-terminate at the container.** The shipped `web.xml` requests
    `CONFIDENTIAL` transport for the whole webapp; the operator must
    actually provide the TLS.
18. **Maintain a process for upstream JSPWiki upgrades**, given the
    project's recurring XSS-class CVE history.

## §11 Known misuse patterns

- **Deploying with default `jspwiki.policy` to a public Internet host.**
  Anonymous users get `modify` and `createPages` on `*:*` — the wiki is
  a public anonymous-editing wiki.
- **Enabling `jspwiki.translatorReader.allowHTML=true` on a wiki where
  any untrusted editor can save pages.** Hands every editor a free XSS
  primitive.
- **Operating Install.jsp publicly after install.** Until
  `Installer.adminExists()` returns true, Install.jsp is the path that
  *creates* the admin account.
- **Treating `Asserted` as "logged in".** It is a self-asserted cookie,
  not an authentication.
- **Putting `$jspwiki.workDir/logincookies` on a shared NFS / inside a
  containerized layer that gets backed up to a non-secure store.**
- **Treating attachment `allow`/`forbid` filters and max-size as
  enforceable against admins** — they aren't.
- **Treating `Content-Disposition: inline` for `.txt`, `.html`, or
  `.svg` attachments as safe.** SVG is now in the default
  `forceDownload` list; operators who shorten the list re-open the
  vector.
- **Installing plugins by dropping JARs into
  `jspwiki.plugin.externalJars` without auditing them.** The plugin
  runs with the wiki's full authority.
- **Adding the wiki page that contains
  `[{ALLOW edit JaneDoe}]` then giving `JaneDoe` `view` on the page
  but not `edit`.** Misreading of the ACL semantics — the ACL applies
  to *any* user matching the named principal regardless of how the
  page text was originally authored *(inferred — §14 Q33)*.
- **Putting the wiki XMLUserDatabase file in `WEB-INF/`.** The XML
  file with SHA-1-hashed passwords ends up in the WAR and may be
  cached or copied; the docs recommend `/etc` or container `conf/`.
- **Allowing the SpamFilter's `ignoreauthenticated=true` setting on a
  wiki where authentication is by self-asserted cookie or by
  cookie-auth** — the spam-bypass is granted to a class that does not
  warrant it.
- **Mixing P3 (container auth) and P1 (Anonymous editing) in the
  same web.xml** — the `<security-constraint>` block enabled for
  `/admin/*` only locks down `/admin/*`; the rest of the wiki remains
  open to anonymous editing per `jspwiki.policy`.

## §11a Known non-findings (recurring false positives)

This section is the highest-leverage input for automated agentic security
scans. Each entry: tool symptom, why it is safe under the model, the §
that licenses the call.

- **"`jspwiki.translatorReader.allowHTML` allows arbitrary HTML / JS
  injection."** This is the documented "DANGEROUS OPTION" — turning it
  on accepts arbitrary HTML by design *(documented: `jspwiki.properties`
  line 242)*. → `OUT-OF-MODEL: non-default-build`.
- **"`Role 'Anonymous'` has `modify *:*` permission in `jspwiki.policy`
  out of the box."** Shipped default for the dev / open-intranet
  posture; the file itself documents the lockdown required for
  production. → `OUT-OF-MODEL: non-default-build` if the deployment
  uses the shipped policy publicly.
- **"`Install.jsp` is reachable from the network."** Documented
  setup endpoint; protected only by `Installer.adminExists()` check.
  Operator must lock down at the container per §10 item 12. →
  `OUT-OF-MODEL: trusted-input` / `non-default-build`.
- **"`AttachmentServlet` accepts `.exe` upload from an admin."**
  `jspwiki.attachment.forbidden` does not apply to `AllPermission`
  users by design *(documented: `jspwiki.properties` lines 163, 173)*.
  → `BY-DESIGN: property-disclaimed`.
- **"Plugin `[{INSERT FooPlugin}]` can run arbitrary Java code with the
  wiki's authority."** Yes — that is the plugin contract. Admission of
  the plugin class is the operator's job; per-invocation argument
  validation is the plugin's job. → `BY-DESIGN: property-disclaimed`
  per §3 item 3 / §9.
- **"There is no Java SecurityManager confining the wiki."**
  Documented non-property per `ReleaseNotes` line 410 ("Running with
  a security manager isn't yet supported (see JSPWIKI-129)"). →
  `BY-DESIGN: property-disclaimed`.
- **"WebDAV endpoint does not enforce JSPWiki ACLs."** Documented
  non-property per `ReleaseNotes` line 394. → `BY-DESIGN:
  property-disclaimed`.
- **"User passwords are SHA-1-hashed in XMLUserDatabase."** Documented
  default per `jspwiki.properties` lines 628–630. This **may** be
  re-classified as `VALID-HARDENING` per the PMC's call (§14 Q17). →
  pending maintainer ruling.
- **"`jspwiki.cookieAuthentication=true` exposes 14-day persistent
  sessions vulnerable to cookie theft."** Documented trade-off
  *(documented: `jspwiki.properties` lines 558–566)*. → `BY-DESIGN`
  conditional on the operator's choice.
- **"`Asserted` users are not really authenticated."** Stated in the
  property file itself. → `BY-DESIGN`.
- **"ACL directive `[{ALLOW edit X}]` in page text means any editor
  can change page permissions."** Yes — that is the editorial model.
  → `BY-DESIGN: property-disclaimed`.
- **"Login throttling does not stop credential stuffing across many
  accounts from one IP."** Throttle is per-account. → `VALID-HARDENING`
  at most pending Q22.
- **"Container's session cookie does not carry the `Secure` flag."**
  Container's cookies are the container's job. JSPWiki sets its own
  cookie's `Secure`+`HttpOnly` per `jspwiki.securecookie`. →
  `OUT-OF-MODEL: trusted-input`.
- **"XSS vulnerability in `tests/`, `jspwiki-it-tests/`,
  `jspwiki-portable/`, `docker-files/`, `jspwiki-wikipages/`."**
  Unsupported components per §3 item 7. → `OUT-OF-MODEL:
  unsupported-component`.
- **"XSS vulnerability in `templates/210/` or `templates/211/`."**
  These are legacy templates; in-model only if the deployment
  actually selected them via `jspwiki.templateDir`. *(inferred —
  §14 Q6)*. → conditional.
- **"Vulnerability in upstream Tomcat / log4j / commons-fileupload /
  jdom2 / oro / Lucene / Tika / Akismet client."** Report upstream
  *(inferred — §14 Q12)*. → `OUT-OF-MODEL: unsupported-component`.
- **"User-supplied page name in a redirect (`nextPage`) parameter
  causes open-redirect."** `AttachmentServlet.validateNextPage`
  validates against the error-page URL *(documented:
  `AttachmentServlet.java` line 253–257)*. A bare flag without
  exercising the validator is a non-finding.
- **"Self-XSS via raw URL in the address bar."** Self-XSS is not in
  the project's threat model.
- **"DoS via a 1 GB page POST."** Per §9; unless the engine can be
  made to corrupt memory or escalate trust, this is `VALID-HARDENING`
  pending an attachment-size cap or page-size cap.

## §12 Conditions that would change this model

Revise this document when any of the following lands:

- A new client-facing authentication mechanism (e.g. OAuth/OIDC
  first-class support beyond what JAAS allows today, FIDO2/WebAuthn).
- A new authorization mode (e.g. a non-JAAS native authorization
  store, OPA integration, attribute-based access control).
- Re-introduction of Java SecurityManager support, or any other
  plugin-sandboxing mechanism — would materially rewrite §3 item 4,
  §9, §11a.
- A new markup parser beyond the existing JSPWiki-syntax + Markdown
  pair.
- A new attachment provider, page provider, or storage backend that
  changes the data-at-rest story.
- A new bundled deployment shape beyond the shipped WAR (e.g. an
  embedded mode, a SaaS posture).
- A change in the default value of any §5a knob with security
  implications.
- A WebDAV implementation that *does* enforce the ACLs (would change
  §3 item 8 / §11a / §11).
- A change to the XMLUserDatabase password hashing algorithm.
- A vulnerability report that cannot be cleanly routed to one of the
  §13 dispositions — that is evidence the model is incomplete.

## §13 Triage dispositions

A report against JSPWiki receives exactly one of the following:

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a §8 property via an in-scope §7 adversary using an in-scope §6 input. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property violated, but a §11 misuse pattern can be made harder to fall into by code change. Fixed at maintainer discretion, typically no CVE. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a §6 parameter the model marks trusted (e.g. `jspwiki.policy`, `jspwiki[-custom].properties`, the user/group XML DB, the cookie store, Akismet API responses, IdP identity assertions). | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires a §7 actor the model excludes (operator, admin, malicious IdP / Akismet, side-channel observer, same-host same-UID process). | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `jspwiki-it-tests/`, `jspwiki-portable/`, `docker-files/`, `jspwiki-bom/`, `jspwiki-wikipages/`, any module's `src/test/`, `jspwiki-210-adapters` / `jspwiki-210-test-adaptees` when not the selected template, the Docker image overlay, or upstream-vendored libraries. | §3 items 7, 9, 10, 12 |
| `OUT-OF-MODEL: non-default-build` | Only manifests when a §5a knob is set to a non-default, discouraged value (`allowHTML=true`, default-`Anonymous-modify` policy on the public Internet, `cookieAssertions=true` on a wiki claiming real authentication, etc.). | §5a |
| `OUT-OF-MODEL: equivalent-harm` | An actor already-authorized under the model can cause the same harm via a documented path (an editor with `edit` rewriting the ACL; an Admin uploading arbitrary file types; a plugin running arbitrary Java code). | §3 items 2, 5, plugins |
| `BY-DESIGN: property-disclaimed` | Concerns a §9 property the project explicitly does not provide (SecurityManager sandbox, WebDAV authz, data-at-rest encryption, per-IP rate-limit, cryptographic page integrity, etc.). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a recurring false positive. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above — triggers §12 model revision. | §12 |

## §14 Open questions for the maintainers

Every *(inferred)* tag in the body maps to one of these. Proposed answers
are inline; please confirm, correct, or strike. The Security team did not
have direct access to the wiki-hosted Security/CVE pages at draft time
(see Q1), so several of these may already be answered there.

### Wave 1 — meta and external-artefact reconciliation

**Q1.** The JSPWiki PMC publishes two canonical security artefacts **on
the wiki itself**:
`https://jspwiki-wiki.apache.org/Wiki.jsp?page=Security` and
`https://jspwiki-wiki.apache.org/Wiki.jsp?page=CVE`. Both pages were
fetched and back-mapped after the initial draft was produced (see the
appendix's two new rows). The Security page's hardening guidance is
folded into §10; its CSS-injection-permitted-by-default stance lands
in §11a; the CVE page's 28+ entries (spanning 2018–2026, with a
prominent recurring-XSS cluster in 2019/2022) is cited as the
disclosure history that confirms the §9 "well-known attack classes"
list and the §14 Q37 §11a-population ask. **Proposed**: this document
supersedes the Security wiki page's threat-model-shaped content
(claims about trust boundaries, what counts as a vuln) but keeps the
CVE page as the disclosure history. **Asking the PMC to ratify**: does
the fold-in correctly capture the canonical pages' positions, or is
something missing / mis-mapped? *(meta — §3.1a of the rubric)*

**Q2.** Cross-wiki / multi-tenancy stance: a single JSPWiki webapp
serves a single logical wiki (`jspwiki.applicationName`). Confirm that
running multiple wikis in one JVM is **not** a supported isolation
posture (proposed)? *(maps to §2)*

**Q3.** Three deployment postures P1/P2/P3 — agreed (proposed: P1 is
the dev / open-intranet default the shipped `jspwiki.policy` matches,
P2 is the mainstream production posture, P3 is the container-managed
posture)? Is any of the three explicitly out of support? *(maps to
§2)*

### Wave 2 — the SecurityManager question

**Q9.** `ReleaseNotes` line 410: "Running with a security manager isn't
yet supported (see JSPWIKI-129)". On JDK 17+ SecurityManager is
deprecated for removal anyway. We propose:
- (a) the wiki's `PagePermission`/`WikiPermission` etc. are enforced
  by JSPWiki itself, not by a JVM SecurityManager;
- (b) plugins / page-filters / providers run with the full Java
  authority of the webapp;
- (c) `BY-DESIGN: property-disclaimed` for any report that requires
  SecurityManager-style isolation.

Is this the PMC's current stance? Is JSPWIKI-129 still considered
open, or is the official line now "this will never be supported on
modern JDKs"? This is the **single highest-impact §14 question** and
will heavily reshape what counts as a non-finding (§11a) for the
scan. *(maps to §3 item 4, §9 first bullet)*

### Wave 3 — XSS and the markup parser

**Q10.** Cut-line on malformed-input behavior in the markup parser:
proposed: memory-safety break = `VALID`; exception / slow path on
pathological wiki text = `VALID-HARDENING`. Is that the line? *(maps
to §3 item 6, §6, §9)*

**Q21.** Is there a maximum total page-text size the engine accepts,
or is "submit a 1 GB page" an unlimited-input concern? The 10 KB
`PUSHBACK_BUFFER_SIZE` only bounds a single line. *(maps to §6, §9)*

**Q23.** Is there any per-page upper bound on the number of plugin
invocations executed, or the total wall-time of plugin execution per
page render? *(maps to §6, §9)*

### Wave 4 — auth & attachments

**Q4.** WebDAV: `ReleaseNotes` line 394 states WebDAV "does not yet
support the new authentication/permissions scheme". Confirm that
WebDAV is `BY-DESIGN: property-disclaimed` for any access-control
finding and that the entry point is not exposed in current default
deployments? Or has the line become stale (the note has been in the
release notes for a long time)? *(maps to §3 item 8, §9)*

**Q5.** XML-RPC interface (`jspwiki-xmlrpc/`): in-model in current
deployments? What is its auth posture? *(maps to §2)*

**Q17.** XMLUserDatabase password hashing: `jspwiki.properties` lines
628–630 still says "Passwords are SHA-1 hashed". Has this been
upgraded (PBKDF2 / bcrypt / scrypt / Argon2) and the docs not yet
updated? If still SHA-1, is the project's stance "this is the
documented behaviour, operators with serious password-secrecy needs
should use P3 + a real IdP" — making it `BY-DESIGN: property-disclaimed`
rather than `VALID`? *(maps to §5, §9)*

**Q18.** `jspwiki.translatorReader.runPlugins` — is it `true` by
default? *(maps to §5a)*

**Q22.** Login throttling is per-account in
`AuthenticationManager.java` lines 38–48. Confirm: per-IP throttling
is **not** in §8, and credential-stuffing across many accounts from
one IP is `VALID-HARDENING` at most? *(maps to §8 P3, §9, §11a)*

**Q32.** CSRF: does JSPWiki ship a CSRF token / SameSite-cookie
default for state-changing POST endpoints (edit, upload, group
operations, profile change)? If not, is "CSRF on POST endpoints"
`VALID-HARDENING` or `BY-DESIGN: property-disclaimed`? *(maps to
§9)*

**Q19.** The big "insecure defaults" question, per the §5a
insecure-default-case rule. Which of these is the supported production
posture (a `VALID` report when violated), and which is dev/test
(a `OUT-OF-MODEL: non-default-build`)?
- `jspwiki.policy` with `Anonymous` having `modify *:*` and
  `createPages` (the shipped default)
- `jspwiki.cookieAssertions=true` (shipped default)
- `jspwiki.attachment.allowed` empty (= all allowed; shipped
  default)
- `jspwiki.attachment.forbidden` unset (shipped default)
- `jspwiki.attachment.maxsize` unbounded (shipped default)
- `web.xml` `<security-constraint>` for `/admin/*` commented out
  (shipped default)

Proposed across the board: **dev / open-intranet default, operator
must flip per §10 for an Internet-facing production wiki**. *(maps
to §5a, §10, §13)*

**Q20.** `Install.jsp`: the only in-app guard is
`Installer.adminExists()`. Is "Install.jsp reachable from the network
after install" a `VALID` report or a §10-violation `OUT-OF-MODEL:
non-default-build`? *(maps to §6, §10)*

**Q31.** Page revision history: is there any cryptographic integrity
on the version chain (signatures, hash-chain), or is the version
history reliant entirely on filesystem trust? Proposed: filesystem
trust only (operator can rewrite history). *(maps to §9)*

### Wave 5 — environment and side-effects

**Q7.** §3 items 1 (operator) and 2 (Admin) are both out of scope.
Confirm? Specifically: is admin compromise via a stolen `Admin`
group membership a `VALID` `Authentication` finding (because the
attacker shouldn't have become Admin in the first place) but
**not** a `VALID` `PagePermission`/`WikiPermission` finding (because
once they are Admin they legitimately have `AllPermission`)? *(maps
to §3 items 1, 2; §7)*

**Q8.** Plugin admission: agreed that admission of *which* plugin
classes are reachable from the classpath is the operator's job, and
once admitted the plugin runs with the wiki's authority? *(maps to
§3 item 3, §9)*

**Q12.** Upstream dependencies (Tomcat, log4j, jdom2, oro, Apache
Commons FileUpload, Akismet client, Lucene, Tika, JavaMail,
jakarta.* libraries): policy is "report upstream; we pick up via
release"? *(maps to §3 item 9)*

**Q13.** The `jspwiki-wiki.apache.org` deployment is itself a
JSPWiki instance. Reports of misconfigurations on it should be
filed against the ASF Infra-run deployment, not against
`apache/jspwiki` the software — confirm? *(maps to §3 item 11)*

**Q14.** OS / locale / clock assumptions: anything beyond "the JVM
runs and `System.currentTimeMillis()` advances at a normal rate"
to flag? *(maps to §5)*

**Q15.** Akismet, LDAP, Keycloak, container-managed SAML IdP — all
modelled as trusted-control-plane. Confirm? *(maps to §5, §6)*

**Q16.** Confirm the "what JSPWiki does not do to its host"
inventory in §5: no child-process spawn, no signal handlers, no
global Locale mutation, no on-disk credential persistence in
cleartext, no required-UID. *(maps to §5)*

**Q24.** Side-channel observers (cache timing, branch prediction):
out of scope (proposed)? *(maps to §7, §9)*

**Q25.** Same-host process running as a different OS user from
the servlet container: out of scope at the OS level
(filesystem perms protect the page / cookie / user DB), out of
scope at the JVM level (no SecurityManager). Confirm? *(maps to
§7, §9)*

**Q26.** Clustered / replicated deployments: any explicit security
properties at the cluster layer, or all delegated to the container's
session replication? *(maps to §7)*

**Q27.** §8 P11 (memory safety in the engine itself): is this in
the model on inference, and the boundary is "in-model for code
JSPWiki authored; out-of-model for plugin / provider / vendored
code"? *(maps to §8 P11, §9)*

**Q28.** Constant-time comparison of authentication secrets — all
delegated to the JCE / JAAS LoginModule? *(maps to §9)*

**Q29.** Attachment decompression bombs (a zip-bomb uploaded as a
`.zip` attachment): the wiki stores it; the browser-side render is
the issue. Is forced-download for `.zip` desired as a hardening
step, or is "the attachment is just bytes, the browser's call"
final? *(maps to §9)*

**Q30.** Data-at-rest encryption is out of model — the operator
applies it at the page-store / attachment-store level if needed
(proposed)? *(maps to §9)*

**Q33.** ACL semantics — confirm: `[{ALLOW edit X}]` *replaces* the
default policy for the page (intersection still applies), and a
named principal is matched at lookup time, so adding the directive
also has the effect of denying everyone not named? Specifically:
does `[{ALLOW edit JaneDoe}]` *deny edit* to other Authenticated
users? *(maps to §6, §11)*

### Wave 6 — meta finalization

**Q6.** Confirm the unsupported-component list and the policy for
the 2.10 / 211 legacy templates: `jspwiki-210-adapters`,
`jspwiki-210-test-adaptees`, `templates/210/`, `templates/211/`.
Are these in-model when the operator explicitly selects them via
`jspwiki.templateDir=210` (the line documented in
`jspwiki.properties` line 302), or are they out-of-model entirely?
*(maps to §3 item 7, §11a)*

**Q11.** §3 item 7 list of unsupported components — anything to
add or remove? Specifically: is `jspwiki-bootstrap/` (which builds
infrastructure that gets shipped) in or out of model? *(maps to
§3 item 7)*

**Q34.** Where should this document live? Proposed:
`docs/threat-model.md` in `apache/jspwiki`, with the wiki Security
page linking to it. *(meta)*

**Q35.** Is there an existing JSPWiki threat-model document
(Confluence, dev list discussion, the wiki Security page itself
beyond the disclosure-process content) that this should reconcile
against rather than supersede? *(meta — §3.1a of the rubric)*

**Q36.** What kind of change to JSPWiki should trigger a revision
of this model (proposed list in §12 — confirm or correct)?
*(meta, §12)*

**Q37.** Recurring-false-positive harvest: §11a is moderately
populated from documented stances. The single most useful PMC
contribution to §11a would be 3–5 patterns the PMC sees recur in
inbound `security@jspwiki.apache.org` reports — categories like
"XSS in plugin X's `link` parameter — already handled by the new
escape; suppress" or "form-based POST endpoint Y missing CSRF —
already known, tracked at JSPWIKI-NNN". *(meta — §11a)*

---

## Appendix: existing security-artefact → § back-map

| Source | Claim | Lands in |
| --- | --- | --- |
| `README.md` | "JSPWiki supports … very detailed access control and security integration using JAAS" | §2, §4 B1, §8 P1/P2 |
| `README.md` line 118 | "Docker images are not official ASF releases but provided for convenience" | §3 item 10 |
| `ReleaseNotes` line 394 | "WebDAV does not yet support the new authentication/permissions scheme" | §3 item 8, §9, §11a |
| `ReleaseNotes` line 410 | "Running with a security manager isn't yet supported (see JSPWIKI-129)" | §3 item 4, §9 (first bullet), §11a |
| `jspwiki.properties` lines 239–259 (`jspwiki.translatorReader.allowHTML`) | "THIS IS A DANGEROUS OPTION!" | §5a, §8 P4, §9, §11a |
| `jspwiki.properties` lines 162–183 (attachment allow/forbid/maxsize) | "These … are not enforced for users with AdminPermissions" | §5a, §8 P6, §9 false-friend, §11a |
| `jspwiki.properties` line 185 (`jspwiki.attachment.forceDownload`) | default `.html .htm .js .pdf .svg .xml` | §5a, §8 P7 |
| `jspwiki.properties` lines 547–566 (cookieAssertions / cookieAuthentication) | "still considered to be unsafe, just like no login at all"; "comes with important security caveats" | §5a, §9 false-friend, §11 |
| `jspwiki.properties` line 543 (`jspwiki.login.throttling`) | default `true` | §8 P3 |
| `jspwiki.properties` line 670 (`jspwiki.securecookie`) | default `true`; "set to false if you're using non-SSL/TLS connections" | §5a, §8 P8 |
| `jspwiki.properties` lines 628–630 | "Passwords are SHA-1 hashed" | §5a, §9 false-friend, §11a, §14 Q17 |
| `jspwiki.properties` lines 945–948 | "If you turn on DEBUG logging, … some security-sensitive information will be logged (such as session IDs)" | §9 (log hygiene), §10 |
| `jspwiki.properties` lines 1152–1187 | audit logging + boot-time security check email | §5a, §8 P9, P10 |
| `jspwiki.policy` lines 50–55 | `Role "All"` grants `view *:*`, login, edit-prefs, edit-profile | §4 B1, §5a |
| `jspwiki.policy` lines 60–62 | "For Internet-facing wikis, you are strongly advised to remove the lines containing the 'modify' and 'createPages' permissions" | §10 item 1, §11 first bullet |
| `jspwiki.policy` lines 68–71 | `Role "Anonymous"` defaults | §5a, §11 |
| `jspwiki.policy` lines 107–112 | `Admin` group / role gets `AllPermission` | §3 item 2, §7, §9 |
| `web.xml` `<security-constraint>` blocks | TLS-CONFIDENTIAL default; container-auth opt-in via "REMOVE ME TO ENABLE…" comments | §2 P3, §5a |
| `MarkupParser.java` line 72–74 | "VERY dangerous option to set — never turn this on in a publicly allowable Wiki" | §5a, §8 P4 |
| `JSPWikiMarkupParser.java` lines 254–255, 300–301, 425–426 | `TextUtil.escapeHTMLEntities` invoked when `!m_allowHTML` | §8 P4 |
| `AttachmentServlet.java` line 209–214 | `PagePermission(att, "view")` check on download | §4 B3, §8 P2 |
| `AttachmentServlet.java` line 589–590 | `PagePermission(att, "upload")` check on upload | §4 B2, §8 P2 |
| `AttachmentServlet.java` lines 143–160 | `isTypeAllowed` forbid-then-allow logic | §8 P6 |
| `AttachmentServlet.java` lines 295–304 | `getContentDisposition` (inline vs attachment per `forceDownload`) | §8 P7 |
| `AuthenticationManager.java` lines 38–48 | login throttling, exponential delay capped at 20 s | §8 P3 |
| `AuthorizationManager.java` lines 50–55 | "ACL + policy intersection" semantics | §8 P5 |
| `AuthorizationManager.java` lines 95–99 | "If security not set to JAAS, will return true" | §3 item 4, §9 |
| `DefaultAclManager.java` line 73 | ACL regex `\[\{\s*ALLOW ...\}\]` | §3 item 5, §4 B5 |
| `Role.java` lines 36–45 | built-in roles `All`, `Anonymous`, `Asserted`, `Authenticated` | §2 |
| `SecurityVerificationUtility.java` | boot-time security check + admin email | §8 P10 |
| `ChangeLog.md` (recurring XSS entries lines 373–375, 750, 937–938, 984, 1525, 1781, 1794, 1875, 1902, 1961, 1970, 1996) | recurring class of XSS findings, all fixed case-by-case | §9 well-known attack classes, §14 Q37 |
| `ChangeLog.md` line 163 (`JSPWIKI-1245`) | "run security validation checks at start up and log it" | §8 P10 |
| `ChangeLog.md` line 213 (`JSPWIKI-1229`) | "cookie security flags. new jspwiki properties added" | §8 P8 |
| `jspwiki-wiki.apache.org/Wiki.jsp?page=Security` | hardening recommendations: enable TLS, remove `Install.jsp` after install, vet third-party plugins, restrict file uploads with size limits + AV scanning, secure file permissions, vet container/JDK versions; **explicit note that CSS can be inserted into wiki pages but JavaScript injection is prevented by default settings**; advises private security mailing list at `security@apache.org` for disclosure | §10 hardening, §11a (CSS-injection-permitted-by-default false-friend) |
| `jspwiki-wiki.apache.org/Wiki.jsp?page=CVE` | 28+ CVEs spanning 2018–2026: CVE-2018-20242; CVE-2019-0224, -0225, -10076, -10077, -10078, -10087, -10089, -10090, -12404, -12407; Log4J-CVE-2021-44228, CVE-2021-40369, -44140; CVE-2022-24947, -24948, -27166, -28730, -28731, -28732, -34158, -46907; CVE-2024-27136; CVE-2025-24853, -24854; CVE-2026-28811, -28812, -28813, -28814, -48910 | §9 well-known attack classes (XSS class confirmed via recurring cluster), §11a (Q37 §11a-population baseline), §13 disposition history |

This back-map now incorporates the wiki-hosted Security and CVE pages.
The PMC should expect to expand it once
those sources are mined.
