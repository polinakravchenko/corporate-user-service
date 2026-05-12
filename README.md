# Corporate User Service — Spring Security + Thymeleaf + Keycloak

Corporate User Service is a Spring Boot application for managing system users in a corporate environment. The application uses PostgreSQL, Hibernate/JPA, Thymeleaf and Spring Security. In this version authentication is delegated to Keycloak through OpenID Connect / OAuth2 Login.

## Practical work 5 scope

Implemented features:

- Keycloak integration for login.
- Spring Security OAuth2 Login configuration.
- Realm roles: `ADMIN`, `CUSTOMER`.
- Custom Keycloak role mapper from token claims to Spring Security authorities.
- `ADMIN` access to user-management pages under `/users/**`.
- `CUSTOMER` access only to the personal cabinet under `/cabinet/**`.
- Local PostgreSQL storage for application user profile data.
- Liquibase migrations for users, roles, user-role relation, password history and Keycloak subject column.
- Synchronization of authenticated Keycloak users into the local application database.
- Thymeleaf pages for login, admin user management and customer cabinet.

Spring Security's OAuth2 Login feature lets an application authenticate users using an external OAuth2/OpenID Connect provider. Keycloak is used here as that identity provider.

## Test accounts

Keycloak realm import creates two users:

| Role | Username | Password |
|---|---|---|
| ADMIN | `admin` | `AdminPass123` |
| CUSTOMER | `customer` | `CustomerPass123` |

The same users are also inserted into the local PostgreSQL application database through Liquibase.

## Start infrastructure

Go to the docker folder and start PostgreSQL, pgAdmin and Keycloak:

```bash
cd docker
docker compose up -d postgres pgadmin keycloak
```

Services:

| Service | URL |
|---|---|
| PostgreSQL | `localhost:5432` |
| pgAdmin | `http://localhost:5050` |
| Keycloak | `http://localhost:8082` |

Keycloak admin console:

```text
http://localhost:8082/admin
```

Bootstrap Keycloak admin:

```text
username: keycloak-admin
password: keycloak-admin
```

Application realm:

```text
corporate-users
```

Application client:

```text
corporate-user-service
```

## Start the application

From the project root:

```bash
mvn spring-boot:run
```

or with Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Open:

```text
http://localhost:8080/login
```

Click **Увійти через Keycloak** and authenticate using one of the test users.

## Access rules

| URL | Required role |
|---|---|
| `/login` | Public |
| `/users/**` | `ADMIN` |
| `/cabinet/**` | `CUSTOMER` |

After login:

- `ADMIN` is redirected to `/users`.
- `CUSTOMER` is redirected to `/cabinet`.

## Role mapping

Keycloak sends roles in token claims such as:

```json
{
  "realm_access": {
    "roles": ["ADMIN"]
  }
}
```

The application maps those roles to Spring Security authorities:

```text
ADMIN -> ROLE_ADMIN
CUSTOMER -> ROLE_CUSTOMER
```

The mapping is implemented in:

```text
src/main/java/com/example/corporateusers/security/keycloak/KeycloakRoleMapper.java
```

## Local user synchronization

After successful OAuth2 login, the application synchronizes Keycloak user profile data into the local PostgreSQL database. This allows the application to keep domain-specific user data while delegating authentication to Keycloak.

Implementation:

```text
src/main/java/com/example/corporateusers/security/keycloak/KeycloakUserSynchronizer.java
```

The local database stores:

- username;
- email;
- full name;
- status;
- roles;
- Keycloak subject.

## Database verification

Use pgAdmin or psql:

```sql
select * from system_roles;

select
    u.id,
    u.username,
    u.email,
    u.full_name,
    u.status,
    u.keycloak_subject,
    r.code as role
from system_users u
join user_roles ur on ur.user_id = u.id
join system_roles r on r.id = ur.role_id
order by u.id;
```

## Important notes

If you previously ran the non-Keycloak version and Liquibase reports that tables already exist, clean the schema or recreate the Docker volume:

```bash
docker compose down -v
docker compose up -d postgres pgadmin keycloak
```

For a cleaner Liquibase setup, Hibernate is configured with:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

So Liquibase owns schema creation and Hibernate only validates the schema.
