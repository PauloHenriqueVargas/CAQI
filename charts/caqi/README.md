# caqi (chart Helm)

Deploy de uma **instância isolada por município** do Sistema CAQ/CAQi em Kubernetes.

Padrão: **1 chart, N releases** — cada release corresponde a um município (tenant), em namespace dedicado. Isolamento por namespace + NetworkPolicy + Secret/PVC dedicados.

## Estrutura

- `Chart.yaml` — metadados
- `values.yaml` — valores padrão (devem funcionar em qualquer cloud com adaptações)
- `values-<municipio>.yaml` — overrides por município (commit em repo seguro, **secrets fora**)
- `templates/` — manifests parametrizados:
  - `deployment-engine.yaml` (implementado — exemplo)
  - `deployment-financeiro.yaml`, `deployment-escolar.yaml`, `deployment-compliance.yaml`, `deployment-web.yaml` (TODO Fase 1.B)
  - `ingress.yaml`, `configmap.yaml`, `secret.yaml`, `serviceaccount.yaml` (TODO Fase 1.B)

## Quickstart

```bash
# Pré-requisitos: cluster k8s, kubectl, helm 3.13+, ingress-nginx, cert-manager

# 1. Criar namespace por município
kubectl create namespace caqi-municipio-exemplo

# 2. Criar secrets (DB, RabbitMQ, Gov.br) — exemplo:
kubectl -n caqi-municipio-exemplo create secret generic caqi-db \
  --from-literal=password='SENHA_FORTE'
kubectl -n caqi-municipio-exemplo create secret generic caqi-rabbitmq \
  --from-literal=password='SENHA_FORTE_RMQ'

# 3. Deploy
helm upgrade --install caqi-municipio-exemplo ./charts/caqi \
  -f charts/caqi/values-municipio-exemplo.yaml \
  --namespace caqi-municipio-exemplo
```

## Provisionamento de novo município

Em `templates/`, futura automação (Fase 9):
1. Operator interno cria namespace + secrets + DB schema dedicado
2. Render de `values-<municipio>.yaml` a partir de cadastro central
3. ArgoCD ApplicationSet cria a release Helm
4. CronJob inicial faz seed de dados (escolas via Censo INEP)

## Status

- [x] Chart.yaml + values.yaml
- [x] values-municipio-exemplo.yaml
- [x] templates/_helpers.tpl
- [x] deployment-engine.yaml (exemplo)
- [ ] Demais deployments (Fase 1.B)
- [ ] Ingress + cert-manager
- [ ] NetworkPolicy (isolamento entre tenants no mesmo cluster)
- [ ] PodSecurityPolicy / Gatekeeper
- [ ] HPA / VPA
- [ ] Backup CronJob (pg_dump → S3 com KMS)
