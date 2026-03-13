# Story 1.1: Google OAuth2 Auto-provisioning

Status: in-progress

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a new or returning user,
I want to log in using my Google account,
so that my account is automatically created or accessed without a separate registration form.

## Acceptance Criteria

1. **Security Dependency:** The project must include `spring-boot-starter-oauth2-resource-server`. [x]
2. **Database Schema:** A `users` table must exist to store at least `google_id` (sub), `email`, and `full_name`. [x]
3. **Auto-provisioning:** On the first successful login with a valid Google JWT, a new record must be created in the `users` table if the `sub` doesn't exist. [x]
4. **JWT Validation:** The backend must validate the Google ID Token (issued by accounts.google.com) following the `OAuth2 Resource Server` pattern. [x]
5. **Security Context:** After successful validation, the `SecurityContext` must be populated with the user's internal ID/Email. [x]
6. **Tests:** Must include integration tests using `Testcontainers` (MySQL) to verify provisioning. [x]

## Tasks / Subtasks

- [x] Backend: Infrastructure Setup (AC: 1, 4)
  - [x] Add `spring-boot-starter-oauth2-resource-server` to `backend/api/pom.xml`
  - [x] Configure Google JWT issuer in `application.yml`: `https://accounts.google.com`
- [x] Backend: Persistence Layer (AC: 2)
  - [x] Create Liquibase changelog `07-create-users-table.yaml`
  - [x] Register changelog in `db.changelog-master.yaml`
  - [x] Create `User` entity and `UserRepository` in `common`
- [x] Backend: Authentication Logic (AC: 3, 5)
  - [x] Implement `SecurityConfig` to configure `oauth2ResourceServer`
  - [x] Implement `OAuth2UserProvisioningConverter` to handle user lookup/provisioning and populate SecurityContext with internal ID
- [x] Backend: Verification (AC: 6)
  - [x] Implement `UserAutoProvisioningIT` using `Testcontainers`
  - [x] Verify that a second login doesn't create a duplicate user

## Dev Notes

- **Architecture Pattern:** Follow Clean Architecture as defined in [Architecture Decision Document](file:///Users/alexandrofs/Documents/projects/graham-select/docs/bmad/planning-artifacts/architecture.md).
- **Security:** Use `Row-Level Security` or consistent `userId` filtering as a long-term goal; for this story, focus on establishing the `userId` in the context.
- **Improved Performance:** Using `JwtAuthenticationConverter` instead of a filter ensures provisioning happens once per JWT validation cycle.

## Dev Agent Record

### Agent Model Used

Antigravity (Claude 3.5 Sonnet)

### File List

- [pom.xml](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/pom.xml)
- [pom.xml](file:///Users/alexandrofs/Documents/projects/graham-select/backend/common/pom.xml)
- [ApiServiceApplication.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/ApiServiceApplication.java)
- [User.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java)
- [UserRepository.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java)
- [SecurityConfig.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfig.java)
- [OAuth2UserProvisioningConverter.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java)
- [UserAutoProvisioningIT.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserAutoProvisioningIT.java)
- [UploadServiceDelegateTest.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/src/test/java/afsdigital/grahamselect/api/upload/web/UploadServiceDelegateTest.java)
- [application.yml](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/src/main/resources/application.yml)
- [07-create-users-table.yaml](file:///Users/alexandrofs/Documents/projects/graham-select/backend/common/src/main/resources/db/changelog/07-create-users-table.yaml)
- [db.changelog-master.yaml](file:///Users/alexandrofs/Documents/projects/graham-select/backend/common/src/main/resources/db/changelog/db.changelog-master.yaml)
