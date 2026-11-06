MAVEN_IMAGE ?= maven:3.9-eclipse-temurin-17

.PHONY: build test

# Plain build on the host. The integration test starts an embedded Keycloak that binds
# localhost:8080 (hardcoded in the Keycloak test framework), so it fails if that port is taken.
# Use `make test` when in doubt.
build:
	mvn -B clean package

# Same build in a throwaway container: its network namespace is isolated, so the embedded
# Keycloak and the in-JVM mail server never collide with anything on the host.
test:
	docker run --rm -u $$(id -u):$$(id -g) -e HOME=/tmp \
		-v "$$PWD":/work -v "$$HOME/.m2":/m2 -w /work \
		$(MAVEN_IMAGE) mvn -B -Dmaven.repo.local=/m2/repository clean package
