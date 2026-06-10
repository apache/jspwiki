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
- **Modelled against:** `apache/jspwiki` default branch `master` (HEAD at time of writing,
  2026-05-31). Where a property differs between the `master` and `master-2.x` release lines,
  the difference is called out inline; this document is intended to bind on both branches (§14.14).
- **Status:** **DRAFT — v0, maintainer-reviewed.** Produced by the ASF Security team via the
  `threat-model-producer` rubric
  (<https://gist.github.com/potiuk/da14a826283038ddfe38cc9fe6310573>) and revised to fold in the
  JSPWiki PMC's §14 answers (PR review by `@juanpablo-santos`). Remaining *(inferred)* claims await
  PMC ratification.
- **Version binding:** This model is versioned alongside the project. A report against
  release *N* is triaged against the model as it stood at *N*, not at HEAD.
- **Reporting cross-reference:** Findings that violate a §8 property should be reported
  privately per `SECURITY.md` / the ASF process (<https://www.apache.org/security/>).
  Findings that fall under §3 or §9 will be closed citing this document.
- **Provenance legend:** *(documented)* = stated in JSPWiki's own docs/README/source;
  *(maintainer)* = confirmed by a JSPWiki PMC member; *(inferred)* = reasoned from code
  structure or wiki-engine domain norms, **not yet confirmed** — every *(inferred)* claim
  has a matching question in §14.
- **Draft confidence:** ~14 documented / ~16 maintainer / ~31 inferred. Wave-1 and most of
  wave-2/3 §14 questions have been answered by the PMC; the residual *(inferred)* claims are
  lower-leverage specifics still awaiting confirmation.

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

Caller roles (a web app has no single "caller") *(maintainer — role taxonomy confirmed)*:

- **Anonymous client** — untrusted; whatever an unauthenticated HTTP request can reach.
- **Asserted identity** — a user who supplied a name via cookie but did **not** authenticate;
  **asserted ≠ authenticated** *(maintainer)* — trusted only as a convenience label, never as an
  identity for a security decision.
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
| Remote APIs (XML-RPC, RSS/Atom feeds) | `jspwiki-xmlrpc` | network | **Yes** on `master-2.x`; XML-RPC **removed** on `master` (§5a) *(maintainer)* |
| Search + content extraction | `jspwiki-tika-searchprovider`, `jspwiki-kendra-searchprovider` | filesystem; Tika parsers; (Kendra → AWS) | **Yes** (parser surface, when enabled) |
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
   stored-XSS boundary: output is sanitized before serving so that one user's page content cannot
   run script in another user's session *(maintainer — engine sanitizes output before serving; §8)*.
3. **Markup → plugin/filter execution:** `[{PluginName ...}]` and the filter chain execute
   server-side Java when a page renders. An author with edit permission can invoke **any installed
   plugin with arbitrary parameters**; which plugins are *installed* is the operator's choice, and
   the default plugin set is considered safe *(maintainer; §8/§9/§11a)*. The plugin's code is
   operator-supplied (§3).
4. **Upload → attachment store → download:** uploaded bytes + filename cross into filesystem
   storage and are later served back. Path interpretation and content-type handling are the
   risk points.
5. **Remote API → engine:** XML-RPC / feed endpoints invoke engine operations under whatever
   identity the request authenticates as. The XML-RPC endpoint exists on `master-2.x` but has been
   removed on `master` (§5a) *(maintainer)*.

**Reachability preconditions (the triager's first test):**

- A finding in the **render/markup** path is in-model only if reachable from stored page content
  or a render-time parameter an untrusted author can set.
- A finding in **auth/ACL** is in-model if it lets an identity exceed the permissions its
  role/ACL grants.
- A finding in **attachments** is in-model if reachable from an uploaded filename or body.
- A finding in a **plugin/filter** is in-model if an author with edit permission can reach it
  *and* it belongs to the default plugin set; the default set is considered safe, so a finding in a
  default plugin that breaks a §8 property is in-model, while a finding that requires an operator to
  have installed a non-default plugin is `OUT-OF-MODEL: unsupported-component` (§3) *(maintainer)*.
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
  Apache Tika parsers over attachment content for indexing, **only when the Tika search provider is
  enabled** *(maintainer)*. It is **not** assumed to spawn child processes or install signal
  handlers *(inferred)*.

## §5a Build-time and configuration variants

JSPWiki's security envelope is set far more by `jspwiki.properties` / `jspwiki.policy` than by
compile flags. The knobs below change which §8 properties hold. Maintainer-confirmed defaults are
tagged *(maintainer)*; the remainder are still *(inferred)* confirmation targets.

| Knob | Effect on model | Default / maintainer stance |
| --- | --- | --- |
| Default page ACL / `jspwiki.policy` anonymous grants | Whether anonymous users may view/edit/upload | Permissive defaults are a **dev convenience**; operators restrict for production *(maintainer)* |
| Self-registration / user creation enabled | Whether anyone may mint an account | **Enabled by default, with an approval workflow** for account creation; operators restrict for production *(maintainer)* |
| Login throttling / lockout | Brute-force resistance of authentication | Throttling configurable, **default on** *(maintainer)* |
| Markup engine: JSPWiki vs Markdown | Different parser → different XSS/render surface | **JSPWiki markup is the default** *(maintainer)* |
| Raw-HTML / `<script>` allowance in markup | Whether stored markup may emit raw HTML | **Raw HTML disallowed / sanitized by default** *(maintainer)*; an operator can enable an "execute arbitrary markup" flag (§8.6, §11) |
| XML-RPC endpoint enabled | Whether the remote API is exposed | **Removed on `master`** (no jakarta-namespace replacement); **enabled on `master-2.x`** *(maintainer)* |
| Attachment max size / allowed types | Upload DoS + content-type handling | Upload size is **operator-customizable** *(maintainer)* |
| Search provider (Lucene/Tika vs Kendra) | Whether attachment content is parsed by Tika (parser attack surface) and whether content leaves to AWS | Tika parsing applies **only when the Tika search provider is enabled** *(maintainer)* |

## §6 Assumptions about inputs

Untrusted inputs originate from HTTP requests by anonymous or authenticated users. Trusted inputs
are operator-controlled configuration. Per-entry-point trust table (entry-point/parameter names
*(inferred)* from module structure where not separately confirmed):

| Entry point | Parameter | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- |
| `Edit`/`Comment` (save page) | wiki markup body | **yes** (any identity with edit ACL; anonymous if permitted) | output sanitization at render; edit ACLs |
| page view / engine | page name, `redirect`, referrer, InterWiki target | **yes** | name canonicalization; redirect allow-listing |
| markup render | `[{Plugin p='…'}]` plugin name + params | **yes** (any author with edit permission may invoke any installed plugin with arbitrary params) *(maintainer)* | keep the installed plugin set to the default-safe set; vet any added plugin |
| `Attach` (upload) | filename, body, declared content-type | **yes** | path canonicalization; size limit (operator-set); serve as attachment with safe content-type |
| Login / user profile | username, password, profile fields | **yes** | credential verification; throttling (default on); salted-hash password storage |
| XML-RPC / feeds | RPC method + args, feed params | **yes** (XML-RPC on `master-2.x` only) | same authZ as web; XXE-safe XML parsing |
| Search | query string | **yes** | query parsing bounds |
| `jspwiki.properties` / `jspwiki.policy` | all keys | **no — operator-trusted** | never sourced from request |

Size/shape/rate: attachment size is bounded by an operator-customizable limit *(maintainer)*. The
engine is otherwise **not** assumed to impose intrinsic bounds on markup size or plugin recursion;
however, super-linear render on pathological markup and unbounded plugin recursion are considered
**bugs** rather than accepted limitations (§8.6) *(maintainer)*.

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
  identity is treated as *unauthenticated* for any security decision *(maintainer — asserted ≠
  authenticated)*.

## §8 Security properties the project provides

*(Properties tagged *(maintainer)* are PMC-confirmed; *(inferred)* entries remain v0 working
hypotheses pending §14. Symptom / severity given per the rubric.)*

1. **Access control on pages and actions.** View/edit/rename/delete/upload and admin operations are
   gated by per-page ACLs, wiki groups, and roles via the JAAS authorization layer *(documented —
   README "very detailed access control … using JAAS"; mechanism in `auth/`, `Acl*`)*.
   *Symptom:* an identity performs an action its ACL/role forbids. *Severity:* critical (CVE-class).
2. **Authentication integrity.** Login verifies credentials against the configured LoginModule;
   asserted (cookie-only) identities are **not** treated as authenticated *(maintainer)*. *Symptom:*
   auth bypass / privilege escalation. *Severity:* critical.
3. **Stored-output sanitization.** Rendered page HTML is sanitized so authored markup cannot inject
   active script into another viewer's session; the engine sanitizes output before serving, and raw
   HTML is disallowed/sanitized by default *(maintainer)*. *Symptom:* stored/reflected XSS.
   *Severity:* critical.
4. **Attachment containment.** Attachment filenames are canonicalized and attachments are served as
   attachments (not executed server-side) *(maintainer — names canonicalized; served as
   attachments)*. *Symptom:* path traversal read/write, or server-side execution of uploaded
   content. *Severity:* critical.
5. **Credential-at-rest protection.** Passwords in the default user database are stored as a
   **salted hash** *(maintainer)*; login throttling is configurable and **on by default**
   *(maintainer)*. *Symptom:* plaintext or trivially reversible credential storage; un-throttled
   brute force. *Severity:* high.
6. **Resource bounds.** Super-linear render cost on pathological markup and unbounded plugin
   recursion are treated as **bugs**, not accepted limitations *(maintainer)* — a report
   demonstrating either is in-model (a bug to fix), not a `BY-DESIGN` non-finding. Attachment upload
   size is bounded by an **operator-customizable** limit *(maintainer)*, so an upload-size DoS at
   the default limit is an operator-tuning matter rather than an engine defect. *Symptom:* hang /
   OOM (render/recursion) → bug; disk-fill via uploads → operator limit. *Severity:* medium (DoS).
7. **Anti-automation / spam controls.** Token / `SpamFilter`-based protection is present on
   state-changing actions and is a claimed property *(maintainer)*. *Symptom:* CSRF /
   automated abuse of edit/profile/group actions. *Severity:* high.

## §9 Security properties the project does NOT provide

- **No defence against a malicious operator/admin** (§3).
- **Plugins and filters are not sandboxed beyond the JVM policy.** Code in an installed plugin/filter
  runs with the server's privileges; JSPWiki does not isolate plugin logic itself *(inferred)*. The
  operator decides which plugins are installed; the default set is considered safe, and an author
  with edit permission may invoke any *installed* plugin with arbitrary parameters *(maintainer)*.
- **The asserted (cookie) identity is not authentication.** It is a display convenience; relying on
  it as proof of identity is a misuse (§11) *(maintainer)*.
- **Execution of arbitrary markup is possible only when the operator enables the flag for it**
  *(maintainer)*. With that flag off (the default), arbitrary-markup execution is not exposed; with
  it on, the operator has accepted that behaviour. Such reports have been rare in recent memory
  *(maintainer)*.
- **No blanket anti-DoS guarantee** beyond the points in §8.6: the operator sets the attachment-size
  limit, while super-linear render and unbounded plugin recursion are bugs to be fixed rather than
  guaranteed-bounded behaviours.

**False-friend properties** (look like a security guarantee, are not):

- *ACLs are an authorization mechanism, not an encryption/confidentiality boundary against the
  operator or the storage layer* — page content on disk is readable by anyone with filesystem access.
- *A page "lock"/version history is integrity-for-collaboration, not tamper-evidence against an
  admin.*
- *`jspwiki.policy` (a Java policy file) limits plugin/JVM permissions; it is not a substitute for
  not installing untrusted plugins.*

**Well-known attack classes for wiki engines that the integrator/operator must keep in view:**

- **Stored / reflected XSS** via markup and raw-HTML allowances (sanitized by default — §8.3).
- **CSRF** on state-changing actions (edit, delete, profile, group management) — token/`SpamFilter`
  present (§8.7).
- **SSRF / outbound fetch** via URL-consuming plugins/filters (RSS, Image, InterWiki) and feeds.
- **XXE** via XML-RPC (on `master-2.x`) and any XML parsing; **content-parser attacks** (zip-bomb,
  malformed documents) via Tika during attachment indexing (only when Tika is enabled — §5/§5a).
- **Path traversal** via attachment/page names (canonicalized — §8.4); **open redirect** via
  redirect parameters.
- **ReDoS / super-linear render** in markup processing and **transclusion / plugin recursion loops**
  — treated as bugs (§8.6).
- **Brute-force / credential stuffing** against login (throttling default on — §8.5).

## §10 Downstream (operator) responsibilities

- Set the default page ACL / `jspwiki.policy` deliberately — the permissive defaults are a dev
  convenience; decide whether anonymous view/edit/upload is intended for *this* deployment and lock
  it down for production *(maintainer)*.
- Account creation is enabled by default with an approval workflow; restrict self-registration for
  production as appropriate *(maintainer)*.
- Restrict the `Admin` role; protect install/setup pages.
- **Deploy only trusted plugin/filter JARs**, and keep the installed plugin set to the default-safe
  set unless a third-party plugin has been vetted — any author with edit permission can invoke any
  installed plugin with arbitrary parameters *(maintainer)*.
- Serve over HTTPS; put the app behind a hardened container. On `master-2.x`, disable XML-RPC and
  unused remote endpoints if not needed (on `master`, XML-RPC is already removed) *(maintainer)*.
- Configure the attachment size limit; leave login throttling on.
- Leave the "execute arbitrary markup" flag **off** unless the deployment genuinely requires it
  *(maintainer)*.
- Choose and secure the user-database / JAAS LoginModule backend; enforce password policy.
- Keep Tika / search and Markdown dependencies patched if those providers are enabled.

## §11 Known misuse patterns

- Enabling anonymous edit/upload on a publicly-reachable instance without intending an open wiki
  (the permissive defaults are a dev convenience, not the production posture) *(maintainer)*.
- Enabling the "execute arbitrary markup" operator flag on an instance reachable by untrusted
  authors *(maintainer)*.
- Installing untrusted or unmaintained third-party plugins and treating them as data, not code —
  any installed plugin is reachable with arbitrary params by any author with edit permission
  *(maintainer)*.
- Treating the cookie-asserted identity as an authenticated identity in custom templates/plugins
  *(maintainer — asserted ≠ authenticated)*.
- Exposing XML-RPC (on `master-2.x`) or admin pages to the public internet.
- Relying on page ACLs to keep content secret from operators or from on-disk access.

## §11a Known non-findings (recurring false positives)

- **Plugin/filter executes server-side Java** — by design; plugins are operator-installed code
  (§3/§9), and any author with edit permission may invoke any *installed* plugin with arbitrary
  params *(maintainer)*. Not a finding unless a *default-set* plugin reachable by an author with
  edit permission violates a §8 property (the default set is considered safe).
- **`jspwiki.policy` grants broad permissions** — operator-controlled configuration (§3), not a code
  vuln.
- **Asserted-identity cookie is not authenticated** — by design (§9); reports treating it as auth
  bypass are `BY-DESIGN` *(maintainer)*.
- **Arbitrary-markup execution when the operator's flag is on** — operator-enabled behaviour (§9);
  reports against an instance that turned the flag on are `OUT-OF-MODEL: non-default-build` unless
  they show the flag being bypassed while off *(maintainer)*.
- **Findings in `jspwiki-portable` / `docker-files` / `jspwiki-it-tests` / sample `jspwiki-wikipages`**
  — out of scope (§3).
- **Reflected request value in an error/redirect page that is HTML-escaped** — not XSS if the §8(3)
  sanitization invariant holds.
- **Java `SecurityManager` deprecation warnings** — platform deprecation, not a JSPWiki vuln.

## §12 Conditions that would change this model

- A new default-*set* plugin/filter, especially one that fetches URLs, reads files, or emits raw HTML.
- A change to the default authentication / anonymous-access / account-approval posture.
- A new input format or markup engine default (e.g. switching default to Markdown).
- Re-introduction of XML-RPC on `master`, or a new/newly-default remote API (REST, GraphQL) or
  search/parser provider.
- A change to the default state of the "execute arbitrary markup" flag.
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
| `OUT-OF-MODEL: non-default-build` | Only under a discouraged/non-default `jspwiki.*` knob (e.g. the arbitrary-markup flag on). | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a §9-disclaimed property (e.g. asserted identity, plugin sandboxing). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a entry. | §11a |
| `MODEL-GAP` | Routes to none of the above → revise the model. | §12 |

## §14 Open questions for the maintainers

Wave-1 and most of wave-2/3 questions were answered by the PMC in the PR review and folded into the
sections above (resolved answers retained here for traceability, tagged *(maintainer)*). Items still
genuinely open keep their *(inferred)* status and a matching claim above.

**Wave 1 — scope & insecure-default rulings — RESOLVED:**
1. Anonymous edit/upload: *(maintainer)* dev convenience; operators restrict for production.
2. Default markup engine / raw HTML: *(maintainer)* JSPWiki markup is the default; raw HTML
   disallowed/sanitized by default.
3. Self-registration: *(maintainer)* enabled by default **with an account-creation approval
   workflow**; operators restrict for production.
4. XML-RPC endpoint: *(maintainer)* **removed on `master`** (no jakarta-namespace replacement);
   **enabled on `master-2.x`**.
5. Role taxonomy / asserted identity: *(maintainer)* Anonymous / Asserted / Authenticated / Admin;
   asserted ≠ authenticated; asserted is never used for a security decision.

**Wave 2 — render, plugins, attachments — RESOLVED:**
6. Output sanitization / stored XSS: *(maintainer)* yes, claimed; the engine sanitizes output before
   serving.
7. Plugin reachability: *(maintainer)* any author with edit permission can invoke **any installed
   plugin with arbitrary params**; reachable plugins are those the operator has installed; the
   default plugin set is considered safe. *(Still open — lower leverage:* which specific default
   plugins fetch URLs / read files, for a per-plugin SSRF/file-read note? *(inferred)* — see the
   SSRF bullet in §9.)*
8. Attachment handling: *(maintainer)* filenames canonicalized; served as attachments.
9. Tika parsing: *(maintainer)* only when the Tika search provider is enabled; in-model when enabled.

**Wave 3 — properties, DoS, CSRF, §11a — RESOLVED:**
10. Resource line (§8.6): *(maintainer)* super-linear render on pathological markup and unbounded
    plugin recursion are **bugs**; attachment upload size is operator-customizable. Executing
    arbitrary markup is exposed only when the operator enables the flag for it (rare in recent
    reports).
11. CSRF / anti-automation: *(maintainer)* present (token / `SpamFilter`); claimed.
12. Password storage / login throttling: *(maintainer)* salted hash; throttling configurable, default
    on.
13. Recurring non-findings the PMC wants pre-classified (§11a): *(inferred — still open)* the §11a
    list above is a v0 seed plus the maintainer-confirmed entries (asserted identity, plugin
    invocation, arbitrary-markup flag); a fuller PMC list of recurring scanner false positives would
    sharpen §11a further.

**Meta — RESOLVED:**
14. Document location & binding: *(maintainer)* `THREAT_MODEL.md` lives at the repository **root**,
    next to `SECURITY.md`, and should appear on both the **`master`** and **`master-2.x`** branches;
    the **PMC owns revisions**.

## §15 Machine-readable companion

Deferred for v0. Once the prose stabilizes, a `threat-model.yaml` sidecar can encode entry-point trust
levels (§6), component in/out-of-scope (§2/§3), the §8 property/severity/symptom rows, §9 false friends,
§11a non-findings, and the §13 dispositions for automated/AI-assisted triage.
