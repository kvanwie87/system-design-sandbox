# OWASP Top 10 — Quick Overview (2021 vs 2025)

The [OWASP Top 10](https://owasp.org/www-project-top-ten/) is a consensus
awareness document listing the most critical security risks to web
applications. It's refreshed every few years based on new data and trends.

Sources:
- [OWASP Top 10:2021](https://top10.owasp.org/2021/)
- [OWASP Top 10:2025](https://top10.owasp.org/2025/)

## Side-by-side

| #   | 2021                                          | 2025                                          |
| --- | --------------------------------------------- | --------------------------------------------- |
| A01 | Broken Access Control                         | Broken Access Control                         |
| A02 | Cryptographic Failures                        | Security Misconfiguration                     |
| A03 | Injection                                     | Software Supply Chain Failures                |
| A04 | Insecure Design                               | Cryptographic Failures                        |
| A05 | Security Misconfiguration                     | Injection                                     |
| A06 | Vulnerable and Outdated Components            | Insecure Design                               |
| A07 | Identification and Authentication Failures    | Authentication Failures                       |
| A08 | Software and Data Integrity Failures          | Software or Data Integrity Failures           |
| A09 | Security Logging and Monitoring Failures      | Security Logging and Alerting Failures        |
| A10 | Server-Side Request Forgery (SSRF)            | Mishandling of Exceptional Conditions         |

## What changed in 2025

- **Broken Access Control** stays at #1 — still the most prevalent risk.
- **Security Misconfiguration** jumped from A05 to A02, reflecting how common
  misconfigured defaults, permissions, and cloud settings have become.
- **Software Supply Chain Failures** is new at A03. It's a broader category that
  expands on 2021's "Vulnerable and Outdated Components," covering the whole
  supply chain (dependencies, build pipelines, distribution), not just
  outdated libraries.
- **Cryptographic Failures** and **Injection** both dropped a few places (to A04
  and A05). Injection falling is often read as a sign that frameworks and tools
  increasingly handle it well by default.
- **Mishandling of Exceptional Conditions** is new at A10. It covers improper
  handling of errors and edge cases (e.g. logic that behaves insecurely when
  something unexpected happens). SSRF, the 2021 A10, was folded into other
  categories.
- Several categories were renamed/trimmed: "Identification and Authentication
  Failures" → **Authentication Failures**; "Logging and Monitoring" →
  "Logging and **Alerting**".

## 2025 list (with brief notes)

1. **A01 Broken Access Control** — users acting outside their intended
   permissions (e.g. accessing another user's data, privilege escalation).
2. **A02 Security Misconfiguration** — insecure defaults, overly permissive
   settings, unnecessary features enabled, verbose errors leaking details.
3. **A03 Software Supply Chain Failures** — risks from dependencies, build
   systems, and distribution channels, not just outdated components.
4. **A04 Cryptographic Failures** — weak, misused, or missing cryptography that
   exposes sensitive data (in transit or at rest).
5. **A05 Injection** — untrusted input interpreted as commands/queries (SQL,
   NoSQL, OS command, LDAP; XSS falls here too).
6. **A06 Insecure Design** — flaws rooted in missing or poor security design,
   not just implementation bugs. Fixed by threat modeling and secure patterns.
7. **A07 Authentication Failures** — weaknesses in verifying identity: weak
   credentials, broken session management, missing MFA.
8. **A08 Software or Data Integrity Failures** — code/data changes not verified
   for integrity (insecure deserialization, unsigned updates, untrusted CI/CD).
9. **A09 Security Logging and Alerting Failures** — insufficient logging,
   detection, and alerting, delaying breach discovery and response.
10. **A10 Mishandling of Exceptional Conditions** — insecure or incorrect
    behavior when handling errors and unexpected states.

## Relevance to Spring Security

Several of these map directly to what you'll practice in this project:

- **A01 Broken Access Control** → authorization rules, method security,
  `SecurityFilterChain` URL rules.
- **A02 Security Misconfiguration** → sensible security defaults, not disabling
  protections (like CSRF) without reason.
- **A04 Cryptographic Failures** → password hashing with `PasswordEncoder`
  (e.g. BCrypt), HTTPS.
- **A05 Injection** → parameterized queries, input validation.
- **A07 Authentication Failures** → login mechanisms, session management, MFA.

> Note: Short descriptions above are paraphrased summaries of the OWASP
> categories for quick reference. See the linked official pages for the
> authoritative definitions and guidance. Content was rephrased for compliance
> with licensing restrictions.
