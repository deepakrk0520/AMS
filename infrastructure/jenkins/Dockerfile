# Local Jenkins for ShopSphere CI (P1-T10): Jenkins LTS (JDK 21) plus the tools
# the Jenkinsfile needs: Maven, Node.js and the Docker CLI (talks to the host
# Docker daemon through the mounted socket; see docs/architecture/ci-jenkins.md).
FROM jenkins/jenkins:lts-jdk21

ARG MAVEN_VERSION=3.9.9
ARG NODE_VERSION=20.18.1
ARG DOCKER_CLI_VERSION=27.3.1

USER root
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates xz-utils; \
    rm -rf /var/lib/apt/lists/*; \
    arch="$(uname -m)"; \
    case "$arch" in \
      x86_64)  node_arch=x64;   docker_arch=x86_64 ;; \
      aarch64) node_arch=arm64; docker_arch=aarch64 ;; \
      *) echo "unsupported architecture: $arch" >&2; exit 1 ;; \
    esac; \
    # Maven
    curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
      | tar -xz -C /opt; \
    ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn; \
    # Node.js (includes npm)
    curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${node_arch}.tar.xz" \
      | tar -xJ -C /opt; \
    ln -s "/opt/node-v${NODE_VERSION}-linux-${node_arch}/bin/node" /usr/local/bin/node; \
    ln -s "/opt/node-v${NODE_VERSION}-linux-${node_arch}/bin/npm" /usr/local/bin/npm; \
    ln -s "/opt/node-v${NODE_VERSION}-linux-${node_arch}/bin/npx" /usr/local/bin/npx; \
    # Docker CLI only (no daemon)
    curl -fsSL "https://download.docker.com/linux/static/stable/${docker_arch}/docker-${DOCKER_CLI_VERSION}.tgz" \
      | tar -xz -C /usr/local/bin --strip-components=1 docker/docker

USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
