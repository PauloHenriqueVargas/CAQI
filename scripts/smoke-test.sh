#!/usr/bin/env bash
# Smoke test E2E — sobe a stack via docker-compose e verifica /health de cada serviço.
# Uso: ./scripts/smoke-test.sh [--keep]  (--keep não derruba os containers no final)

set -euo pipefail

KEEP=0
[[ "${1:-}" == "--keep" ]] && KEEP=1

cd "$(dirname "$0")/.."

cleanup() {
  if [[ $KEEP -eq 0 ]]; then
    echo
    echo "→ docker compose down -v"
    docker compose down -v --remove-orphans 2>/dev/null || true
  else
    echo
    echo "→ Containers preservados (--keep). Derrube com: docker compose down -v"
  fi
}
trap cleanup EXIT

echo "→ docker compose up --build --wait"
docker compose up --build -d --wait

echo
echo "→ Aguardando serviços ficarem saudáveis (até 180s)..."
DEADLINE=$(($(date +%s) + 180))

check_url() {
  local name=$1 url=$2 pattern=${3:-'"status":"UP"'}
  while [[ $(date +%s) -lt $DEADLINE ]]; do
    if body=$(curl -fsSL --max-time 5 "$url" 2>/dev/null); then
      if echo "$body" | grep -q "$pattern"; then
        echo "  ✓ $name → $url"
        return 0
      fi
    fi
    sleep 2
  done
  echo "  ✗ $name → $url (timeout)"
  echo "  Última resposta: ${body:-<vazia>}"
  return 1
}

FAILED=0
check_url "caq-engine-svc"    "http://localhost:8081/api/v1/health" || FAILED=1
check_url "caq-financeiro-svc" "http://localhost:8082/api/v1/health" || FAILED=1
check_url "caq-escolar-svc"   "http://localhost:8083/api/v1/health" || FAILED=1
check_url "caq-compliance-svc" "http://localhost:8084/api/v1/health" || FAILED=1
check_url "caqi-web"          "http://localhost:3000/api/health"     || FAILED=1

echo
if [[ $FAILED -eq 0 ]]; then
  echo "✓ SMOKE TEST PASSOU — todos os 5 serviços responderam UP"
  exit 0
else
  echo "✗ SMOKE TEST FALHOU — verifique logs com: docker compose logs"
  exit 1
fi
