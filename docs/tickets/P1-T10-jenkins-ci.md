# P1-T10 - Jenkins CI Pipeline

**Commit:** `ci: establish jenkins ci pipeline`

## Delivered

- `Jenkinsfile` (repo root), declarative: Checkout, Backend Build, Backend
  Tests (JUnit published), Frontend Build (`npm ci` + vite build), Frontend
  Tests (runs only if a `test` script exists; none yet), Docker Build
  (`shopsphere-api:<commit>` and `:ci-<build>`, no push), Pipeline Summary.
  Post: archive the jar on success, remove test containers, clean workspace.
- Backend tests get throwaway PostgreSQL and Redis containers per build (random
  password, unique names, removed afterwards) on `shopsphere-network`.
- Local Jenkins: `infrastructure/jenkins/` (Dockerfile with Maven, Node 20,
  Docker CLI, plugins) and a `jenkins` service in `docker-compose.yml` under the
  opt-in `ci` profile, with a persistent volume.
- Docs: `docs/architecture/ci-jenkins.md` (start, plugins, GitHub credentials,
  Multibranch job, webhook, troubleshooting).
- Only non-CI change: `RedisCacheIntegrationTest` reads the Redis host from
  `SPRING_DATA_REDIS_HOST` (default `localhost`) so CI can point it at its container.

## Verify

```bash
docker compose --profile ci up -d --build jenkins
docker exec shopsphere-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
Then follow `docs/architecture/ci-jenkins.md`.

Result when delivered:
- Jenkinsfile passed Jenkins' declarative linter.
- Pipeline ran in the Jenkins container from a throwaway clone of the repo
  (checked out over HTTP): SUCCESS with 41/41 tests, frontend build, Docker image
  built, jar archived, test containers removed.
- A deliberately broken test: build FAILED at Backend Tests, JUnit showed 1
  failure, later stages skipped, no image built, test containers removed.
- Fixed two issues found by those runs (artifact archived before workspace
  cleanup; summary printing a null URL).

## Notes

- The Jenkins container mounts the host Docker socket (root-equivalent on the
  host); local use only. Documented in the CI doc.
- Not verified here: GitHub scanning, PR builds and webhook delivery (need
  GitHub access and a public Jenkins URL). Credentials are configured in
  Jenkins, never in the repository.
- No frontend image: the frontend has no Dockerfile in this repository.
- Jenkins data volume was removed after verification, so first start shows the
  normal setup wizard.

## Out of scope

Image registry push, CD/deployment (AWS, Kubernetes, Terraform), frontend
tests, scanners, Jenkins configuration-as-code.
