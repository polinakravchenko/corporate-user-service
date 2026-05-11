# Practical Work 4 - Spring Security + Thymeleaf + DAO Provider

This version adds a database-backed security model to `Corporate User Service`.

## Implemented requirements

- Spring Security is integrated into the Thymeleaf application.
- Authentication uses a DAO Security Provider backed by PostgreSQL/Hibernate.
- Database schema and default security data are created by Liquibase.
- Default roles: `ADMIN`, `CUSTOMER`.
- Default users:
  - `admin` / `AdminPass123` with role `ADMIN`.
  - `customer` / `CustomerPass123` with role `CUSTOMER`.
- `ADMIN` can manage users and create new customers.
- `CUSTOMER` can access only `/cabinet` and update only own profile/password.
- `/users/**` is restricted to `ADMIN`.
- `/cabinet/**` is restricted to `CUSTOMER`.

## Key files

```text
src/main/java/com/example/corporateusers/config/SecurityConfig.java
src/main/java/com/example/corporateusers/security/CorporateUserDetailsService.java
src/main/java/com/example/corporateusers/security/CorporateUserDetails.java
src/main/resources/db/changelog/db.changelog-master.xml
src/main/resources/templates/auth/login.html
src/main/resources/templates/cabinet/index.html
```

## Run

Start PostgreSQL:

```bash
cd docker
docker compose up -d postgres pgadmin
```

Run application:

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080/login
```

## Verify in PostgreSQL

```sql
select * from system_roles;
select u.id, u.username, u.status, r.code
from system_users u
join user_roles ur on ur.user_id = u.id
join system_roles r on r.id = ur.role_id;
```
