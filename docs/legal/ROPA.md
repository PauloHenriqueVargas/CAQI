# Registro de Operações de Tratamento (ROPA)

> Documento exigido pelo art. 37 da LGPD. Mantém o inventário das operações
> de tratamento de dados pessoais, atualizado a cada mudança no sistema.

**Última revisão:** 2026-05-04
**Próxima revisão obrigatória:** anualmente ou a cada nova tabela com PII

---

## Operações ativas

### 1. Matrícula de aluno

| Campo | Valor |
|---|---|
| Tabela | `aluno` + `matricula` |
| Categoria | Dado pessoal (nome, data nascimento, responsável, localização) — **inclui menor de idade** |
| Base legal | LGPD art. 7°, III + art. 14 (proteção do melhor interesse da criança) |
| Finalidade | Apurar matrículas para cálculo CAQ/CAQi e para envio ao Censo Escolar (INEP) |
| Tempo de retenção | Vinculação ativa + 5 anos após desligamento |
| Compartilhamento | INEP (Censo Escolar — obrigação legal); TCE em fiscalização |
| Medidas de segurança | TLS, RBAC (apenas role ADMIN escreve), encriptação at rest, auditoria |
| Direitos do titular | Confirmação, acesso, correção; **eliminação só após prazo de retenção** (não pode anular obrigação legal de cadastro escolar) |

### 2. Folha de pessoal (servidor da educação)

| Campo | Valor |
|---|---|
| Tabela | `pessoa_servidor` + `folha_evento` |
| Categoria | Dado pessoal (nome, **CPF armazenado como hash SHA-256**, cargo, carga horária, lotação) |
| Base legal | LGPD art. 7°, II — obrigação legal: comprovação de efetivo exercício (Lei 14.113/2020 art. 26) |
| Finalidade | Apurar despesas de pessoal para o cálculo do limite Fundeb 70% |
| Tempo de retenção | **30 anos** (prescrição INSS) |
| Compartilhamento | INSS (eSocial), Receita Federal, TCE |
| Medidas de segurança | CPF nunca em texto claro (hash); pgcrypto; logs de acesso a folha; segregação de função (ADMIN ≠ GESTOR) |
| Direitos do titular | Confirmação, acesso aos eventos próprios; correção via RH; **eliminação somente após prazo de prescrição** |

### 3. Fornecedor PJ + contrato

| Campo | Valor |
|---|---|
| Tabela | `fornecedor` + `contrato` |
| Categoria | Dados de PJ (CNPJ, nome empresarial, município) — **não PII de pessoa física** |
| Base legal | Execução de contrato (LGPD art. 7°, V) + obrigação legal (Lei 14.133 art. 174) |
| Finalidade | Operacionalizar pagamentos, retenções tributárias, publicação no PNCP/portal LAI |
| Tempo de retenção | 5 anos após término do contrato (Lei 14.133 art. 169) |
| Compartilhamento | PNCP (publicidade), Receita Federal (retenções), TCE, LAI |
| Medidas de segurança | RBAC; logs |
| Direitos do titular | Não aplicável (PJ; LGPD se aplica a pessoa natural) — porém, processo de exercício pelo representante legal mantido por boa prática |

### 4. Cadastro de usuários do sistema

| Campo | Valor |
|---|---|
| Tabela | `usuario` |
| Categoria | Dado pessoal (nome, email funcional, perfil) |
| Base legal | LGPD art. 7°, IX — interesse legítimo (controle de acesso) |
| Finalidade | Autenticação e autorização (RBAC) |
| Tempo de retenção | Durante a vinculação ativa + 1 ano após desativação (auditoria de logins históricos) |
| Compartilhamento | Não — uso interno |
| Medidas de segurança | Senha hashed; sessão JWT 8h; MFA TOTP (Fase 9.B) |
| Direitos do titular | Acesso, correção, **eliminação após desativação** |

### 5. Logs de auditoria (chain SHA-256)

| Campo | Valor |
|---|---|
| Tabela | `log_auditoria` |
| Categoria | Operacional — referência a usuario_id, tabela, ação, hashes |
| Base legal | LGPD art. 7°, II — obrigação legal (LRF, LAI, controle interno/externo) |
| Finalidade | Trilha imutável para auditoria; detecção de adulteração (cadeia Merkle SHA-256) |
| Tempo de retenção | **5 anos mínimo** (prescrição administrativa); se sob investigação, prazo maior |
| Compartilhamento | TCE, MP, CGU em apuração |
| Medidas de segurança | Cadeia SHA-256 (hash_antes/hash_depois) — adulteração detectável offline; lock pessimista no insert serializa concorrência |
| Direitos do titular | Acesso ao registro próprio; **NÃO há direito de eliminação** (obrigação legal sobrepõe) |

### 6. ROPA (este registro)

| Campo | Valor |
|---|---|
| Local | `docs/legal/ROPA.md` (versionado em git) |
| Atualização | A cada nova tabela com PII ou nova finalidade |
| Responsável | DPO + arquiteto técnico |

---

## Operações descartadas / não realizadas

- **NÃO há tratamento de dado sensível** (saúde, religião, biometria etc.)
- **NÃO há decisão automatizada com efeitos jurídicos sobre titulares pessoa-física** (decisões legais são sobre execução orçamentária)
- **NÃO há perfilamento** ou marketing
- **NÃO há transferência internacional** nos defaults (cloud BR; on-prem opcional)

---

## Procedimento de atualização do ROPA

1. Toda nova tabela com PII exige PR + atualização deste arquivo
2. Toda nova finalidade no Sistema CAQ/CAQi exige avaliação do DPO
3. Mudanças materiais disparam revisão do RIPD (`DPIA_RIPD.md`)
4. Auditoria anual: DPO confere se ROPA reflete operações reais
