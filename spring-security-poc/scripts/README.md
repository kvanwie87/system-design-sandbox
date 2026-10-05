# Scripts

Helper scripts for running the app under each security profile and validating
the resulting auth behavior.

All scripts are bash. On Windows, run them from Git Bash (the environment this
project is set up for).

## Prerequisites

- A JDK 21. The scripts auto-detect a valid `JAVA_HOME` (the project's default
  sdkman `current` symlink is broken, so they fall back to a known-good JDK).
  You can override by exporting `JAVA_HOME` yourself.
- `curl` on the PATH (used by the validation scripts).

## Run scripts

Start the application with a specific profile. These block (the server runs in
the foreground) — use a separate terminal to run the validation scripts.

| Script | Profile | Behavior |
| --- | --- | --- |
| `run-formlogin.sh` | `formlogin` | All endpoints require login via the default form. |
| `run-permitall.sh` | `permitall` | All endpoints open, no authentication. |
| `run-basic.sh` | `basic` | All endpoints require HTTP Basic auth (good for `curl`). |
| `run-basicwithcsrf.sh` | `basicwithcsrf` | HTTP Basic auth with CSRF left on; POSTs need a token. |
| `run-stateless.sh` | `stateless` | HTTP Basic with no session (`STATELESS`); no `JSESSIONID`. |

```bash
./scripts/run-formlogin.sh
./scripts/run-permitall.sh
./scripts/run-basic.sh
./scripts/run-basicwithcsrf.sh
./scripts/run-stateless.sh
```

## Validation scripts

Run these in a second terminal while the app is running under the matching
profile. Each exits `0` on success, non-zero on failure.

| Script | Validates | Expectation |
| --- | --- | --- |
| `validate-permitall.sh` | `permitall` | `GET /hello` returns `200 Hello, World!` with no credentials. |
| `validate-formlogin.sh` | `formlogin` | Unauthenticated `GET /hello` is redirected (302) to `/login`; a full CSRF-aware form login then reaches `/hello`. |
| `validate-basic.sh` | `basic` | `GET /hello` returns `401` with no/wrong credentials and `200 Hello, World!` with valid HTTP Basic credentials. |
| `validate-basicwithcsrf.sh` | `basicwithcsrf` | `GET /hello` with creds returns `200`; `POST /echo` with creds but no CSRF token returns `401` (CSRF denial surfaces via the Basic auth entry point); `POST /echo` with a token (fetched from `/csrf`) returns `200`. |
| `validate-stateless.sh` | `stateless` | `GET /hello` returns `401` with no creds and `200 Hello, World!` with valid creds; a successful response sets **no `JSESSIONID`** cookie. |

```bash
# terminal 1
./scripts/run-permitall.sh
# terminal 2
./scripts/validate-permitall.sh
```

```bash
# terminal 1
./scripts/run-formlogin.sh
# terminal 2
./scripts/validate-formlogin.sh
```

```bash
# terminal 1
./scripts/run-basic.sh
# terminal 2
./scripts/validate-basic.sh
```

```bash
# terminal 1
./scripts/run-basicwithcsrf.sh
# terminal 2
./scripts/validate-basicwithcsrf.sh
```

```bash
# terminal 1
./scripts/run-stateless.sh
# terminal 2
./scripts/validate-stateless.sh
```

## Configuration

Override defaults via environment variables (read by `lib-common.sh`):

| Variable | Default | Purpose |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | Where the app is served. |
| `APP_USER` | `user` | Username for login (matches `application.yaml`). |
| `APP_PASSWORD` | `password` | Password for login (matches `application.yaml`). |

```bash
APP_PASSWORD=secret ./scripts/validate-formlogin.sh
```

## Notes

- `validate-formlogin.sh` checks the redirect-to-login behavior (the reliable,
  profile-defining signal) and then attempts a full form-login flow: it scrapes
  the CSRF token from the login page, posts credentials, and reuses the session
  cookie. Form login is enabled on this profile; HTTP Basic is **not**, so
  `curl -u` alone will not authenticate. For simple `curl -u` auth, use the
  `basic` profile instead.
- `lib-common.sh` is sourced by the others and is not meant to be run directly.
