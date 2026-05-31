<!--
SPDX-License-Identifier: Apache-2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Threat Model — Apache JSPWiki

## §1 Header

- **Project:** Apache JSPWiki — a feature-rich, WikiWiki-style engine built on
  standard Java/Jakarta EE components (servlet container), with page content
  authored in JSPWiki markup (or Markdown), server-side plugins and filters, file
  attachments, and JAAS-based authentication plus per-page access control lists.
- **Modelled against:** `apache/jspwiki` `master` (HEAD at time of writing, 2026-05-31).
- **Status:** **DRAFT — v0, not yet reviewed by the JSPWiki PMC.** Produced by the ASF
  Security team via the `threat-model-producer` rubric
  (<https://gist.github.com/potiuk/da14a826283038ddfe38cc9fe6310573>) for the PMC to
  react to — confirm, correct, or strike each claim.
- **Version binding:** This model is versioned alongside the project. A report against
  release *N* is triaged against the model as it stood at *N*, not at HEAD.
- **Reporting cross-reference:** Findings that violate a §8 property should be reported
  privately per `SECURITY.md` / the ASF process (<https://www.apache.org/security/>).
  Findings that fall under §3 or §9 will be closed citing this document.
- **Provenance legend:** *(documented)* = stated in JSPWiki's own docs/README/source;
  *(maintainer)* = confirmed by a JSPWiki PMC member; *(inferred)* = reasoned from code
  structure or wiki-engine domain norms, **not yet confirmed** — every *(inferred)* claim
  has a matching question in §14.
- **Draft confidence:** ~14 documented / 0 maintainer / ~58 inferred. This is a v0 written
  from public artifacts; most claims await PMC ratification.

JSPWiki is deployed as a web application (a WAR) inside a servlet container. Anonymous and
authenticated web users read and edit pages whose content is rendered from wiki markup to
HTML, may upload and download attachments, and may invoke server-side plugins and filters
embedded in page markup. Who may do what to which page is governed by per-page ACLs, wiki
groups, and a JAAS-backed authentication layer; the deploying operator controls the JVM
security policy (`WEB-INF/jspwiki.policy`), which plugin JARs are installed, and the page /
attachment / user-database storage backends.

## §2 Scope and intended use

Primary intended use *(documented)*: a self-hosted collaborative wiki served from a Java
servlet container, with page content collaboratively authored over HTTP, "very detailed
access control and security integration using JAAS" *(documented — README)*, and content
persisted via pluggable page/attachment providers (default: filesystem;
`jspwiki.fileSystemProvider.pageDir`, `jspwiki.basicAttachmentProvider.storageDir`)
*(documented — README)*.

Caller roles (a web app has no single "caller"):

- **Anonymous client** — untrusted; whatever an unauthenticated HTTP request can reach.
- **Asserted identity** — a user who supplied a name via cookie but did **not** authenticate
  *(inferred)*; trusted only as a convenience label, not as an identity.
- **Authenticated user** — logged in via JAAS; trusted up to the permissions their roles/ACLs grant.
- **Wiki admin** — holds the `Admin` role / `AllPermission`-class grants; trusted for the instance.
- **Operator / deployer** — controls the WAR, `jspwiki.properties`, `jspwiki.policy`, installed
  plugin JARs, and storage backends. Fully trusted; **out of model** as an adversary (§3).

**Component-family table:**

| Family | Representative entry point | Touches outside process | In model? |
| --- | --- | --- | --- |
| Wiki engine core (page CRUD, references) | `Edit.jsp` / `WikiEngine`, `jspwiki-main` | filesystem (pages) | **Yes** |
| Markup render → HTML | `jspwiki-main` render, `jspwiki-markdown` | no (CPU) | **Yes** |
| Plugins & filters (server-side, invoked from markup) | `[{Plugin}]`, `jspwiki-plugins`, filter chain | varies per plugin (net/fs) | **Yes** (invocation surface) |
| Attachments (upload/download/store) | `Attach.jsp`, `BasicAttachmentProvider` | filesystem | **Yes** |
| AuthN / AuthZ (JAAS, ACLs, groups, user DB) | `auth/*`, `Acl`, `UserManager` | user DB (XML/JDBC), JAAS | **Yes** |
| HTTP / session / UI (JSPs, forms) | `jspwiki-http`, `jspwiki-war` | network | **Yes** |
| Remote APIs (XML-RPC, RSS/Atom feeds) | `jspwiki-xmlrpc` | network | **Yes** |
| Search + content extraction | `jspwiki-tika-searchprovider`, `jspwiki-kendra-searchprovider` | filesystem; Tika parsers; (Kendra → AWS) | **Yes** (parser surface) |
| WYSIWYG editor (client-side) | `jspwiki-wysiwyg` | browser only | No → §3 |
| Portable launcher / Docker demo | `jspwiki-portable`, `docker-files` | — | No → §3 |
| Tests, IT, adapters, sample wikipages | `jspwiki-it-tests`, `*-adapters`, `jspwiki-wikipages` | — | No → §3 |

## §3 Out of scope (explicit non-goals)

- **The operator / deployer as adversary.** Anyone who can edit `jspwiki.properties`,
  `jspwiki.policy`, deploy plugin JARs, or reach the page/attachment/user-DB storage on disk
  has already won; JSPWiki does not defend the instance against its own administrator *(inferred)*.
- **JVM security-policy hardening.** `WEB-INF/jspwiki.policy` configures JVM-level permissions
  *(documented — README)*; getting it right is the operator's responsibility (§10), not a
  property JSPWiki enforces at runtime.
- **Servlet-container and transport security.** TLS, container auth realms, JVM/OS hardening
  are the deployment's job, not JSPWiki's.
- **Plugin / filter *code* supplied by the operator.** Installing a plugin or filter JAR is a
  deploy-time, operator-trust action (§9 / §11a). The *invocation* of installed plugins by
  untrusted page authors **is** in model.
- **Custom providers / LoginModules** written by the operator (page/attachment/user-DB/auth
  backends other than the shipped defaults).
- **Shipped-but-unsupported code:** `jspwiki-portable` (demo launcher), `docker-files`,
  `jspwiki-it-tests`, `*-adapters`/`*-adaptees`, and the default `jspwiki-wikipages` content,
  which are separately authored and not part of the security contract *(inferred)*.

## §4 Trust boundaries and data flow

The trust boundary is the **HTTP request surface**. Every inbound request carries an identity
(anonymous / asserted / authenticated) that the authorization layer resolves to a set of
permissions before any state-changing or ACL-guarded action *(inferred)*.

Key trust transitions:

1. **Authoring → storage:** a request with edit rights stores page markup verbatim. The markup
   is **untrusted data at rest** — it is attacker-influenced content that later renders into
   HTML served to *other* users. The render step (transition 2) is the security-relevant one.
2. **Storage → render → viewer:** stored markup is transformed to HTML and served. This is the
   stored-XSS boundary: output must be sanitized so that one user's page content cannot run
   script in another user's session *(inferred — see §8/§9)*.
3. **Markup → plugin/filter execution:** `[{PluginName ...}]` and the filter chain execute
   server-side Java when a page renders. The author of the markup influences *which* plugin runs
   and *with what parameters*; the plugin's code is operator-supplied (§3) *(inferred)*.
4. **Upload → attachment store → download:** uploaded bytes + filename cross into filesystem
   storage and are later served back. Path interpretation and content-type handling are the
   risk points.
5. **Remote API → engine:** XML-RPC / feed endpoints invoke engine operations under whatever
   identity the request authenticates as.

**Reachability preconditions (the triager's first test):**

- A finding in the **render/markup** path is in-model only if reachable from stored page content
  or a render-time parameter an untrusted author can set.
- A finding in **auth/ACL** is in-model if it lets an identity exceed the permissions its
  role/ACL grants.
- A finding in **attachments** is in-model if reachable from an uploaded filename or body.
- A finding in a **plugin/filter** is in-model only if an untrusted author can reach it *and* the
  plugin ships/loads by default; a finding that requires an operator to have installed a
  non-default plugin is `OUT-OF-MODEL: unsupported-component` (§3) *(inferred — confirm in §14)*.
- A finding reachable only from `jspwiki.properties` / `jspwiki.policy` / disk is out of model (§3).

## §5 Assumptions about the environment

- **Runtime:** a standard Java servlet container (Jakarta EE) hosting the WAR *(documented — README)*;
  a conformant JVM.
- **Storage:** a filesystem the process can read/write for pages and attachments by default
  *(documented — README)*; optionally a JDBC database and/or XML files for the user database and
  groups *(inferred)*.
- **Identity backend:** JAAS LoginModule(s) — the default user database, or container/LDAP/custom
  modules the operator configures *(documented — JAAS; inferred for specifics)*.
- **Concurrency:** the engine serves concurrent requests; page/attachment providers and the
  reference manager are expected to tolerate concurrent access *(inferred)*.
- **What JSPWiki does to its host (inventory, predominantly *(inferred)* — wave-2 confirmation):**
  reads/writes the configured page and attachment directories; reads `jspwiki.properties` and
  `jspwiki.policy`; may open outbound network connections **only** through plugins/filters/feeds
  that fetch URLs (e.g. RSS, image, InterWiki) or through the Kendra search provider; may invoke
  Apache Tika parsers over attachment content for indexing. It is **not** assumed to spawn child
  processes or install signal handlers *(inferred)*.

## §5a Build-time and configuration variants

JSPWiki's security envelope is set far more by `jspwiki.properties` / `jspwiki.policy` than by
compile flags. The knobs below change which §8 properties hold; **defaults are *(inferred)* and
are wave-1 confirmation targets** because an insecure default reshapes §8/§10/§11a/§13 at once.

| Knob (names *(inferred)*) | Effect on model | Insecure-default? — Maintainer stance |
| --- | --- | --- |
| Default page ACL / `jspwiki.policy` anonymous grants | Whether anonymous users may view/edit/upload | **Open** — is anonymous *edit* on by default the supported posture, or a dev convenience? |
| Self-registration / user creation enabled | Whether anyone may mint an authenticated account | **Open** |
| Login throttling / lockout (`jspwiki.login.throttling`?) | Brute-force resistance of authentication | **Open** |
| Markup engine: JSPWiki vs Markdown (`jspwiki.syntax`?) | Different parser → different XSS/render surface | **Open — which is default?** |
| Raw-HTML / `<script>` allowance in markup | Whether stored markup may emit raw HTML | **Open — load-bearing for §8 XSS** |
| XML-RPC endpoint enabled | Whether the remote API is exposed by default | **Open** |
| Attachment max size / allowed types | Upload DoS + content-type handling | **Open** |
| Search provider (Lucene/Tika vs Kendra) | Whether attachment content is parsed by Tika (parser attack surface) and whether content leaves to AWS | **Open** |

## §6 Assumptions about inputs

Untrusted inputs originate from HTTP requests by anonymous or authenticated users. Trusted inputs
are operator-controlled configuration. Per-entry-point trust table (entry points and parameters
*(inferred)* from module structure — confirm names in §14):

| Entry point | Parameter | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- |
| `Edit`/`Comment` (save page) | wiki markup body | **yes** (any identity with edit ACL; anonymous if permitted) | output sanitization at render; edit ACLs |
| page view / engine | page name, `redirect`, referrer, InterWiki target | **yes** | name canonicalization; redirect allow-listing |
| markup render | `[{Plugin p='…'}]` plugin name + params | **yes** (page author) | only default-safe plugins reachable by untrusted authors |
| `Attach` (upload) | filename, body, declared content-type | **yes** | path canonicalization; size/type limits; serve with safe content-type |
| Login / user profile | username, password, profile fields | **yes** | credential verification; throttling; password hashing |
| XML-RPC / feeds | RPC method + args, feed params | **yes** | same authZ as web; XXE-safe XML parsing |
| Search | query string | **yes** | query parsing bounds |
| `jspwiki.properties` / `jspwiki.policy` | all keys | **no — operator-trusted** | never sourced from request |

Size/shape/rate: inputs are bounded only where the operator configures limits (attachment size,
throttling); the engine is **not** assumed to impose intrinsic bounds on markup size, plugin
recursion (e.g. `InsertPage`), or reference-graph cost *(inferred — §8/§9 resource line)*.

## §7 Adversary model

- **Primary adversary:** an untrusted web user — anonymous, or an authenticated user acting beyond
  their granted permissions. Capabilities: submit page markup, upload attachments, invoke plugins
  via markup, call remote APIs, attempt authentication, and craft page/attachment/redirect names.
- **What they want:** run script in another user's browser (stored XSS); read or modify pages they
  shouldn't (ACL bypass); escalate from anonymous/asserted to authenticated/admin; exfiltrate
  server-side resources (SSRF/file read via plugins or XML); exhaust CPU/memory/disk; smuggle a
  malicious attachment to other users.
- **Explicitly out of model:** the operator/deployer and anyone with filesystem, JAR-deploy,
  `jspwiki.policy`, or container access (§3) — they already control the instance. The asserted-only
  identity is treated as *unauthenticated* for any security decision *(inferred — confirm §14)*.

## §8 Security properties the project provides

*(All §8 entries are *(inferred)* working hypotheses for v0 unless tagged otherwise; each needs a
maintainer ruling in §14. Symptom / severity given per the rubric.)*

1. **Access control on pages and actions.** View/edit/rename/delete/upload and admin operations are
   gated by per-page ACLs, wiki groups, and roles via the JAAS authorization layer *(documented —
   README "very detailed access control … using JAAS"; mechanism in `auth/`, `Acl*`)*.
   *Symptom:* an identity performs an action its ACL/role forbids. *Severity:* critical (CVE-class).
2. **Authentication integrity.** Login verifies credentials against the configured LoginModule;
   asserted (cookie-only) identities are not treated as authenticated *(inferred)*. *Symptom:* auth
   bypass / privilege escalation. *Severity:* critical.
3. **Stored-output sanitization.** Rendered page HTML is sanitized so authored markup cannot inject
   active script into another viewer's session *(inferred — load-bearing; §14 wave-2)*. *Symptom:*
   stored/reflected XSS. *Severity:* critical.
4. **Attachment containment.** Attachment filenames are canonicalized so an upload cannot traverse
   outside the storage dir, and attachments are served without being executed server-side
   *(inferred)*. *Symptom:* path traversal read/write, or server-side execution of uploaded content.
   *Severity:* critical.
5. **Credential-at-rest protection.** Passwords in the default user database are stored hashed, not
   plaintext *(inferred — `PasswordComplexityVerifier` exists; scheme unconfirmed)*. *Symptom:*
   plaintext or trivially reversible credential storage. *Severity:* high.
6. **Resource bounds — UNRESOLVED.** Whether super-linear render cost, unbounded plugin recursion,
   or large uploads are considered bugs is **not yet stated**; treated as "no intrinsic guarantee
   beyond operator-configured limits" pending §14. *Symptom:* hang / OOM / disk-fill. *Severity:*
   medium (DoS) — contested until the maintainer draws the line.

## §9 Security properties the project does NOT provide

- **No defence against a malicious operator/admin** (§3).
- **Plugins and filters are not sandboxed beyond the JVM policy.** Code in an installed plugin/filter
  runs with the server's privileges; JSPWiki does not isolate plugin logic itself *(inferred)*. The
  operator decides which plugins exist; a page author chooses among the *installed* ones.
- **The asserted (cookie) identity is not authentication.** It is a display convenience; relying on
  it as proof of identity is a misuse (§11) *(inferred)*.
- **No intrinsic anti-automation / DoS guarantee** against pathological markup, deeply nested or
  self-referential `InsertPage`/transclusion, oversized uploads, or expensive search queries beyond
  operator-set limits *(inferred)*.

**False-friend properties** (look like a security guarantee, are not):

- *ACLs are an authorization mechanism, not an encryption/confidentiality boundary against the
  operator or the storage layer* — page content on disk is readable by anyone with filesystem access.
- *A page "lock"/version history is integrity-for-collaboration, not tamper-evidence against an
  admin.*
- *`jspwiki.policy` (a Java policy file) limits plugin/JVM permissions; it is not a substitute for
  not installing untrusted plugins.*

**Well-known attack classes for wiki engines that the integrator/operator must keep in view**
(JSPWiki defends some; confirm which in §14):

- **Stored / reflected XSS** via markup and raw-HTML allowances.
- **CSRF** on state-changing actions (edit, delete, profile, group management).
- **SSRF / outbound fetch** via URL-consuming plugins/filters (RSS, Image, InterWiki) and feeds.
- **XXE** via XML-RPC and any XML parsing; **content-parser attacks** (zip-bomb, malformed
  documents) via Tika during attachment indexing.
- **Path traversal** via attachment/page names; **open redirect** via redirect parameters.
- **ReDoS** in markup/regex processing; **transclusion loops** (recursive `InsertPage`).
- **Brute-force / credential stuffing** against login if throttling is off.

## §10 Downstream (operator) responsibilities

- Set the default page ACL / `jspwiki.policy` deliberately — decide whether anonymous view/edit/upload
  is intended for *this* deployment; do not ship the dev default to production unreviewed.
- Restrict the `Admin` role; protect install/setup pages.
- **Deploy only trusted plugin/filter JARs.** Treat third-party plugins as code you are running.
- Serve over HTTPS; put the app behind a hardened container; disable XML-RPC and unused remote
  endpoints if not needed.
- Configure attachment size/type limits and login throttling.
- Choose and secure the user-database / JAAS LoginModule backend; enforce password policy.
- Keep Tika / search and Markdown dependencies patched if those providers are enabled.

## §11 Known misuse patterns

- Enabling anonymous edit/upload on a publicly-reachable instance without intending an open wiki.
- Installing untrusted or unmaintained third-party plugins and treating them as data, not code.
- Treating the cookie-asserted identity as an authenticated identity in custom templates/plugins.
- Exposing XML-RPC or admin pages to the public internet.
- Relying on page ACLs to keep content secret from operators or from on-disk access.

## §11a Known non-findings (recurring false positives)

*(v0 seed — the PMC's real list is the highest-leverage §14 input.)*

- **Plugin/filter executes server-side Java** — by design; plugins are operator-installed code
  (§3/§9). Not a finding unless a *default-shipped* plugin is reachable by an *untrusted* author and
  violates a §8 property.
- **`jspwiki.policy` grants broad permissions** — operator-controlled configuration (§3), not a code
  vuln.
- **Asserted-identity cookie is not authenticated** — by design (§9); reports treating it as auth bypass
  are `BY-DESIGN`.
- **Findings in `jspwiki-portable` / `docker-files` / `jspwiki-it-tests` / sample `jspwiki-wikipages`**
  — out of scope (§3).
- **Reflected request value in an error/redirect page that is HTML-escaped** — not XSS if the §8(3)
  sanitization invariant holds.
- **Java `SecurityManager` deprecation warnings** — platform deprecation, not a JSPWiki vuln.

## §12 Conditions that would change this model

- A new default-shipped plugin/filter, especially one that fetches URLs, reads files, or emits raw HTML.
- A change to the default authentication/anonymous-access posture.
- A new input format or markup engine default (e.g. switching default to Markdown).
- A new or newly-default remote API (REST, GraphQL) or search/parser provider.
- Any inbound report that cannot be routed to a single §13 disposition (→ revise the model, don't
  make an ad-hoc call).

## §13 Triage dispositions

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a claimed property via an in-scope adversary/input. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property broken, but a §11 misuse is easy enough to warrant hardening. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires control of an input marked trusted (config/policy). | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires operator/filesystem/JAR-deploy capability. | §7, §3 |
| `OUT-OF-MODEL: unsupported-component` | Lands in a non-default plugin, portable/docker, tests, or sample pages. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only under a discouraged/non-default `jspwiki.*` knob. | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a §9-disclaimed property (e.g. asserted identity, plugin sandboxing). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a entry. | §11a |
| `MODEL-GAP` | Routes to none of the above → revise the model. | §12 |

## §14 Open questions for the maintainers

Every *(inferred)* claim above routes here. Proposed answers are stated so you can confirm / correct /
strike with a one-liner. Grouped in waves.

**Wave 1 — scope & insecure-default rulings (most load-bearing; §2/§3/§5a/§8 depend on these):**
1. Is anonymous *edit/upload* enabled by default, and is that the **supported production posture**
   (reports → `VALID`) or a dev convenience operators must lock down (→ `OUT-OF-MODEL: non-default-build`)?
   *Proposed:* dev convenience; operators restrict for production.
2. Default markup engine — JSPWiki markup or Markdown? Does the default allow **raw HTML / `<script>`**
   in page content? *Proposed:* JSPWiki markup default; raw HTML disallowed/sanitized by default.
3. Is self-registration (account creation) enabled by default? *Proposed:* off by default.
4. Is the XML-RPC endpoint enabled by default? *Proposed:* off / opt-in.
5. Confirm the role taxonomy and that the **asserted (cookie) identity is never used for a security
   decision**. *Proposed:* Anonymous / Asserted / Authenticated / Admin; asserted ≠ authenticated.

**Wave 2 — render, plugins, attachments (§4/§8/§9):**
6. What sanitizes rendered output, and is **stored XSS a claimed §8 property** for default config?
   Which sanitizer/library (e.g. an HTML filter, AntiSamy-style)? *Proposed:* yes, claimed; engine
   sanitizes before serving.
7. Are **plugins reachable by untrusted authors** restricted to a safe default set, or can any author
   invoke any installed plugin with arbitrary params? Which default plugins fetch URLs / read files?
   *Proposed:* untrusted authors limited to safe defaults; URL-fetching plugins are gated.
8. Attachment handling: how are filenames canonicalized against **path traversal**, and are uploads
   served with a non-executable, sniff-safe content-type? *Proposed:* names canonicalized; served as
   attachments.
9. Is attachment content parsed by **Tika** for indexing by default, and is that parser surface
   considered in-model? *Proposed:* only when Tika search provider is enabled; in-model when enabled.

**Wave 3 — properties, DoS, CSRF, §11a (§8/§9/§11a):**
10. Where is the **resource line** (§8.6)? Is a hang / super-linear render on pathological markup or a
    transclusion loop a bug, or is no guarantee made beyond operator limits? *Proposed:* no intrinsic
    guarantee; operator sets limits.
11. Is **CSRF protection** present on state-changing actions, and is it a claimed property?
    *Proposed:* present (token/`SpamFilter`); claimed.
12. How are passwords hashed in the default user database, and is login **throttled** by default?
    *Proposed:* salted hash; throttling configurable, default on.
13. What do scanners / researchers most often report that the PMC considers a **non-finding**, and why?
    (Directly seeds §11a.)

**Meta:**
14. Where should this document live and bind — root `THREAT_MODEL.md` referenced from a new `SECURITY.md`
    (this PR's proposal), or merged into JSPWiki's existing wiki Security page? Who owns revisions, and
    which release branches should carry it?

## §15 Machine-readable companion

Deferred for v0. Once the prose stabilizes, a `threat-model.yaml` sidecar can encode entry-point trust
levels (§6), component in/out-of-scope (§2/§3), the §8 property/severity/symptom rows, §9 false friends,
§11a non-findings, and the §13 dispositions for automated/AI-assisted triage.
