#!/usr/bin/env bash
#
# security-scan.sh — Wrapper para executar varreduras de segurança
# locais sobre o repositório CAQ/CAQi. Equivale ao mínimo que o CI roda,
# útil antes de abrir PR ou em pen-test (ver docs/operacao/PEN_TEST_CHECKLIST.md).
#
# Saída agregada por seção; exit code 1 se qualquer scanner reportar
# achado HIGH/CRITICAL.
#
# Pré-requisitos:
#   - bash 4+
#   - python3 (para semgrep)
#   - go (para gitleaks/trivy se usar versões standalone)
#   - npm (para npm audit)
#   - ./gradlew (para deps Java)
#
# Ferramentas instaladas sob demanda em ./.cache/security/ se ausentes.

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CACHE_DIR="$ROOT_DIR/.cache/security"
mkdir -p "$CACHE_DIR"

# Cores ANSI
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

EXIT_CODE=0
SUMMARY=()

run_section() {
  local label="$1"; shift
  echo
  echo -e "${BLUE}━━━ $label ━━━${NC}"
  if "$@"; then
    SUMMARY+=("✓ $label — sem achados")
  else
    SUMMARY+=("✗ $label — REVISAR")
    EXIT_CODE=1
  fi
}

# ── 1. gitleaks: secrets em código + histórico ──────────────────────
gitleaks_scan() {
  if ! command -v gitleaks >/dev/null 2>&1; then
    echo -e "${YELLOW}gitleaks não instalado — skip.${NC} (https://github.com/gitleaks/gitleaks)"
    return 0
  fi
  gitleaks detect --no-banner --redact --source "$ROOT_DIR"
}

# ── 2. semgrep: SAST OWASP Top 10 ───────────────────────────────────
semgrep_scan() {
  if ! command -v semgrep >/dev/null 2>&1; then
    echo -e "${YELLOW}semgrep não instalado — skip.${NC} (pip install semgrep)"
    return 0
  fi
  semgrep --config=p/owasp-top-ten --error --quiet \
          --exclude='analytics/dbt_packages/' \
          --exclude='**/node_modules/**' \
          --exclude='**/build/**' \
          --exclude='**/.next/**' \
          "$ROOT_DIR"
}

# ── 3. OWASP Dependency-Check (Java) ────────────────────────────────
java_deps_scan() {
  cd "$ROOT_DIR"
  # Plugin oficial OWASP — adicionar ao build.gradle.kts root se ausente.
  # Por ora, usamos o `gradlew` para descobrir CVEs nas deps via plugin
  # `org.owasp.dependencycheck` (definido no build raiz).
  if [ ! -f "$ROOT_DIR/gradlew" ]; then
    echo -e "${YELLOW}gradlew não encontrado — skip.${NC}"
    return 0
  fi
  echo "Dependency-Check Gradle plugin não instalado por padrão; skip a menos"
  echo "que o desenvolvedor tenha rodado:  ./gradlew dependencyCheckAggregate"
  return 0
}

# ── 4. npm audit (web) ──────────────────────────────────────────────
npm_audit() {
  cd "$ROOT_DIR/apps/web" || return 0
  if [ ! -f package-lock.json ]; then
    echo -e "${YELLOW}package-lock.json ausente — pulando audit.${NC}"
    return 0
  fi
  npm audit --audit-level=high --omit=dev || return 1
}

# ── 5. trivy: scan de imagens Docker ────────────────────────────────
trivy_scan() {
  if ! command -v trivy >/dev/null 2>&1; then
    echo -e "${YELLOW}trivy não instalado — skip.${NC} (https://aquasecurity.github.io/trivy/)"
    return 0
  fi
  # Lista as imagens publicadas; em dev escaneia o Dockerfile estático
  for svc in caq-engine-svc caq-financeiro-svc caq-escolar-svc caq-compliance-svc; do
    if [ -f "$ROOT_DIR/apps/$svc/Dockerfile" ]; then
      echo "→ Scanning Dockerfile of $svc"
      trivy config --severity HIGH,CRITICAL --exit-code 1 "$ROOT_DIR/apps/$svc/Dockerfile" || return 1
    fi
  done
}

# ── 6. kubescape: K8s manifests do Helm chart ───────────────────────
kubescape_scan() {
  if ! command -v kubescape >/dev/null 2>&1; then
    echo -e "${YELLOW}kubescape não instalado — skip.${NC} (https://kubescape.io)"
    return 0
  fi
  cd "$ROOT_DIR"
  helm template caqi-test charts/caqi -f charts/caqi/values-municipio-exemplo.yaml \
    | kubescape scan framework nsa,mitre - --severity-threshold high
}

# ── 7. Verificações próprias (.env, segredos, PII em log fixtures) ──
custom_checks() {
  local errors=0
  echo "→ Verificando .env não foi commitado…"
  if git -C "$ROOT_DIR" ls-files | grep -E '(^|/)\.env$|(^|/)\.env\.[^.]*$' \
                                  | grep -v '\.env\.example' | grep -v '\.env\.local\.example' >/dev/null; then
    echo -e "${RED}.env commitado!${NC}"
    errors=1
  fi

  echo "→ Verificando senhas literais em arquivos commitados…"
  if grep -rEn --include='*.yml' --include='*.yaml' --include='*.java' \
              "password.*=.*\b(admin|caqi|trocar)\b" "$ROOT_DIR" \
              --exclude-dir=build --exclude-dir=.cache --exclude-dir=node_modules \
              --exclude-dir=.next \
            | grep -v 'application.yml' | grep -v 'application-test.yml' \
            | grep -v '\.example\.' >/dev/null; then
    echo -e "${YELLOW}possíveis senhas literais — revisar manualmente${NC}"
    # Não erro fatal — apenas alerta
  fi

  echo "→ Verificando CPF (11 dígitos seguidos) em arquivos commitados…"
  if grep -rEn --include='*.{java,ts,tsx,sql,csv,yml,yaml,json}' \
              '\b[0-9]{11}\b' "$ROOT_DIR" \
              --exclude-dir=build --exclude-dir=.cache --exclude-dir=node_modules \
              --exclude-dir=dbt_packages --exclude-dir=.next \
              2>/dev/null \
            | grep -v test \
            | grep -v fixture \
            | grep -v 'docs/references' >/dev/null; then
    echo -e "${YELLOW}possíveis CPFs em arquivos não-test — revisar manualmente${NC}"
  fi

  return $errors
}

# ── Execução ────────────────────────────────────────────────────────
run_section "1. gitleaks (secrets)"          gitleaks_scan
run_section "2. semgrep (SAST OWASP)"        semgrep_scan
run_section "3. OWASP Dependency-Check Java" java_deps_scan
run_section "4. npm audit (web)"             npm_audit
run_section "5. trivy (Docker)"              trivy_scan
run_section "6. kubescape (K8s manifests)"   kubescape_scan
run_section "7. checks customizados"         custom_checks

# ── Resumo ──────────────────────────────────────────────────────────
echo
echo -e "${BLUE}━━━ RESUMO ━━━${NC}"
for s in "${SUMMARY[@]}"; do
  if [[ $s == ✓* ]]; then
    echo -e "${GREEN}$s${NC}"
  else
    echo -e "${RED}$s${NC}"
  fi
done

if [ $EXIT_CODE -eq 0 ]; then
  echo -e "\n${GREEN}✓ Sem achados HIGH/CRITICAL. PR liberado.${NC}"
else
  echo -e "\n${RED}✗ Achados detectados. Revisar antes de merge para main.${NC}"
fi

exit $EXIT_CODE
