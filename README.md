# REST Task Manager (Spring Boot)

> Spring Boot REST API for task management — H2 by default, MySQL when available.

## Prerequisites

- **JDK 17+**
- Maven 3.8+

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Quick Start

```bash
mvn spring-boot:run
```

Or:

```bash----
mvn clean package
java -jar target/task-manager-showcase-1.0-SNAPSHOT.jar
```

Server: **http://localhost:8080**

```bash
curl http://localhost:8080/health
```

---

## API Reference

### Health
```bash
curl http://localhost:8080/health
```

### Create a Task
```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Fix login bug","description":"NPE on null user","priority":"HIGH"}'
```

### List / filter
```bash
curl http://localhost:8080/tasks
curl "http://localhost:8080/tasks?status=IN_PROGRESS"
curl "http://localhost:8080/tasks?priority=HIGH"
curl "http://localhost:8080/tasks?status=IN_PROGRESS&priority=HIGH"
```

### Get / Update / Delete
```bash
curl http://localhost:8080/tasks/1
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
curl -X DELETE http://localhost:8080/tasks/1
```

---

## Task Lifecycle

```
TODO ──► IN_PROGRESS ──► DONE
              │
              └──────────► CANCELLED
```

Invalid transitions return **409 Conflict**.

---

## Running Tests

```bash
mvn test
```

| Test | Type | Coverage |
|------|------|----------|
| `TaskServiceImplTest` | Unit | Service + Mockito |
| `TaskIntegrationTest` | Integration | Full HTTP via `@SpringBootTest` |
| `TaskManagerApplicationTest` | Smoke | Context loads |

---

## Database — MySQL or H2 fallback

Configured in `DataSourceConfig`:

1. If `db.mysql.enabled=true` and MySQL is reachable → MySQL  
2. Otherwise → H2 in-memory  

### H2 (default)
```bash
mvn spring-boot:run
```

### MySQL
```sql
CREATE DATABASE taskdb;
```

Edit `application.properties` (or set env `DB_MYSQL_URL`, `DB_MYSQL_USERNAME`, `DB_MYSQL_PASSWORD`).

Force H2: `db.mysql.enabled=false`

### H2 Console (dev only)
The H2 web console is **disabled by default**. It is enabled only on the `dev` profile (activated automatically by `mvn spring-boot:run`). Do not enable it in production.

```bash
mvn spring-boot:run
# or, for a packaged jar:
java -jar target/task-manager-showcase-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Open **http://localhost:8080/h2-console**

 JDBC URL : `jdbc:h2:mem:taskdb`  
 User : `sa`  
 Password : *(empty)*  

## Configuration

| Key | Default | Notes |
|-----|---------|-------|
| `server.port` | `8080` | Also `SERVER_PORT` |
| `app.name` | `task-manager` | Used in `/health` |
| `db.mysql.enabled` | `true` | Probe MySQL first |
| `spring.h2.console.enabled` | `false` | `true` only on `dev` profile (`/h2-console`) |

