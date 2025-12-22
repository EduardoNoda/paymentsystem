# Sistema de Pagamentos em Java
Este projeto implementa o núcleo do processamento de pagamentos com cartão de crédito (simulado), focado em garantir consistência de estado e idempotência em cenários de falha, concorrência e requisições duplicadas.

# O Que o Sistema Resolve
O sistema visa solucionar problemas que geram requisições duplicadas devido a múltiplas solicitações de pagamento, causado por falhas de rede, timeout ou tentativa de retry.

Isso é solucionado garantindo que a mesma intenção de pagamento seja processada uma única vez. Requisições duplicadas sempre retornam o mesmo resultado, independente da quantidade de solicitações

Em caso de falhas no sistema, é persistido o estado atual do pagamento antes e após o processamento, sendo utilizado para garantir repostas consistentes e preservar a integridade de dados do cliente.

# O Que o Sistema Não Faz

. Não processa dinheiro real

. Não integra com bancos ou operadoras

. Não implementa antifraude

. Não gerencia usuários

. Não realiza estornos ou chargebacks

# Estados Possíveis de Pagamento

. RECEBIDO - significa registrar que a requisição foi aceita, sendo ponto de ancoragem para garantir idêmpotencia. Estado transitório.

. PROCESSANDO - significa gerenciar processamento e integridade de dados, lidar com concorrência de solicitações e detectar pagamentos que passaram por alguma inconsistência. Estado transitório.

. APROVADO - significa que o estado de processamento foi concluído com sucesso e o pagamento foi efetuado. Estado final.

. RECUSADO - significa que o estado de processamento foi concluído com sucesso mas o pagamento não foi aprovado devido a alguma condição inválida. Estado final.

. FALHA - signfica que o estado de processamento fi concluído com sucesso mas o pagamento não foi finalizado devido a algum erro técnico, falha na infraestrutura ou exceção inesperada. Estado final.

# Modelo de Domínio

## Entidade: pagamento

Tarefa única de cobrar um valor do cliente, pode ser processada uma única vez retornando exatamente um estado final.

### Atributos

A entidade deve conter identificador único de acesso, uma chave lógica de pagamento para garantir que duas ou mais requisições tenham o mesmo resultado, o valor e a moeda que serão processados, os estados transitórios ou finais, data de criação e de finalização do processo para controle de linha do tempo.

Sendo assim, devem ser atributos imutáveis: valor, moeda e estado do pagamento.

# Garantias do Sistema

. Um pagamento nunca pode ser aprovado duas vezes.

. Requisições duplicadas geram o mesmo resultado.

. Falhas no sistema não gera aprovações.

. Pagamentos nunca mudam de estado.

. Estado inconsistente resulta em falha.
