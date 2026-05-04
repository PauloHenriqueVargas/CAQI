# Política de Privacidade — Sistema CAQ/CAQi

> **Modelo de referência.** O município contratante (controlador) deve adotar
> esta política após análise jurídica, atualizar dados de contato e publicar
> no portal de transparência. A redação aqui já está em linguagem clara
> conforme exige LGPD art. 9°, §1°.

**Versão:** 0.1
**Vigente desde:** [data da publicação]

---

## Quem somos

O Sistema CAQ/CAQi é operado pela **[Município]** (controlador) com suporte
técnico da **Controller — Consultoria & Assessoria** (operador). Trata
dados para apurar e gerir o Custo Aluno Qualidade (CAQ/CAQi) da rede
municipal de educação, conforme PNE Meta 20 e Lei 14.113/2020.

---

## Quais dados tratamos

### Sobre alunos
Nome, data de nascimento, nome do responsável, escola e turma de matrícula,
endereço (urbano/rural). **Não tratamos** dados de saúde, religião,
orientação política, biometria ou outros sensíveis.

### Sobre servidores da educação
Nome, CPF (armazenado de forma irreversível — hash SHA-256), cargo, carga
horária, lotação, eventos de folha (rubricas, valores, competência).

### Sobre fornecedores
CNPJ, nome empresarial, município, dados contratuais. **Pessoas jurídicas
(PJ) não estão protegidas pela LGPD**, mas tratamos com mesmo cuidado.

### Sobre usuários do sistema
Nome, email funcional, perfil de acesso (LEITOR/GESTOR/ADMIN), histórico
de login.

---

## Para que usamos

- **Cálculo do CAQ/CAQi** por etapa/escola/ano (alunos)
- **Comprovação do limite Fundeb 70%** (servidores em efetivo exercício)
- **Pagamento de contratos e cálculo de retenções tributárias** (fornecedores)
- **Controle de acesso e auditoria** (usuários)
- **Transparência ativa** (LAI/LRF) — publicação de dados agregados e contratos

---

## Com quem compartilhamos

- **FNDE (SIOPE)**: receitas/despesas educacionais agregadas (obrigação legal)
- **INEP (Censo Escolar)**: matrículas e dados de escolas (obrigação legal)
- **Receita Federal, INSS, Município (ISS)**: retenções tributárias (obrigação legal)
- **TCE, MP, CGU**: em fiscalização (obrigação legal)
- **PNCP**: contratos da Lei 14.133 (publicidade obrigatória)
- **Cidadão (portal LAI)**: dados públicos por natureza, sem PII de menor

**Não compartilhamos** dados com terceiros para finalidades comerciais.

---

## Por quanto tempo guardamos

- **Folha de pessoal**: 30 anos (prescrição INSS)
- **Despesas, receitas, contratos**: 5 anos (Lei 14.133)
- **Cálculos CAQ + memória**: 5 anos
- **Matrícula**: durante a vinculação + 5 anos
- **Logs de auditoria**: 5 anos (mínimo)

Após o prazo: eliminação ou anonimização irreversível.

---

## Como protegemos

- TLS 1.3 obrigatório em todas as comunicações
- Senhas armazenadas com hash + salt; nunca em texto claro
- CPF de servidor armazenado apenas como hash (não reversível)
- Banco com criptografia at rest (TDE) e segredos em Vault/KMS
- Logs imutáveis com cadeia SHA-256 (adulteração detectável)
- Acesso por perfis (RBAC) com auditoria de toda alteração
- Backups diários encriptados em armazenamento separado

---

## Seus direitos (LGPD art. 18)

Você pode, a qualquer momento e gratuitamente, solicitar:
1. **Confirmação** se tratamos seus dados
2. **Acesso** aos dados que temos
3. **Correção** de dados incompletos ou desatualizados
4. **Anonimização** ou eliminação de dados desnecessários
5. **Portabilidade** a outro fornecedor de serviço (quando aplicável)
6. **Informação** sobre compartilhamentos realizados
7. **Revogação** de consentimento (quando o tratamento depender dele)

**Importante:** dados tratados por obrigação legal (folha, matrícula, contratos)
não podem ser eliminados antes do prazo legal — mesmo a pedido do titular.

### Como exercer

- Email do Encarregado (DPO): **[email-dpo@municipio.gov.br]** *(a definir)*
- Ouvidoria: **[contato da ouvidoria]**
- Prazo de resposta: até **15 dias** (LGPD art. 18 §6°)

---

## Atualizações desta política

Esta política será atualizada quando houver mudança material no tratamento.
Versões anteriores ficam disponíveis em [link]. A data da última revisão
está no topo deste documento.

---

## Encarregado pelo tratamento de dados (DPO)

Nome: **[a designar]**
Email: **[email-dpo@municipio.gov.br]**
Telefone: **[telefone]**

---

## Reclamações

Caso considere que seus direitos foram violados, você pode reclamar
ao DPO acima ou diretamente à **Autoridade Nacional de Proteção de
Dados (ANPD)** via [https://www.gov.br/anpd/pt-br](https://www.gov.br/anpd/pt-br).
