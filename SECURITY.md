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

# Security Policy

## Reporting a Vulnerability

Apache JSPWiki follows the [Apache Software Foundation security process](https://www.apache.org/security/).
Please report suspected vulnerabilities **privately** to `security@apache.org` (the JSPWiki PMC is
reachable at `private@jspwiki.apache.org`). Do **not** open public GitHub issues or pull requests for
security reports.

When reporting, include the affected version, a description of the issue, and — if you can — which
security property you believe is violated (see the Threat Model below) and a reproduction.

## Threat Model

What JSPWiki considers in scope and out of scope, the security properties it claims and the ones it
explicitly disclaims, the adversary model, and how inbound reports and tool/AI findings are triaged are
documented in [THREAT_MODEL.md](./THREAT_MODEL.md). Reporters and triagers should consult that document
alongside this policy: a finding that violates a claimed property is handled per the process above; a
finding that falls outside the model is closed citing the relevant section.
