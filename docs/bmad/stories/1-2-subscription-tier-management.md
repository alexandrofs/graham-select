# Story 1.2: Subscription Tier Management

Status: ready-for-dev

## Story

As a platform user,
I want to view my current subscription level (Trial/Free/Premium),
so that I know which features are available to me and when my trial expires.

## Acceptance Criteria

1. **User Entity Update:** The `users` table and `User` entity must be updated to include:
    - `tier` (Enum: TRIAL, FREE, PREMIUM) [ ]
    - `trial_ends_at` (LocalDateTime) [ ]
2. **Default Tier:** New users (provisioned via Google OAuth2) must be assigned the `TRIAL` tier by default. [ ]
3. **Trial Duration:** The `trial_ends_at` must be set to 30 days from the account creation date. [ ]
4. **Profile API:** A new endpoint `GET /api/v1/users/me` must return the current user's profile, including the tier and remaining trial days. [ ]
5. **Trial Expiration Logic:** A mechanism (e.g., a simple check during login or a scheduled job) must be able to identify expired trials. For this story, focus on the data model and the API return. [ ]
6. **Tests:** Integration tests must verify that new users get 30 days of trial and that the API returns the correct tier. [ ]

## Tasks / Subtasks

- [ ] Backend: Model & Persistence
  - [ ] Update `User` entity with `tier` and `trialEndsAt`
  - [ ] Create Liquibase changelog to add columns to `users` table
  - [ ] Update `UserRepository` if necessary
- [ ] Backend: Business Logic (Provisioning)
  - [ ] Modify `OAuth2UserProvisioningConverter` to set default tier and trial end date
- [ ] Backend: API Layer
  - [ ] Create `UserProfileResponse` DTO
  - [ ] Create `UserController` with `GET /me` endpoint
- [ ] Backend: Verification
  - [ ] Implement `UserTierManagementIT` to verify default tier and trial period
  - [ ] Verify API response for authenticated user

## Dev Notes

- **Enum Handling:** Use `@Enumerated(EnumType.STRING)` for the tier field.
- **Security Context:** Use the `SecurityContext` populated in Story 1.1 to identify the current user.
- **Timezone:** Use `UTC` for all date-time operations.
