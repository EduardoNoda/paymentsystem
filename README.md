# Sistema de Pagamento em Java
[](https://github.com/EduardoNoda/paymentsystem#sistema-de-pagamentos-em-java)
Este projeto implementa o núcleo de um sistema de processamento de pagamentos com cartão de crédito (simulado), com foco em consistência de estado, idempotência, controle de concorrência, safe retry e auditoria confiável, mesmo sob falhas de infraestrutura, timeouts e execuções concorrentes.

O objetivo não é integrar com meios de pagamento reais, mas modelar corretamente o problema, tratando o banco de dados como guardião das invariantes de domínio.

# O Que o Sistema Resolve

[](https://github.com/EduardoNoda/paymentsystem#o-que-o-sistema-resolve)

Este sistema resolve problemas clássicos de processamento de pagamentos, como:

-  Requisições duplicadas causadas por:

   - falhas de rede

   -  timeouts

   - retries automáticos

   - concorrência entre múltiplas execuções

- Processamentos interrompidos no meio da execução

- Estados inconsistentes causados por falhas parciais

- A solução garante que:

   - A mesma intenção de pagamento é processada uma única vez

   - Requisições duplicadas retornam sempre o mesmo resultado

   - Falhas nunca geram aprovações indevidas

   - O estado do pagamento é persistido e auditado em todas as transições
# O Que o Sistema Não Faz

[](https://github.com/EduardoNoda/paymentsystem#o-que-o-sistema-não-faz)

Este projeto deliberadamente não implementa:

- Processamento de dinheiro real

- Integração com bancos ou operadoras

- Antifraude

- Gestão de usuários

- Estornos ou chargebacks

- Comunicação externa real com gateways

O foco está exclusivamente no design correto do domínio e da infraestrutura de estado.

# Estados Possíveis de Pagamento

[](https://github.com/EduardoNoda/paymentsystem#estados-possíveis-de-pagamento)

. RECEBIDO - significa registrar que a requisição foi aceita, sendo importante para gerenciamento de logs do sistema. Estado transitório.

. PROCESSANDO - significa gerenciar processamento e integridade de dados, lidar com concorrência de solicitações e detectar pagamentos que passaram por alguma inconsistência. Estado transitório.

. EM_ANALISE - usado quando o lease do processamento da requisição é expirado e o sistema admite que há um problema com a transação que deve ser resolvido através de um job do sistema. Estado transitório.

. APROVADO - significa que o estado de processamento foi concluído com sucesso e o pagamento foi efetuado. Estado final.

. RECUSADO - significa que o estado de processamento foi concluído com sucesso mas o pagamento não foi aprovado devido a alguma condição inválida. Estado final.

. FALHA - signfica que o estado de processamento fi concluído com sucesso mas o pagamento não foi finalizado devido a algum erro técnico, falha na infraestrutura ou exceção inesperada. Estado final.

. CANCELADO_ADMINISTRADOR - é admitido quando o job não resolve o problema de um lease expirado e o problema é repassado para intervenção humana. Caso não seja solucionado, a solicitação é cancelada e recebe esse status. Estado final.

# Transições de Estado Possíveis e Impossíveis

Possíveis:

- RECEBIDO -> PROCESSANDO

- PROCESSANDO -> APROVADO

- PROCESSANDO -> RECUSADO

- PROCESSANDO -> FALHA

- PROCESSANDO -> EM_ANALISE

- EM_ANALISE -> PROCESSANDO

- EM_ANALISE -> CANCELADO_ADMINISTRADOR

Impossíveis:

- RECEBIDO -> APROVADO

- RECEBIDO -> RECUSADO

- RECEBIDO -> FALHA

- RECEBIDO -> EM_ANALISE

- RECEBIDO -> CANCELADO_ADMINISTRADOR

- EM_ANALISE -> APROVADO

- EM_ANALISE -> RECUSADO

- EM_ANALISE -> FALHA

APROVADO; RECUSADO; FALHA; CANCELADO_ADMINISTRADOR são estados finais imutáveis, uma vez determinados não devem ser alterados.

# Garantias do Sistema

[](https://github.com/EduardoNoda/paymentsystem#garantias-do-sistema)

- Um pagamento nunca pode ser aprovado duas vezes.

- Requisições duplicadas geram o mesmo resultado.

- Falhas no sistema não gera aprovações.

- Pagamentos nunca mudam de estado.

- Estado inconsistente resulta em falha.

# Auditoria de Estado

Toda mudança de status é auditada automaticamente no banco de dados.

## Características

- Auditoria feita via trigger SQL

- Nenhuma dependência de código de aplicação

- Registro contém:

   - ID do pagamento

   - Novo status

   - Origem da ação (API / JOB / ADMIN)

   - Descrição da transição

   - Timestamp

Isso garante auditoria confiável mesmo em falhas inesperadas da aplicação.

# Modelo de Autoridade

[](https://github.com/EduardoNoda/paymentsystem#modelo-de-autoridade)

---

## Cliente

[](https://github.com/EduardoNoda/paymentsystem#cliente)

O cliente deve solicitar pagamento e consultar status atual da solicitação. Ele não pode reprocessar a solicitação, nem cancelar ou decidir o resultado.

---

## Sistema (API)

[](https://github.com/EduardoNoda/paymentsystem#sistema-api)

O sistema pode PROCESSAR a solicitação, executar as regras de negócio deo pagamento uma vez e retornar uma resposta, caso houver. Ele não pode solicitar de novo o gateway, retroceder status atual ou decidir casos de PROCESSANDO incertos.

---

## Job (BATCH)

[](https://github.com/EduardoNoda/paymentsystem#job-batch)

O job detecta solicitações em processo com lease expirado, muda status atual para EM_ANALISE, notifica e gera relatório do problema. Ele não chama regra de negócio (gateway) e não altera status da solicitação para um estado FINAL.

---

## Intervenção Humana

[](https://github.com/EduardoNoda/paymentsystem#intervenção-humana)

A intervenção humana possui autoridade excepcional, o desenvolvedor pode analisar o relatório gerado pelo job, resolver o problmea ou cancelar adminstrativamente (CANCELADO_ADMINISTRADOR).
