[English](README.md) | [Português](README.pt-BR.md)

# Simplified Banking Service

O Simplified Banking Service é uma API REST em Java/Spring Boot para um domínio
deliberadamente pequeno de banco digital. Seu escopo documentado cobre criação
de contas, transferências seguras entre contas e histórico limitado de
movimentações, tendo consistência financeira e rastreabilidade como restrições
principais.

Atualmente, o serviço implementa criação de contas, tokens de idempotência para
transferências emitidos pelo servidor, transferências atômicas entre contas,
consultas paginadas de movimentações financeiras, notificações best effort via
RabbitMQ, métricas operacionais e suítes automatizadas de testes funcionais e
de carga.

## Modelo de entrega de engenharia

Criei este modelo de [agentes de IA](AGENTS.md), [workflows](.agents/workflows/README.md) e [skills](.agents/skills/) com a experiência
e os fundamentos de software que adquiri na carreira. Ele acelera entregas com qualidade, rastreabilidade e controle humano nos gates de maior impacto.

O processo de elaboração da solução e entrega segue esta sequência:

1. **Elaboração da solução:** entendimento do problema, objetivos do produto, requisitos, fluxos, restrições e decisões em aberto.
2. **Decisões duráveis:** criação de BDRs para decisões de negócio e produto, ADRs para arquitetura e atualização dos modelos de dados ou engineering standards quando necessário.
3. **Organização do escopo:** criação de épicos com suas respectivas user stories, critérios de aceite e limites explícitos.
4. **Planejamento da execução:** criação de execution plans com etapas ordenadas, riscos, estratégia de testes, validações e checkpoints retomáveis.
5. **Execução pelos workflows:** Design and Context → Feature Implementation → Quality Gates → AI Code Review → Integrated
   Functional Tests → Finalization and Documentation, sempre governada pelos [engineering standards](docs/engineering/README.md)
   e pela [Definition of Done](docs/engineering/definition-of-done.md).

Em conjunto, essas etapas mantêm cada mudança conectada a uma necessidade aprovada, sustentada por evidências objetivas de
validação e sujeita à supervisão humana antes de ações com consequências relevantes.

## Arquitetura

- As decisões de arquitetura e seu histórico estão indexados nos [Architecture Decision Records](docs/adr/README.md).
- Java 21 e Spring Boot 3.5.16.
- Maven com Maven Wrapper.
- Spring Web MVC, Bean Validation, Spring Data JPA com Hibernate, MapStruct e
  Spring Security.
- PostgreSQL 17.6 com migrações Flyway imutáveis. A V1 cria contas e
  movimentações, a V2 adiciona o estado de idempotência das transferências e o
  outbox histórico, e a V3 remove a tabela de outbox substituída.
- RabbitMQ 4.1.4 para publicação direta de eventos de transferência concluída.
- Spring Boot Actuator e Micrometer com coleta pelo Prometheus e dashboard
  Grafana provisionado para observabilidade local.
- Springdoc OpenAPI e Swagger UI para documentação REST interativa gerada.
- Docker e Docker Compose para ambientes locais reproduzíveis.
- Monorepo em camadas com limites entre API, mapper, service, repository,
  entity, DTO e configuration.
- `BigDecimal` e tipos de precisão fixa no banco para valores monetários.
- Uma única transação `READ_COMMITTED` com bloqueio pessimista das contas em
  cada transferência.
- Pelo menos 90% de cobertura unitária da lógica relevante, além de testes
  isolados, integrados, de concorrência, migração e carga com Gatling.

Os registros de decisão possuem diferentes estados. Os registros aceitos
governam idempotência, acesso temporário sem autenticação, falha limitada por
contenção e notificações diretas best effort; os registros substituídos do
outbox permanecem como histórico.

## Capacidades implementadas

- Criar uma conta com ID numérico gerado, nome do cliente, saldo inicial não
  negativo e instante de criação em UTC.
- Normalizar valores monetários para escala dois com arredondamento `HALF_EVEN`
  e persistir como valores PostgreSQL `NUMERIC(19,2)`.
- Emitir um token UUID de idempotência gerado pelo servidor e válido por 10
  minutos.
- Transferir um valor monetário positivo entre duas contas existentes e
  diferentes.
- Debitar e creditar as duas contas atomicamente, sem criar ou destruir
  dinheiro.
- Reproduzir o resultado estabelecido quando uma transferência concluída for
  repetida com o mesmo token e payload normalizado, sem duplicar efeitos
  financeiros.
- Impedir saldo negativo, reutilização do token com outro payload, atualizações
  perdidas e efeitos financeiros parciais sob requisições concorrentes.
- Registrar cada transferência bem-sucedida como uma movimentação de débito e
  outra de crédito com o mesmo identificador de operação.
- Listar as movimentações de uma conta em páginas fixas de 10, com filtros
  opcionais por intervalo de datas e `CREDIT`/`DEBIT`.
- Bloquear as contas da transferência em ordem crescente de ID dentro de uma
  transação `READ_COMMITTED`, com timeout configurável.
- Solicitar a publicação síncrona best effort de um evento
  `TRANSFER_COMPLETED` no RabbitMQ após o commit de cada nova transferência.
- Expor métricas operacionais de throughput, latência, falhas, timeouts e
  contenção de locks.
- Publicar o contrato REST versionado, os principais exemplos de sucesso e
  validação e uma interface interativa por OpenAPI e Swagger UI.
- Validar migrações, comportamento HTTP/PostgreSQL/RabbitMQ real, concorrência
  e carga sustentada por suítes automatizadas separadas.

## Limitações atuais

- Não existem endpoints para consulta, listagem, atualização ou exclusão de
  contas.
- Cheque especial, tarifas, câmbio, transferências agendadas e operações
  financeiras corretivas estão fora do escopo atual.
- `/api/v1/**` está temporariamente sem autenticação e excluída das verificações
  de CSRF. O serviço não deve ser exposto a uma rede não confiável enquanto
  autenticação e autorização não forem implementadas.
- A entrega pelo RabbitMQ é best effort. Não há outbox, retry durável,
  confirmação do publisher, garantia exactly-once ou recuperação após falha do
  processo ou broker.
- Não há consumer de notificações nem canal de entrega ao cliente.

## API REST

Os endpoints públicos são versionados sob `/api/v1`. Após iniciar a aplicação,
use a documentação interativa canônica para consultar schemas completos de
request e response, exemplos executáveis, casos de validação e contratos de
falha:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

| Método | Caminho | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/accounts` | Criar uma conta |
| `GET` | `/api/v1/accounts/{accountId}/movements` | Listar movimentações paginadas e opcionalmente filtradas |
| `POST` | `/api/v1/transfer-tokens` | Emitir um token de idempotência válido por 10 minutos |
| `POST` | `/api/v1/transfers` | Criar ou reproduzir idempotentemente uma transferência entre contas |

O Swagger UI é a fonte dos principais exemplos de sucesso, resultado vazio,
validação de transporte, conflito de negócio, recurso inexistente e falha
temporária. As falhas esperadas utilizam Problem Details seguros conforme RFC
9457.

### Notificações de transferência

Cada nova transferência concluída solicita a publicação de um evento contendo
ID único do evento, ID da operação de transferência, ID da conta destinatária,
tipo `TRANSFER_COMPLETED`, valor normalizado e instante de ocorrência.

Topologia RabbitMQ:

- Exchange: `banking.transfer.notifications`
- Queue: `banking.transfer.notifications.completed`
- Routing key: `transfer.completed`

Repetições idênticas da transferência não solicitam outra publicação. Após o
commit do PostgreSQL liberar os locks financeiros, a publicação é executada
sincronamente com limites de conexão, handshake, RPC de canal, quantidade de
tentativas e duração total. O esgotamento das tentativas não invalida a
transferência já confirmada. O evento ainda pode ser perdido se o processo
parar após o commit ou duplicado pelo RabbitMQ ou pela rede.

## Documentação

- [Mapa da documentação](docs/README.md)
- [Épico do schema principal do banco](docs/epics/EPIC000-core-database-schema.md)
- [Relatório de execução do desenvolvimento](docs/epics/execution-report.md)
- [Épico de criação de contas](docs/epics/EPIC001-account-creation.md)
- [Épico de transferência entre contas](docs/epics/EPIC002-account-to-account-transfer.md)
- [Épico de simplificação dos testes funcionais](docs/epics/EPIC003-functional-test-suite-simplification.md)
- [Épico de listagem de movimentações](docs/epics/EPIC004-account-movement-listing.md)
- [Business Decision Records](docs/bdr/README.md)
- [Architecture Decision Records](docs/adr/README.md)
- [Modelo lógico de dados](docs/database/logical-data-model.md)
- [Padrões de engenharia](docs/engineering/README.md)
- [Workflows de entrega assistida por IA](.agents/workflows/README.md)

As instruções gerais de contribuição do repositório estão definidas no
[AGENTS.md](AGENTS.md).

## Desenvolvimento

### Pré-requisitos

- Docker com Docker Compose para executar o produto local completo.
- Java 21 ou posterior somente para desenvolvimento e testes executados
  diretamente no host.

O Maven Wrapper baixa a versão do Maven configurada no repositório, portanto
não é necessário instalar Maven no host.

### Build e testes

```bash
./mvnw -B -ntp test
./mvnw -B -ntp verify
```

Os testes unitários ficam em `src/test/unit/java`. Os testes funcionais isolados
ficam em `src/test/isolated/java` e integram o ciclo normal de `verify`. O
`verify` padrão executa cenários completos da aplicação contra PostgreSQL
Testcontainers descartáveis e exige pelo menos 90% de cobertura de linhas
elegíveis. Assim, Docker é obrigatório para `verify`; `test` permanece local ao
processo e não requer Docker.

O source set integrado é opt-in e adiciona exatamente um cenário focado de
compatibilidade contra um RabbitMQ Testcontainer descartável. Ele exercita o
publisher real, topologia AMQP, roteamento, conversão JSON e consumo sem iniciar
PostgreSQL nem executar transferência financeira:

```bash
./mvnw -B -ntp -Pintegrated-functional-tests verify
```

Os testes isolados apoiados por PostgreSQL verificam que seu datasource é
exatamente o container pertencente ao teste antes da execução. Nenhum teste
funcional limpa tabelas do banco; os containers são descartados por inteiro.
Docker deve estar disponível para ambos os comandos `verify`.

### Executar testes de carga Gatling

Com o ambiente dedicado de testes de carga configurado pelas variáveis
`TRANSFER_LOAD_*` do `.env.example`, execute:

```bash
./mvnw -B -ntp -Pload-tests \
  -Dtransfer.load.rate=10 \
  -Dtransfer.load.duration-seconds=30 \
  gatling:test
```

`transfer.load.rate` define a quantidade de transferências iniciadas por
segundo. A carga planejada é `taxa × duração`; o exemplo acima executa
aproximadamente 300 transferências. Altere qualquer um dos parâmetros Maven
para ajustar a carga sem editar o código da simulação.

### Executar o produto local completo

Construa a aplicação e inicie-a com PostgreSQL, RabbitMQ, Prometheus e Grafana.
Não é necessário ter Java ou Maven instalados no host:

```bash
docker compose up --build --wait
```

Quando todos os serviços estiverem saudáveis, estas interfaces locais estarão
disponíveis:

| Interface | URL | Acesso |
| --- | --- | --- |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Documentação pública local da API |
| OpenAPI JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Contrato público local da API |
| Dashboard Grafana | [http://localhost:3000/d/simplified-banking/simplified-banking-service](http://localhost:3000/d/simplified-banking/simplified-banking-service) | Anônimo, somente leitura |
| Prometheus UI | [http://localhost:9090](http://localhost:9090) | Consultas de métricas limitadas ao loopback |
| Administração RabbitMQ | [http://localhost:15672](http://localhost:15672) | Credenciais do `.env.example` |

### Dashboard do Grafana

O datasource e o dashboard do Grafana são provisionados automaticamente.
Exercite as APIs pelo Swagger e atualize o Grafana para visualizar métricas de
requisições, resultados, latência, banco, locks, JVM, CPU e pool de conexões.
Clique na prévia para abrir o dashboard local interativo.

[![Prévia do dashboard Grafana](docs/assets/grafana-dashboard.png)](http://localhost:3000/d/simplified-banking/simplified-banking-service)

### Gerar dados de demonstração para o dashboard

Copie o prompt abaixo em um agente de programação com IA enquanto a aplicação
estiver em execução:

```text
Usando as APIs documentadas no Swagger em
http://localhost:8080/swagger-ui.html, crie 10 novas contas com saldo inicial
de 10000.00 e depois crie 50 transferências bem-sucedidas distribuídas entre
essas contas. Gere um novo token de idempotência para cada transferência,
aguarde 2 segundos após cada request, não exclua nem altere dados existentes e,
ao final, informe os IDs das contas criadas, a quantidade de transferências e
o total de movimentações.
```

Encerre a topologia completa sem excluir os volumes nomeados:

```bash
docker compose down
```

Para sobrescrever portas locais, retenção ou credenciais de desenvolvimento,
copie `.env.example` para `.env` antes de iniciar e edite somente os valores
desejados. O Docker Compose carrega o `.env` automaticamente.

### Inicialização opcional da aplicação diretamente no host

Para um ciclo mais rápido de edição e reinicialização da aplicação, inicie
somente suas dependências:

```bash
docker compose up -d --wait postgres rabbitmq
./mvnw spring-boot:run
```

As portas e credenciais da infraestrutura local podem ser sobrescritas pelas
variáveis documentadas no `.env.example`. O Docker Compose carrega um arquivo
`.env` copiado automaticamente; as variáveis da aplicação devem ser exportadas
ou passadas explicitamente para o processo da aplicação.

### Configuração

Configurações de runtime da aplicação:

| Variável | Padrão | Finalidade |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Porta HTTP do servidor |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/simplified_banking` | URL de conexão JDBC |
| `DATABASE_USERNAME` | `simplified_banking` | Usuário do banco da aplicação |
| `DATABASE_PASSWORD` | `simplified_banking` | Senha do banco da aplicação |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta AMQP do RabbitMQ |
| `RABBITMQ_USERNAME` | `simplified_banking` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `simplified_banking` | Senha do RabbitMQ |
| `TRANSFER_LOCK_TIMEOUT_MS` | `5000` | Timeout positivo do lock PostgreSQL local à transação |
| `TRANSFER_NOTIFICATION_MAX_ATTEMPTS` | `3` | Máximo de tentativas de publicação no RabbitMQ após o commit |
| `TRANSFER_NOTIFICATION_MAX_DURATION` | `3s` | Orçamento monotônico total para retry da publicação |
| `TRANSFER_NOTIFICATION_CONNECTION_TIMEOUT` | `1s` | Timeout da conexão TCP com RabbitMQ |
| `TRANSFER_NOTIFICATION_HANDSHAKE_TIMEOUT` | `1s` | Timeout do handshake AMQP com RabbitMQ |
| `TRANSFER_NOTIFICATION_CHANNEL_RPC_TIMEOUT` | `1s` | Timeout do RPC de canal do RabbitMQ |
| `PROMETHEUS_HOST` | `127.0.0.1` | Interface do host para a UI local do Prometheus |
| `PROMETHEUS_PORT` | `9090` | Porta do host para a UI local do Prometheus |
| `PROMETHEUS_RETENTION` | `7d` | Retenção local das séries temporais do Prometheus |
| `GRAFANA_HOST` | `127.0.0.1` | Interface do host para a UI local do Grafana |
| `GRAFANA_PORT` | `3000` | Porta do host para a UI local do Grafana |

A infraestrutura do Compose também aceita `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`, `POSTGRES_PORT` e `RABBITMQ_MANAGEMENT_PORT`. O Gatling
utiliza as variáveis `TRANSFER_LOAD_*` separadas, documentadas no `.env.example`
e no guia de testes de carga acima.

### Observabilidade e segurança

O Actuator expõe `health`, `info`, `metrics` e `prometheus`; as rotas
operacionais permanecem protegidas por padrão. A topologia completa do Compose
habilita o scraping não autenticado do Prometheus somente na porta de
management interna e não publicada da aplicação. A exceção pública temporária
da API se aplica a `/api/v1/**`, `/v3/api-docs/**` e às rotas de documentação do
Swagger UI.

As operações da API usam tags `operation` de cardinalidade limitada
(`account.create`, `movement.list`, `transfer-token.issue` e `transfer.create`)
e publicam os seguintes meters do Micrometer:

- `banking.api.requests.total`
- `banking.api.requests.successful`
- `banking.api.requests.rejected`
- `banking.api.requests.failed`
- `banking.api.database.errors`
- `banking.api.timeouts`
- `banking.api.lock.contention`
- `banking.api.request.latency`

### Construir a imagem da aplicação

```bash
docker build -t simplified-banking-service .
```

A imagem executa com um usuário não root e espera as configurações de conexão
com PostgreSQL e RabbitMQ pelas mesmas variáveis de ambiente utilizadas na
execução local.

## Modelo de branches

- `main` contém releases estáveis e validadas.
- `develop` é a branch de integração.
- Branches de feature são integradas em `develop`; releases validadas são
  promovidas de `develop` para `main`.

## Estado do desenvolvimento

A entrega implementada inclui:

- Flyway V1 para contas/movimentações, V2 para o histórico de idempotência e V3
  para remoção do outbox.
- Criação de contas com validação e normalização monetária.
- Histórico de movimentações somente leitura, com paginação fixa e filtros
  opcionais por data e tipo de movimentação.
- Tokens emitidos pelo servidor e transferências idempotentes, atômicas e com
  bloqueio pessimista.
- Eventos diretos best effort de transferência concluída via RabbitMQ, com
  retry limitado em memória.
- Problem Details seguros, métricas da API com cardinalidade limitada,
  infraestrutura local em Docker Compose e imagem da aplicação sem root.
- Testes unitários, testes funcionais isolados com PostgreSQL, um teste de
  integração focado em RabbitMQ e testes de carga Gatling.

Autenticação e autorização, operações de consulta/atualização/exclusão de
contas e consumers de notificação permanecem como trabalho futuro.

O comando canônico de qualidade local é:

```bash
./mvnw -B -ntp verify
```
