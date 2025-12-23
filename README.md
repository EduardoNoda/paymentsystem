
[](https://github.com/EduardoNoda/paymentsystem#sistema-de-pagamentos-em-java)

Este projeto implementa o núcleo do processamento de pagamentos com cartão de crédito (simulado), focado em garantir consistência de estado e idempotência em cenários de falha, concorrência e requisições duplicadas.

# O Que o Sistema Resolve

[](https://github.com/EduardoNoda/paymentsystem#o-que-o-sistema-resolve)

O sistema visa solucionar problemas que geram requisições duplicadas devido a múltiplas solicitações de pagamento, causado por falhas de rede, timeout ou tentativa de retry.

Isso é solucionado garantindo que a mesma intenção de pagamento seja processada uma única vez. Requisições duplicadas sempre retornam o mesmo resultado, independente da quantidade de solicitações

Em caso de falhas no sistema, é persistido o estado atual do pagamento antes e após o processamento, sendo utilizado para garantir repostas consistentes e preservar a integridade de dados do cliente.

# O Que o Sistema Não Faz

[](https://github.com/EduardoNoda/paymentsystem#o-que-o-sistema-não-faz)

. Não processa dinheiro real

. Não integra com bancos ou operadoras

. Não implementa antifraude

. Não gerencia usuários

. Não realiza estornos ou chargebacks

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
RECEBIDO -> PROCESSANDO
PROCESSANDO -> APROVADO
PROCESSANDO -> RECUSADO
PROCESSANDO -> FALHA
PROCESSANDO -> EM_ANALISE
EM_ANALISE -> PROCESSANDO
EM_ANALISE -> CANCELADO_ADMINISTRADOR

Impossíveis:
RECEBIDO -> APROVADO
RECEBIDO -> RECUSADO
RECEBIDO -> FALHA
RECEBIDO -> EM_ANALISE
RECEBIDO -> CANCELADO_ADMINISTRADOR
EM_ANALISE -> APROVADO
EM_ANALISE -> RECUSADO
EM_ANALISE -> FALHA
APROVADO; RECUSADO; FALHA; CANCELADO_ADMINISTRADOR são estados finais imutáveis, uma vez determinados não devem ser alterados.

# Modelo de Domínio

[](https://github.com/EduardoNoda/paymentsystem#modelo-de-domínio)

## Entidade: pagamento

[](https://github.com/EduardoNoda/paymentsystem#entidade-pagamento)

Tarefa única de cobrar um valor do cliente, pode ser processada uma única vez retornando exatamente um estado final.

### Atributos

[](https://github.com/EduardoNoda/paymentsystem#atributos)

A entidade deve conter identificador único de acesso, uma chave lógica de pagamento para garantir que duas ou mais requisições tenham o mesmo resultado, o valor e a moeda que serão processados, os estados transitórios ou finais, data de criação e de finalização do processo para controle de linha do tempo.

Sendo assim, devem ser atributos imutáveis: valor, moeda e estado do pagamento.

# Garantias do Sistema

[](https://github.com/EduardoNoda/paymentsystem#garantias-do-sistema)

. Um pagamento nunca pode ser aprovado duas vezes.

. Requisições duplicadas geram o mesmo resultado.

. Falhas no sistema não gera aprovações.

. Pagamentos nunca mudam de estado.

. Estado inconsistente resulta em falha.

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





# Pseudocódigo

    TIPO STATUS: ENUM (
        PROCESSANDO;
        EM_ANALISE;
        APROVADO;
        RECUSADO;
        FALHA;
        CANCELADO_ADMINSTRADOR
    )

    VAR

        solicitacao: REGISTRO
            id: INTEIRO
            chave_idempontecia: INTEIRO
            dados_pagamento: REGISTRO
            status_atual: STATUS
        FIM_REGISTRO

        resultado: STATUS

    FUNCAO pagamento(solicitacao): solicitacao
    INICIO

        solicitacao.status_atual <- STATUS.RECEBIDO
        
        INICIAR_TRANSACAO
        
        // verifica existencia da solicitação a partir da chave_idempotencia via restrição no banco de
        // dados (constranint), se ja existe, retorna o resultado atual da primeira solicitação e fina-
        // liza a funcao pagamento
        SE existe chave_idempotencia ENTAO
            SE solicitacao.status_atual = APROVADO ou RECUSADO ou FALHA ENTAO (xOU)
                retorna solicitacao.status_atual
            FIMSE

            SE solicitacao.status_atual = PROCESSANDO ENTAO
                SE lease não expirou ENTAO
                    COMMIT
                    retorne solicitacao.status_atual
                    
                SENAO SE lease expirou ENTAO
                    renovar lease

                FIMSE
            FIMSE
        FIMSE

        

        // se for possível inserir solicitação, se não for possível, o fluxo deve retornar
        solicitacao.status_atual = PROCESSANDO
        atribuir lease (1m)

        TENTAR
            // chama gateway para aprovação ou não do pagamento, retornando resultado
            resultado <- gateway(solicitacao.dados_pagamento)

            solicitacao.status <- resultado 
    
            COMMIT
            retorne solicitacao.status
        CAPTURAR
            // se houve erro técnico inesperado
            propagar erro
        FIM

    FIMFUNCAO

    FUNCAO gateway(requisicao)
    INICIO

    1. regra de negocio do sistema
    2. retorna FALHA, APROVADO ou RECUSADO

    FIMFUNCAO
