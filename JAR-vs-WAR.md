# JAR vs WAR

## Summary

- **JAR (Java ARchive)** — A `.jar` file bundling compiled `.class` files, resources, and a manifest. The general-purpose Java packaging unit: libraries, command-line tools, and (with an embedded server) standalone applications.
- **WAR (Web Application ARchive)** — A `.war` file packaging a web application in a fixed layout (`WEB-INF/`, etc.) meant to be **deployed into a servlet container** (Tomcat, Jetty, WildFly) that supplies the runtime.

Both are just ZIP files with a defined structure and a different file extension.

## Internal Layout

```
my-lib.jar                         my-app.war
├── META-INF/                      ├── META-INF/
│   └── MANIFEST.MF                │   └── MANIFEST.MF
├── com/example/...                ├── index.html, css, js      (static web content)
│   └── *.class                    └── WEB-INF/
└── resources (properties, etc.)       ├── web.xml              (optional deployment descriptor)
                                        ├── classes/
                                        │   └── com/example/*.class  (your code)
                                        └── lib/
                                            └── *.jar            (dependency JARs)
```

Key structural point: a WAR's own classes live under `WEB-INF/classes` and its
dependencies under `WEB-INF/lib`. Nothing under `WEB-INF/` is directly reachable
by a browser — the container controls access.

## Key Differences

| Aspect | JAR | WAR |
|--------|-----|-----|
| Full name | Java ARchive | Web Application ARchive |
| Primary purpose | Libraries, utilities, standalone apps | Web apps deployed to a servlet container |
| Required layout | None (any package structure) | Fixed: `WEB-INF/`, `WEB-INF/classes`, `WEB-INF/lib` |
| How it runs | `java -jar app.jar` (if executable) or on the classpath | Deployed into Tomcat/Jetty/WildFly, which runs it |
| Who provides the server | Bundled/embedded (or none) | The external servlet container |
| Entry point | `Main-Class` in `MANIFEST.MF` | Servlet container + mappings (annotations or `web.xml`) |
| Dependencies | On the classpath / fat-jar bundled | Under `WEB-INF/lib` |
| Typical deployment | Copy and run the artifact | Drop into the container's `webapps/` (or deploy via its tooling) |
| Spring Boot default | Yes (embedded Tomcat) | Opt-in (for external containers) |

## Executable (Fat/Uber) JAR

A plain JAR is not necessarily runnable on its own — it may just be a library.
An **executable JAR** declares a `Main-Class` and typically bundles all
dependencies inside it (a "fat" or "uber" JAR), so it runs with a single command:

```bash
java -jar my-app.jar
```

Spring Boot's default build produces exactly this: a fat JAR with an **embedded**
servlet container, so the application *is* the server. This is why every Spring
Boot service in this workspace runs with `./gradlew bootRun` or `java -jar`
without installing Tomcat separately.

## WAR Deployment Model

A WAR takes the opposite approach: the application does **not** carry a server.
You hand the `.war` to a running servlet container, which loads it, wires up the
servlets/filters, and serves it under a context path:

```
tomcat/webapps/my-app.war   →   http://host:8080/my-app/...
```

The container owns the HTTP listener, thread pool, and lifecycle; the WAR
contributes only the application. Multiple WARs can share one container, each
under its own context path.

## Building Each (this workspace)

Most projects here are **Gradle + Spring Boot** and produce executable JARs by
default via the `org.springframework.boot` plugin — no extra configuration needed.

To produce a **WAR** instead (Gradle):

```gradle
plugins {
    id 'war'                                  // adds the war task + WEB-INF layout
    id 'org.springframework.boot'
}

dependencies {
    // Let the external container supply the servlet runtime; don't bundle it.
    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'
}
```

(The `prep1/servlet-prep` module in this workspace is a **Maven** project that
builds a WAR — `<packaging>war</packaging>` with a `provided`-scope
`jakarta.servlet-api`, since Tomcat 10+ supplies the Servlet API at runtime.)

For a Spring Boot app that must deploy as a WAR to an external container, the
main class also extends `SpringBootServletInitializer` so the container can
bootstrap it.

## How to Choose

**Reach for a JAR when:**
- Building a library or shared module consumed by other code.
- Building a standalone service or CLI (Spring Boot fat JAR, microservices).
- You want self-contained deployment: one artifact, `java -jar`, done.
- Running in containers/Kubernetes, where "the app is the process" fits cleanly.

**Reach for a WAR when:**
- Deploying into an existing, managed servlet container (corporate Tomcat/WildFly).
- Multiple web apps must share one container instance and its resources.
- Organizational or operational standards mandate container-managed deployment.
- Integrating with a legacy stack built around `webapps/` deployment.

## Common Misconceptions

1. **"A JAR can't be a web application"** — It can. A Spring Boot fat JAR *is* a
   full web app; it just embeds the server instead of being deployed into one.
2. **"A WAR is a special kind of JAR"** — They share the ZIP format and `META-INF`,
   but a WAR has a required `WEB-INF/` layout and is not run with `java -jar`.
3. **"You need a WAR to run on Tomcat"** — A Spring Boot JAR embeds Tomcat already.
   You only need a WAR when deploying into a *separate*, externally managed Tomcat.
4. **"Every JAR is executable"** — Only JARs with a `Main-Class` in the manifest
   are runnable directly; many JARs are libraries meant for the classpath.
5. **"WAR is obsolete"** — Fat JARs dominate cloud-native deployments, but WARs are
   still common where a shared, centrally managed application server is the standard.
```
