#!/usr/bin/env sh
set -eu
mkdir -p out
javac -d out src/main/java/*.java
java -cp out LegalTenantProvisioning "$@"
