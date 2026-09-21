# 0002. Keycloak as identity provider, API as JWT resource server

- Status: Accepted
- Date: 2026-09-21

## Context and problem statement

The application needs login, two roles (`MANAGER`, `EMPLOYEE`) and a browser single-page app as client.
Writing password storage, sessions and token handling in the API would be security-critical code with no
business value for this project.

## Considered options

- Spring Security form login with users in the application database
- Keycloak as external OpenID Connect provider; the API validates access tokens (OAuth2 resource server)
- A hosted identity provider (Auth0, Cognito, Entra ID)

## Decision outcome

Chosen option: **Keycloak**, because it is open source, runs locally in Docker with a versioned realm export
(`realm-export/hr-realm.json`) and speaks standard OIDC, so the API code is not tied to it.

- The UI uses the public client `hr-api-login` with authorization code + PKCE; no client secret exists in the browser.
- The API is a stateless resource server: it validates the JWT signature (JWKS) and the issuer, maps
  `realm_access.roles` to Spring roles (`KeycloakRealmRoleConverter`) and authorises with `@PreAuthorize`.
  It never creates an HTTP session.
- Employees are linked to Keycloak users by `username` (the token's `preferred_username`), not by display name.
- Everything except the API docs, `/public/**` and the health probe requires a token (deny by default).

### Consequences

- Good: no credentials in the API; any OIDC provider with a roles claim could replace Keycloak by changing
  configuration and the role converter.
- Good: the tests use mocked JWTs (Spring Security Test), so they do not need Keycloak.
- Bad: local development needs a running Keycloak (docker compose).
- Bad: the token issuer is the URL the *browser* uses (`http://localhost:8082/...`). In the containerised stack the
  API therefore validates `iss` against that URL but downloads the keys from `http://keycloak:8080/...` on the
  Docker network (`jwk-set-uri`). A deployment with a public Keycloak hostname does not need this split.
