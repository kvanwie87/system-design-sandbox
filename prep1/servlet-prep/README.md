# servlet-prep — Day 1

A minimal Maven WAR project for Day 1 of the servlet prep plan. Targets **Tomcat 10+** with the **`jakarta.servlet`** API.

## What's here

```
servlet-prep/
├── pom.xml                         # Maven build, packages a WAR
├── src/main/java/com/prep/servlets/
│   ├── HelloServlet.java           # Phase 0/1: GET vs POST, lifecycle logging
│   └── JsonEchoServlet.java        # Build #1: accept JSON, return JSON, status codes
└── src/main/webapp/
    ├── index.html                  # landing page with try-it instructions
    └── WEB-INF/web.xml              # near-empty descriptor (annotations do the mapping)
```

## Prerequisites

- JDK 17+  (`java -version`)
- Apache Tomcat **10.x**  (10+ uses `jakarta.servlet`; 9 and earlier use `javax.servlet`)
- Maven is **not** required — this project ships the Maven Wrapper (`mvnw`), which downloads a pinned Maven (3.9.11) on first use.

## Build

Using the wrapper (recommended, no global Maven install needed):

```bash
# macOS/Linux/Git Bash
./mvnw clean package

# Windows CMD / PowerShell
mvnw.cmd clean package
```

Produces `target/servlet-prep.war`.

> **Note:** the wrapper needs `JAVA_HOME` set. If you see "JAVA_HOME is not defined correctly", point it at your JDK first:
> ```bash
> export JAVA_HOME="/c/Program Files/Amazon Corretto/jdk21.0.4_7"   # Git Bash example
> ```

If you have a global Maven install, `mvn clean package` works too.

## Deploy to Tomcat

Copy the WAR into Tomcat's `webapps/` directory, then start Tomcat:

```bash
cp target/servlet-prep.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh      # Windows: %CATALINA_HOME%\bin\startup.bat
```

Tomcat auto-deploys it under the context path `/servlet-prep`.

## Try it

Landing page:
```
http://localhost:8080/servlet-prep/
```

HelloServlet:
```bash
curl -i "http://localhost:8080/servlet-prep/hello"
curl -i "http://localhost:8080/servlet-prep/hello?name=Priya"
curl -i -X POST "http://localhost:8080/servlet-prep/hello"
```

JsonEchoServlet (Build #1):
```bash
# 200 OK
curl -i -X POST http://localhost:8080/servlet-prep/echo \
     -H "Content-Type: application/json" \
     -d '{"message":"hello"}'

# 415 Unsupported Media Type (wrong content type)
curl -i -X POST http://localhost:8080/servlet-prep/echo -d 'plain text'

# 400 Bad Request (empty body)
curl -i -X POST http://localhost:8080/servlet-prep/echo \
     -H "Content-Type: application/json"

# 405 Method Not Allowed (GET not supported)
curl -i "http://localhost:8080/servlet-prep/echo"
```

## Automated endpoint test

With the app deployed and Tomcat running, run the smoke test:

```bash
./test-endpoints.sh
# or against a different host/context:
BASE_URL=http://localhost:8080/servlet-prep ./test-endpoints.sh
```

It asserts the HTTP status of all 8 cases (landing page, `/hello` variants, and `/echo` 200/415/400/405) and exits non-zero if any fail.

## Local Tomcat used for verification

A Tomcat 10.1.60 install lives at `../tools/apache-tomcat-10.1.60`. Start it (Git Bash) with:

```bash
cd ../tools/apache-tomcat-10.1.60
JAVA_HOME="/c/Program Files/Amazon Corretto/jdk21.0.4_7" ./bin/catalina.bat run   # foreground
# or ./bin/startup.bat to run detached; ./bin/shutdown.bat to stop
```

The app is served at http://localhost:8080/servlet-prep/.

> **Note:** `../tools/` is **git-ignored** — the Tomcat install and its zip are not committed (they're large, reproducible binaries). If the folder is missing (fresh clone), re-download and extract it:
>
> ```bash
> # from the prep1/ directory
> mkdir -p tools
> curl -sL -o tools/apache-tomcat-10.1.60.zip \
>   https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.60/bin/apache-tomcat-10.1.60.zip
> unzip -q tools/apache-tomcat-10.1.60.zip -d tools/
> ```
>
> Any Tomcat 10.1.x works — adjust the version in the URL if you want a newer patch release.

## What to notice (ties back to the plan)

- **`@WebServlet`** does URL mapping — no `web.xml` entries needed.
- **`init()` / `destroy()`** log to Tomcat's console — watch them fire once each.
- **`getWriter()`** is used for text; `getOutputStream()` comes in Day 2 (binary streaming).
- The servlet API dependency is **`provided`** scope — Tomcat supplies it, so it's not bundled in the WAR.
