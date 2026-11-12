# Jenkins CI (P1-T10)

Continuous Integration for ShopSphere: on every push or pull request Jenkins
builds and tests the backend and frontend, builds the backend Docker image and
publishes test results. **CI only**: images are not pushed anywhere and nothing is deployed.

```
GitHub ──webhook──► Jenkins (container, Compose profile "ci")
                      │  Jenkinsfile (repo root)
                      ├─ Maven build + tests ──► throwaway PostgreSQL + Redis containers
                      ├─ npm ci + vite build
                      └─ docker build shopsphere-api:<commit>   (host Docker via socket)
```

## Start Jenkins locally

```bash
docker compose --profile ci up -d --build jenkins     # first build downloads plugins, a few minutes
docker exec shopsphere-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open http://localhost:8081 (`JENKINS_PORT` to change), paste the initial admin
password, then **skip "Install suggested plugins"** (the required plugins are
already in the image) and create your admin user. Data persists in the
`shopsphere-jenkins-home` volume (jobs, credentials, Maven cache).

Plain `docker compose up -d` does not start Jenkins.

### What is in the Jenkins image (`infrastructure/jenkins/`)

JDK 21 (Jenkins LTS), Maven 3.9, Node.js 20, Docker CLI 27 (client only), and
the plugins in `plugins.txt`: `workflow-aggregator`, `pipeline-stage-view`,
`git`, `github`, `github-branch-source`, `junit`, `credentials`,
`credentials-binding`, `timestamper`, `ws-cleanup`.

### Docker access and its risk

Pipelines build images and start test containers through the **host Docker
socket** (`/var/run/docker.sock` mounted into Jenkins). Anyone who can edit a
pipeline can then control the host's Docker, which is effectively root on the
host. Acceptable for a local single-user setup; for anything shared, use a
dedicated build agent or rootless/remote Docker. The Jenkins user gets access
through `group_add: DOCKER_GID` (default `0`, correct for Docker Desktop). On
Linux set `DOCKER_GID=$(stat -c %g /var/run/docker.sock)` in `.env`.

## Pipeline stages (`Jenkinsfile`)

| Stage | What it does |
|---|---|
| Checkout | `checkout scm`; prints branch, PR number, commit, author, message; sets `IMAGE_TAG` to the short commit hash |
| Backend Build | `mvn -DskipTests package` in `backend/shopsphere-api` |
| Backend Tests | starts throwaway `postgres:16-alpine` and `redis:7-alpine` containers on `shopsphere-network` (random password, unique names), runs `mvn test` against them, publishes JUnit reports; a test failure fails the build |
| Frontend Build | `npm ci` (falls back to `npm install` without a lockfile) and `npm run build` in `frontend/shopsphere-web` |
| Frontend Tests | runs `npm test` only if a `test` script exists; otherwise logs that none is configured (currently none) |
| Docker Build | builds `shopsphere-api:<commit>` and `shopsphere-api:ci-<build number>`; no push |
| Pipeline Summary | prints the result and the test report URL |

Post actions: archive the backend jar (successful builds only), print a failure hint, and
always remove the test containers and clean the workspace. The frontend has no
Dockerfile in this repository, so no frontend image is built yet.

Configuration (environment, all optional): `CI_NETWORK` (default
`shopsphere-network`; must be reachable from the agent). No secrets are in the
repository: the test database password is generated per build and the only
credentials Jenkins needs (GitHub, below) live in the Jenkins credential store.

Builds use the same integration tests as local development: `ShopsphereApiApplicationTests`,
`RedisCacheIntegrationTest` and `ObservabilityIntegrationTest` get PostgreSQL/Redis from
the `DB_*` and `SPRING_DATA_REDIS_HOST` variables the pipeline sets.

## Connect GitHub

Repository: `https://github.com/deepakrk0520/AMS`. The `Jenkinsfile` is at the repository root.

1. **Credentials (private repos or higher API limits):** GitHub → Settings →
   Developer settings → fine-grained personal access token with read access to
   the repository's contents, metadata and pull requests (plus commit statuses
   if you want build status on PRs). In Jenkins: *Manage Jenkins → Credentials →
   (global) → Add credentials → Username with password* (username = GitHub user,
   password = the token). Never put the token in the Jenkinsfile or repository.
2. **Create the job:** *New Item → Multibranch Pipeline* (name `shopsphere`) →
   *Branch Sources → Add source → GitHub* → select the credentials, enter the
   repository URL, keep behaviours *Discover branches* and *Discover pull
   requests from origin*. *Build Configuration*: mode *by Jenkinsfile*, script path
   `Jenkinsfile`. Save; Jenkins scans and builds every branch and PR with a Jenkinsfile.
   3. **Webhook (automatic builds on push/PR):** GitHub repo → Settings → Webhooks →
   Add webhook: Payload URL `http://<public-jenkins-host>/github-webhook/`, content type
   `application/json`, events *Pushes* and *Pull requests*. GitHub must be able to reach
   Jenkins, so for a laptop expose it with a tunnel (e.g. `ngrok http 8081`, `smee.io`)
   and set *Manage Jenkins → System → Jenkins URL* to that public URL. Without a
   webhook use *Scan Multibranch Pipeline Now* or a periodic scan.

## Run a build

Open the job → *Scan Multibranch Pipeline Now* (or push a commit) → open the
branch → *Build Now*. Test results: build page → *Test Result*; image:
`docker image ls shopsphere-api`.

To try the pipeline without GitHub, create a *Pipeline* job with *Pipeline
script from SCM* → Git → any reachable repository URL containing the Jenkinsfile.

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `permission denied` on `/var/run/docker.sock` | `DOCKER_GID` does not match the socket's group; set it (see above) and recreate the container |
| `docker: command not found`, `mvn: command not found` | pipeline ran on an agent without the tools; use the project's Jenkins image |
| Test services "did not become ready" | image pull failed or `CI_NETWORK` does not exist: `docker network ls`; start the stack once with `docker compose up -d` |
| `UnknownHostException: ci-...-pg` / Redis connection refused in tests | Jenkins container is not on `CI_NETWORK` (it is when started via the Compose service) |
| `Port ... already allocated` for Jenkins | `JENKINS_PORT` clashes (8081 default); change it in `.env` |
| `npm ci` fails: lockfile out of sync | run `npm install` locally and commit `package-lock.json` |
| `docker build` cannot download Maven dependencies | network/proxy issue inside the Docker VM; retry |
| Leftover `ci-jenkins-*` containers | a build was killed hard: `docker ps -a --filter name=ci-` then `docker rm -f` them |
| GitHub rate-limit or 401 during scan | credentials missing/expired; re-create the token credential |
| Webhook shows delivery failures | Jenkins URL not publicly reachable or missing trailing `/github-webhook/` |

## Out of scope

Pushing images to a registry, deployment/CD (AWS, Kubernetes, Terraform), frontend
tests and image, code-quality/security scanners, Jenkins configuration-as-code.
