# Relatório de Impacto à Proteção de Dados Pessoais (RIPD/DPIA)

> Documento exigido pelo art. 38 da LGPD quando o tratamento puder gerar
> riscos às liberdades civis e direitos fundamentais. Este RIPD cobre o
> Sistema CAQ/CAQi quando operado em piloto/produção por uma instância
> municipal.

**Versão:** 0.1 — MVP de referência
**Última revisão:** 2026-05-04
**Próxima revisão obrigatória:** anualmente ou a cada mudança material no tratamento

---

## 1. Identificação dos agentes

| Papel | Identificação |
|---|---|
| **Controlador** | Município contratante (ente público); o Secretário Municipal de Educação atua como ponto focal operacional |
| **Operador** | Controller — Consultoria & Assessoria (CNPJ a cadastrar) — fornece o software, opera infraestrutura conforme contrato |
| **Encarregado (DPO)** | A designar pelo município (LGPD art. 41); preferencialmente servidor concursado da estrutura administrativa |
| **ANPD** | Autoridade Nacional de Proteção de Dados — instância recursal externa |

Contrato entre Controlador e Operador deve incluir cláusulas mínimas do art. 39: instruções, segurança, sigilo, comunicação de incidentes, eliminação ou devolução de dados ao término.

---

## 2. Descrição do tratamento

O Sistema CAQ/CAQi gerencia:
- Cálculo do Custo Aluno Qualidade por etapa/escola/ano (informação agregada)
- Orçamento, despesas e receitas educacionais (impostos, Fundeb)
- Cadastro de fornecedores PJ e contratos (Lei 14.133)
- Folha de pessoal docente e técnico (servidores)
- Matrículas e dados básicos de alunos (dados pessoais — incluindo de menor de idade)
- Notificações automáticas de violação legal e cadeia imutável de auditoria
- Portal público de transparência (LAI)

Hospedagem: 1 instância isolada por município (Helm release dedicado em namespace Kubernetes).

---

## 3. Categorias de dados pessoais tratados

| Tabela | Dados | Sensíveis (LGPD art. 5°, II)? | PII de criança/adolescente (ECA + LGPD art. 14)? |
|---|---|---|---|
| `aluno` | nome, data nascimento, responsável, localização | Não | **SIM** — atenção redobrada |
| `pessoa_servidor` | nome, CPF (hash SHA-256, sem reverso), cargo, carga horária, lotação | Não | Não |
| `usuario` | nome, perfil, email funcional | Não | Não |
| `fornecedor` | CNPJ (PJ — não PII pessoal), nome PJ, município | Não (PJ) | Não |
| `log_auditoria` | usuario_id (referência), ação, timestamp | Não | Não |

**O sistema NÃO armazena** dados sensíveis (saúde, biometria, religião, orientação política/sexual, etc.) e NÃO realiza decisões automatizadas com efeitos jurídicos.

---

## 4. Finalidades do tratamento

| Finalidade | Categoria de dados | Base legal (LGPD art. 7° / 11° / 14°) |
|---|---|---|
| Apurar matrícula para cálculo CAQ/CAQi | aluno | Art. 7°, III — execução de política pública (CF art. 205) |
| Comprovar efetivo exercício para 70% Fundeb | pessoa_servidor | Art. 7°, II — cumprimento de obrigação legal (Lei 14.113/2020) |
| Pagar fornecedores e calcular retenções | fornecedor | Art. 7°, V — execução de contrato |
| Auditar operações para controle interno/externo | log_auditoria | Art. 7°, II — obrigação legal (LRF, LAI) |
| Publicar dados em portal público | dados agregados, contratos PJ | Art. 7°, II — LAI 12.527/2011 art. 8°; LRF art. 48-A |

---

## 5. Compartilhamento de dados

| Destinatário | Dados | Finalidade |
|---|---|---|
| FNDE (SIOPE) | Receitas/despesas agregadas + percentuais Fundeb/MDE | Obrigação legal (Lei 14.113) |
| INEP (Censo Escolar) | Quantitativo de matrículas, dados básicos da escola | Obrigação legal (LDB) |
| TCE/TCM, MP, controle interno | Logs auditoria + relatórios consolidados | Obrigação legal — fiscalização |
| Cidadão (LAI) | Dados públicos por natureza (contratos, valores agregados) — **sem PII de menor** | LAI 12.527/2011 |
| Operador (Controller) | Acesso técnico para operar o software | Contrato + art. 39 LGPD |

**NÃO há transferência internacional** prevista nas configurações default.

---

## 6. Tempo de retenção

| Dado | Retenção mínima | Base |
|---|---|---|
| Despesas, receitas, contratos | **5 anos** após exercício | Lei 14.133 art. 169; LC 101 |
| Folha de pagamento | **30 anos** | Decreto-lei 5.452/1943 (CLT prescrição); INSS exige 30 anos |
| Cálculos CAQ + memória | **5 anos** após exercício | Documento de gestão pública (LRF) |
| log_auditoria | **5 anos** mínimo | Boas práticas (TCE) |
| Aluno (matrícula) | Durante a vinculação ativa + **5 anos** após desligamento | LGPD art. 16 — necessidade da finalidade |
| Notificação de compliance | **5 anos** | Documento de gestão; pode ser pseudonimizado após resolução |

Após o prazo: **eliminação** ou **anonimização** (transformar PII em estatístico irreversível).

---

## 7. Riscos identificados e mitigações

| Risco | Probabilidade | Impacto | Mitigação implementada |
|---|---|---|---|
| Vazamento de banco (acesso não autorizado) | Baixa | Alto | TLS 1.3 obrigatório; senhas via Vault/Secrets KMS; postgres só acessível pela rede interna do namespace; NetworkPolicy default-deny |
| Acesso indevido por usuário interno | Média | Médio | RBAC granular (LEITOR/GESTOR/ADMIN); cadeia SHA-256 imutável detecta adulteração; logs de acesso |
| Reidentificação a partir de dados públicos | Baixa | Médio | API pública omite PII; SIOPE público omite pendências internas; agregação por escola, não por aluno |
| Adulteração de logs de auditoria | Muito baixa | Crítico | Chain SHA-256 (Merkle) com hash_antes/depois; verificarIntegridade() detecta offline |
| Indisponibilidade prolongada | Baixa | Alto | Backup pg_dump diário (S3 + KMS); RPO 24h, RTO 4h; runbooks documentados |
| Falha no cálculo (regra legal violada não detectada) | Baixa | Alto | Test de regressão contra planilha CAQi; AvaliadorComplianceService aplica regras MDE/Fundeb/VAAT; bloqueador opcional rejeita empenho violador |
| LGPD: titular não consegue exercer direitos | Média | Médio | Interface DPO (futuro); processos documentados; resposta em 15 dias (LGPD art. 18 §6°) |

---

## 8. Direitos dos titulares (LGPD art. 18)

O sistema deve responder, no prazo legal, aos pedidos de:
1. **Confirmação** da existência do tratamento
2. **Acesso** aos dados
3. **Correção** de dados incompletos/incorretos
4. **Anonimização**, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade
5. **Portabilidade** a outro fornecedor
6. **Eliminação** dos dados tratados com consentimento (não aplicável aos dados tratados por obrigação legal — ver art. 16)
7. **Informação** sobre compartilhamento
8. **Revogação** de consentimento (quando aplicável)

**Canal de exercício:** ouvidoria municipal + email do DPO (a designar). SLA: 15 dias.

---

## 9. Decisões automatizadas

O sistema executa **2 decisões automatizadas** com potencial impacto:

| Decisão | Base | Direito de revisão |
|---|---|---|
| Bloqueio de empenho violador (`bloquear-empenhos-violadores=true`) | Regra legal (Fundeb 70% / MDE 25% / VAAT 15%) | GESTOR pode reclassificar a despesa; ADMIN pode desligar a flag |
| Geração de notificação de violação | Regra legal | GESTOR pode mover status para `ignorada` com justificativa |

LGPD art. 20: titular tem direito a revisão das decisões automatizadas. O sistema **NÃO** decide sobre direitos de pessoas físicas — decide sobre execução orçamentária. Mesmo assim, o motor de regras é auditável e cada decisão deixa registro em log_auditoria.

---

## 10. Aprovações

- [ ] Encarregado (DPO) — após designação
- [ ] Procuradoria municipal — análise jurídica
- [ ] Secretaria de Educação — aprovação operacional
- [ ] Tribunal de Contas / controle interno — ciência

Versão controlada via git (este arquivo). Mudanças exigem nova revisão e aprovação.
