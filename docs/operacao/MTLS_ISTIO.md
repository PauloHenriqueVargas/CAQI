# mTLS via Istio Service Mesh

Defesa em profundidade para tráfego pod-to-pod do Sistema CAQ/CAQi:
**toda comunicação entre os 4 microserviços + web BFF é criptografada
e autenticada com certificado X.509 mTLS** emitido e rotacionado
automaticamente pelo plano de controle Istio (`istiod`).

> Ameaça que isso mitiga: pod hostil/comprometido no mesmo cluster
> (lateral movement), MITM no plano de dados Kubernetes (CNI
> comprometido), exfiltração via sniffer no kube-proxy.

## Pré-requisitos no cluster

1. **Istio instalado** — Operator OU ambient mesh:
   ```bash
   # Profile padrão (sidecar)
   istioctl install --set profile=default -y
   ```
2. **Namespace marcado para sidecar injection** — feito **uma vez
   por município** ao criar o namespace:
   ```bash
   kubectl label namespace caqi-municipio-exemplo istio-injection=enabled
   ```
3. **CRDs Istio**: `PeerAuthentication`, `DestinationRule`,
   `ServiceEntry`, `AuthorizationPolicy`. Vêm com a instalação Istio.

## Ativando no chart

```yaml
# values-municipio-exemplo.yaml
istio:
  enabled: true
  mtlsMode: STRICT       # PERMISSIVE durante migração; STRICT em prod
```

`helm upgrade` aplica:

- **`PeerAuthentication`** (`security.istio.io/v1`) — força mTLS no
  lado *servidor* de todos os pods do namespace (`mtls.mode: STRICT`).
  Pods sem sidecar não conseguem aceitar conexões dos serviços CAQi.
- **`DestinationRule`** (`networking.istio.io/v1`) — força
  `ISTIO_MUTUAL` no lado *cliente* para qualquer destino dentro do
  cluster (`*.<ns>.svc.cluster.local`). Garante que chamadas saindo
  apresentem o certificado do sidecar.

## Política de migração STRICT

Em prod, a transição segura é:

1. **Pre-flight**: subir Istio + relabel namespace + helm upgrade com
   `mtlsMode: PERMISSIVE`. Pods rolam recebendo sidecar; tráfego
   continua aceitando ambos (sidecars já injetados começam a usar mTLS,
   workloads sem sidecar continuam plain).
2. **Verificação**: monitorar `istio_requests_total` por
   `connection_security_policy=mutual_tls` no Grafana até **100%** das
   conexões serem mTLS.
3. **Switch para STRICT**: helm upgrade alterando
   `istio.mtlsMode: STRICT`. Pods sem sidecar param de conseguir
   conectar (esperado).
4. **Pós-flight**: rodar smoke-tests do `docker-compose` análogo
   (chamadas REST entre serviços) — todos devem responder.

Documentar a operação em ATA do CACS-Fundeb (atende auditoria do TCE).

## Tráfego para fora do mesh

Postgres externo (RDS/Cloud SQL), RabbitMQ gerenciado, MinIO/S3 e
Gov.br OAuth2 não fazem parte do mesh. Para tráfego saindo deve-se:

1. **`ServiceEntry`** declarando o host externo;
2. **`DestinationRule`** com `tls.mode: SIMPLE` ou `DISABLE` conforme o
   serviço externo (DBs com TLS próprio = SIMPLE; servidor sem TLS =
   DISABLE em rede privada).

Modelos prontos a serem adicionados via PR específico:

```yaml
apiVersion: networking.istio.io/v1
kind: ServiceEntry
metadata:
  name: postgres-external
spec:
  hosts:
    - rds.caqi-municipio-exemplo.amazonaws.com
  ports:
    - number: 5432
      name: tcp-postgres
      protocol: TCP
  resolution: DNS
  location: MESH_EXTERNAL
```

## Observabilidade

Com Istio ligado, métricas adicionais aparecem em
`/actuator/prometheus` dos sidecars (porta 15020) e via Kiali se
instalado. Dashboards relevantes:

- **Topologia em tempo-real** (Kiali) — visualiza chamadas
  engine→financeiro, web→engine, compliance→financeiro, com indicador
  de mTLS por aresta
- **Latência P95/P99** por hop — overhead típico do sidecar é 1-3ms
- **Falhas de handshake** — devem ser zero após STRICT estabilizar

## Troubleshooting

| Sintoma | Causa provável | Correção |
|---|---|---|
| Pod não recebe sidecar | Namespace sem label `istio-injection=enabled` | `kubectl label ns NS istio-injection=enabled --overwrite`, depois rollout restart |
| 503 Upstream Connect Error | mTLS STRICT mas pod alvo sem sidecar | Aplicar label de injection + reiniciar pod alvo, OU temporariamente PERMISSIVE |
| Latência P99 cresce >50ms | Sidecar com CPU starved | Aumentar `proxy.resources.limits.cpu` no IstioOperator |
| `connection reset by peer` | Cert expirado (raro — istiod rotaciona) | `kubectl rollout restart deployment` para forçar reissue |

## Custo computacional

- **CPU sidecar**: ~50-100m por pod em carga normal; pico ~500m
- **Memória sidecar**: ~50-150Mi por pod
- **Latência adicional**: 1-3ms P95 por hop
- **Throughput**: redução de ~5-10% comparado a tráfego direto

Para um município pequeno (4 microserviços × 1 réplica + web), o overhead
total é ~250m CPU + 400Mi memória — desprezível em qualquer cluster
prod-grade.

## Defer: AuthorizationPolicy granular

A próxima evolução (Sprint 9.D) adiciona `AuthorizationPolicy` por
serviço para implementar o **princípio do menor privilégio em rede**:

- engine só aceita chamadas do web BFF e do compliance
- financeiro só aceita do web BFF, engine e compliance
- compliance pode chamar financeiro mas o inverso é negado
- escolar não aceita chamadas internas (só /import via web)

Hoje (Sprint 9.B) qualquer pod com sidecar pode chamar qualquer outro —
mTLS garante criptografia mas não autorização L7.
