#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

BOOMERANG_DIR="${SCRIPT_DIR}/Boomerang"
CRYPTOANALYSIS_DIR="${SCRIPT_DIR}/CryptoAnalysis"
CRYPTO_API_RULES_DIR="${SCRIPT_DIR}/Crypto-API-Rules"

# Ruleset subdirectories under Crypto-API-Rules, in install order
CRYPTO_API_RULES=(
  "JavaCryptographicArchitecture"
  "BouncyCastle"
  "BouncyCastle-JCA"
  "JCA-BouncyCastle-JCA"
  "Tink"
)

log() {
  printf '[build-local-deps] %s\n' "$1"
}

require_repo() {
  local repo_dir="$1"
  local repo_name="$2"

  if [[ ! -d "${repo_dir}" ]]; then
    printf '[build-local-deps] Missing expected repository: %s (%s)\n' "${repo_name}" "${repo_dir}" >&2
    exit 1
  fi
  if [[ ! -f "${repo_dir}/pom.xml" ]]; then
    printf '[build-local-deps] Repository %s exists but has no pom.xml (%s)\n' "${repo_name}" "${repo_dir}" >&2
    printf '[build-local-deps] This usually means git submodules are not checked out.\n' >&2
    printf '[build-local-deps] Run: git submodule update --init --recursive\n' >&2
    exit 1
  fi
}

build_repo() {
  local repo_dir="$1"
  local repo_name="$2"

  log "Installing ${repo_name}"
  (
    cd "${repo_dir}"
    mvn install -DskipTests -B
  )
}

resolve_maven_project_version() {
  local repo_dir="$1"

  (
    cd "${repo_dir}"
    mvn -q -N help:evaluate -Dexpression=project.version -DforceStdout
  ) | tail -n 1 | tr -d '\r'
}

build_cryptoanalysis() {
  local boomerang_version="$1"

  log "Installing CryptoAnalysis with spds.version=${boomerang_version}"
  (
    cd "${CRYPTOANALYSIS_DIR}"
    mvn install -DskipTests -Dspds.version="${boomerang_version}" -B
  )
}

log "Repository root: ${REPO_ROOT}"
log "Building local dependency repos from ${SCRIPT_DIR}"

require_repo "${BOOMERANG_DIR}" "Boomerang"
require_repo "${CRYPTOANALYSIS_DIR}" "CryptoAnalysis"

build_repo "${BOOMERANG_DIR}" "Boomerang"

BOOMERANG_VERSION="$(resolve_maven_project_version "${BOOMERANG_DIR}")"
if [[ -z "${BOOMERANG_VERSION}" ]]; then
  printf '[build-local-deps] Failed to resolve Boomerang Maven project version\n' >&2
  exit 1
fi
log "Resolved Boomerang project version: ${BOOMERANG_VERSION}"

build_cryptoanalysis "${BOOMERANG_VERSION}"

require_repo "${CRYPTO_API_RULES_DIR}" "Crypto-API-Rules"
for ruleset in "${CRYPTO_API_RULES[@]}"; do
  build_repo "${CRYPTO_API_RULES_DIR}/${ruleset}" "Crypto-API-Rules/${ruleset}"
done

log "Local dependency build finished"

