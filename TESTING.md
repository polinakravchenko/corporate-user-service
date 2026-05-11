# Testing guide

The project contains JUnit 5 tests for service, repository, controller and end-to-end web scenarios.

## Test structure

```text
src/test/java/com/example/corporateusers
 ├── controller
 │   ├── HomeControllerTest.java
 │   └── UserControllerTest.java
 ├── e2e
 │   └── UserManagementE2ETest.java
 ├── repository
 │   └── SystemUserRepositoryTest.java
 ├── service
 │   └── UserServiceTest.java
 └── support
     └── TestFixtures.java

src/test/resources
 └── application-test.yml
```

## What is covered

- UserService business rules: create, update, status change, password change, delete, duplicate username/email validation, password history.
- SystemUserRepository and PasswordHistoryRepository custom queries.
- UserController MVC scenarios with MockMvc and mocked service layer.
- HomeController redirect from `/` to `/users`.
- End-to-end user lifecycle through real Spring context, real Thymeleaf views, MockMvc and H2 database.

## Run all tests

```bash
mvn test
```

On Windows with Maven Wrapper:

```powershell
.\mvnw.cmd test
```

## Run one test class

```bash
mvn -Dtest=UserServiceTest test
mvn -Dtest=UserControllerTest test
mvn -Dtest=UserManagementE2ETest test
```

## Database used in tests

Tests use H2 in PostgreSQL compatibility mode, configured in `src/test/resources/application-test.yml`. Docker and PostgreSQL are not required for running tests.

The demo data initializer is disabled in the `test` profile:

```yaml
app:
  demo-data:
    enabled: false
```
