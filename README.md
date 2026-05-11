# Corporate User Service

Spring Boot application for the practical work topic: **Creating a Spring Boot application using Hibernate and Thymeleaf**.

## Functional scope

The application implements a user service for a corporate system:

- Create system users.
- Store account data: username, email, full name, status.
- Store passwords only as BCrypt hashes.
- Change user password.
- Save password change history.
- Change user status: `PENDING`, `ACTIVE`, `BLOCKED`, `DISABLED`.
- Search and filter users.
- Render UI with Thymeleaf.
- Persist data in PostgreSQL through Hibernate/JPA.

## Technologies

- Java 17
- Spring Boot 3.5.10
- Spring Web MVC
- Spring Data JPA
- Hibernate
- Thymeleaf
- PostgreSQL
- BCrypt password hashing
- Jakarta Validation

## Database infrastructure

The supplied `docker-compose.yml` is located in `docker/docker-compose.yml` and includes:

- PostgreSQL 15
- pgAdmin
- MongoDB
- Mongo Express
- RabbitMQ

This practical work uses PostgreSQL. MongoDB and RabbitMQ remain in the compose file for the distributed corporate systems environment.

## Start infrastructure

```bash
cd docker
docker compose up -d
```

PostgreSQL connection:

```text
url: jdbc:postgresql://localhost:5432/mydb
username: admin
password: admin
```

pgAdmin:

```text
http://localhost:5050
email: admin@mail.com
password: admin
```

## Start application

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/users
```

## Main pages

```text
GET  /users                 User list, search and status filter
GET  /users/new             Create user form
POST /users                 Create user
GET  /users/{id}            User details and password history
GET  /users/{id}/edit       Edit user data
POST /users/{id}/edit       Save user data
GET  /users/{id}/password   Change password form
POST /users/{id}/password   Change password
GET  /users/{id}/status     Change status form
POST /users/{id}/status     Change status
POST /users/{id}/delete     Delete user
```

## JPA model

`SystemUser` is the main entity. `PasswordHistory` stores each password hash change. The relationship is:

```text
SystemUser 1 --- * PasswordHistory
```

Hibernate creates these tables:

```text
system_users
password_history
```

## Verification in PostgreSQL

```sql
select id, username, email, full_name, status, password_hash
from system_users
order by id;

select ph.id, ph.user_id, ph.changed_at, ph.changed_by, ph.password_hash
from password_history ph
order by ph.id;
```

## Practical work report notes

Recommended screenshots:

1. Docker Compose file and PostgreSQL container.
2. Spring Boot project structure.
3. Entity classes `SystemUser` and `PasswordHistory`.
4. Repository with custom search query.
5. Service method for password hashing and status change.
6. Thymeleaf user list page.
7. Create user page.
8. User details page with password history.
9. Change password page.
10. PostgreSQL tables in pgAdmin.
