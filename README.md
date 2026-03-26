# Teste Tecnico Backend MaterImperium

API em Java 21 com Spring Boot para upload, processamento assíncrono e consulta de resultado de arquivos.

## Objetivo

Esta aplicacao implementa o fluxo pedido no teste tecnico:

- receber upload de arquivo texto
- validar cabecalho antes do processamento
- processar o arquivo em background
- persistir historico e resumo em banco relacional
- permitir consulta de status e resultado com controle de acesso por role

## Requisitos

- Docker
- O script [`mvn-dev-docker`](mvn-dev-docker) incluido no repositorio

## Execucao Rapida

Fluxo mais curto para validar a API localmente:

```bash
docker compose -f docker-compose.dev.yml up -d
./mvn-dev-docker spring-boot:run
```

Depois disso:

- API: `http://localhost:8082`
- banco: `localhost:5432`
- arquivo de exemplo para upload manual: [`exemplo-registros.txt`](exemplo-registros.txt)

## Ambientes

- `docker-compose.dev.yml`: sobe apenas o PostgreSQL para desenvolvimento
- `docker-compose.prod.yml`: sobe PostgreSQL + API empacotada em Docker
- [`mvn-dev-docker`](mvn-dev-docker): wrapper para rodar Maven em container no fluxo de desenvolvimento

## Stack Tecnica

- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- H2 para testes automatizados
- Docker e Docker Compose

## Desenvolvimento

Subindo o banco:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Se a porta `5432` ja estiver ocupada no host:

```bash
POSTGRES_PORT=5434 docker compose -f docker-compose.dev.yml up -d
```

Rodando a aplicacao:

```bash
./mvn-dev-docker spring-boot:run
```

Esse comando publica a porta da API no host. Por padrao:

- container: `8082`
- host: `8082`

Se estiver rodando o Postgres do projeto em outra porta, a aplicacao precisa apontar para o host:

```bash
DB_URL=jdbc:postgresql://host.docker.internal:5434/materimperium ./mvn-dev-docker spring-boot:run
```

Por padrao a API sobe na porta `8082`.

Se quiser mudar a porta publicada no host:

```bash
APP_PORT=8083 ./mvn-dev-docker spring-boot:run
```

Rodando a suite de testes:

```bash
./mvn-dev-docker test
```

## Producao / Demo

Subindo a stack completa:

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

Se quiser mudar as portas publicadas no host:

```bash
POSTGRES_PORT=5434 APP_PORT=8083 docker compose -f docker-compose.prod.yml up --build -d
```

## Configuracao

Variaveis mais relevantes:

- `DB_URL`: URL JDBC do PostgreSQL. Default: `jdbc:postgresql://localhost:5432/materimperium`
- `DB_USERNAME`: usuario do banco. Default: `materimperium`
- `DB_PASSWORD`: senha do banco. Default: `materimperium`
- `SERVER_PORT`: porta interna da aplicacao. Default: `8082`
- `APP_PORT`: porta publicada no host ao usar `mvn-dev-docker` ou `docker-compose.prod.yml`. Default: `8082`
- `APP_TEMP_DIR`: diretorio temporario para armazenar uploads antes do processamento assincrono. Default: `/tmp/materimperium`

## Tokens disponiveis

- `token-envio` -> role `ENVIO`
- `token-consulta` -> role `CONSULTA`
- `token-full` -> roles `ENVIO` e `CONSULTA`

## Arquitetura

O projeto foi mantido propositalmente simples para priorizar clareza e aderencia ao escopo do teste.

- `controller`: exposicao dos endpoints REST
- `service`: validacao, orquestracao de upload, processamento e consulta
- `repository`: persistencia com Spring Data JPA
- `domain`: entidades e enum de status
- `security`: autenticacao Bearer simples com tokens fixos em memoria
- `exception`: padronizacao das respostas de erro

Fluxo resumido:

1. o upload recebe um `multipart/form-data`
2. as duas primeiras linhas do arquivo sao validadas
3. o arquivo e salvo temporariamente
4. um registro de processamento e criado com status `EM_PROCESSAMENTO`
5. o processamento e executado em background
6. o resumo por codigo de registro e persistido no banco
7. o status final fica disponivel para consulta

## Postman

Arquivos disponiveis:

- collection: [postman/Test-RafyWP.postman_collection.json](postman/Test-RafyWP.postman_collection.json)
- environment: [postman/Test-RafyWP-Local.postman_environment.json](postman/Test-RafyWP-Local.postman_environment.json)

Como usar:

1. Importe a collection e o environment no Postman.
2. Selecione o environment `Test-RafyWP Local`.
3. Ajuste `baseUrl` se a API nao estiver em `http://localhost:8082`.
4. No request `Upload valido`, escolha um arquivo manualmente no campo `file`.
5. Execute o upload. O `processingId` sera salvo automaticamente para uso nos requests seguintes.

## Endpoints

### Upload

```bash
curl -X POST http://localhost:8082/api/uploads \
  -H "Authorization: Bearer token-envio" \
  -F "file=@/caminho/arquivo.txt"
```

Resposta esperada:

- `202 Accepted`
- body:

```json
{
  "id": "uuid-do-processamento"
}
```

### Consulta de status

```bash
curl http://localhost:8082/api/uploads/{id}/status \
  -H "Authorization: Bearer token-consulta"
```

Resposta esperada:

- `200 OK`
- body:

```json
{
  "id": "uuid-do-processamento",
  "status": "EM_PROCESSAMENTO"
}
```

### Consulta de resultado

```bash
curl http://localhost:8082/api/uploads/{id}/resultado \
  -H "Authorization: Bearer token-consulta"
```

Se finalizado com sucesso:

- `200 OK`

```json
{
  "status": "FINALIZADO_COM_SUCESSO",
  "resumo": [
    { "registro": "0000", "total": 1 },
    { "registro": "0001", "total": 1 },
    { "registro": "C170", "total": 2 }
  ]
}
```

Se ainda estiver processando:

- `409 Conflict`

```json
{
  "id": null,
  "status": "EM_PROCESSAMENTO"
}
```

## Arquivo de entrada

Regras de validacao inicial:

- a primeira linha deve comecar com `|0000|017|` ou `|0000|006|`
- a segunda linha deve ser exatamente `|0001|0|`

Exemplo valido:

```text
|0000|017|...
|0001|0|
|C100|...
|C170|...
```

## Regras de acesso

- `POST /api/uploads`: requer token com role `ENVIO`
- `GET /api/uploads/{id}/status`: requer token com role `ENVIO` ou `CONSULTA`
- `GET /api/uploads/{id}/resultado`: requer token com role `CONSULTA`

## Erros comuns

- `400 Bad Request`: arquivo vazio, arquivo ausente, cabecalho invalido ou `id` mal formatado
- `401 Unauthorized`: token ausente ou invalido
- `403 Forbidden`: token sem role suficiente para a rota
- `404 Not Found`: upload inexistente
- `500 Internal Server Error`: falha ao armazenar o arquivo temporario ou erro interno inesperado

## Decisoes Tecnicas

- autenticacao simplificada por token fixo
  O enunciado nao exige armazenamento seguro real de token, apenas validacao de roles.

- processamento assincrono dentro da aplicacao
  Foi escolhida uma abordagem sem fila externa para manter o escopo objetivo e demonstrar o fluxo de background sem dependencias adicionais.

- leitura em streaming
  O arquivo e lido linha a linha com `BufferedReader`, evitando carregar todo o conteudo em memoria.

- persistencia do historico por upload
  Cada envio gera um novo `ArquivoProcessamento`, preservando historico e permitindo multiplos resumos independentes.

- arquivo temporario em disco
  O arquivo recebido e copiado para um diretorio temporario antes do processamento assincrono. Isso desacopla o ciclo de vida da requisicao HTTP do processamento em background.

## Trade-offs

- memoria
  A solucao privilegia baixo consumo de memoria, porque o processamento e streaming.

- disco
  O uso de arquivo temporario nao e o minimo absoluto de disco, mas foi uma escolha pragmatica para garantir confiabilidade no processamento assincrono.

- complexidade
  Nao foram adicionados componentes como mensageria, cache ou object storage para evitar overengineering no contexto do teste.

## Validacao Realizada

- fluxo manual de upload, status e resultado validado localmente
- testes automatizados para fluxo feliz
- testes automatizados para autenticacao e autorizacao
- testes automatizados para validacao de cabecalho
- testes automatizados para erros operacionais
- testes de contrato para validar o formato exato das respostas JSON

Resumo atual da suite:

- `23 testes`
- `0 failures`
- `0 errors`

## Estrutura do Projeto

Arquivos principais para avaliacao rapida:

- [`src/main/java/com/materimperium/backendtest/controller/UploadController.java`](src/main/java/com/materimperium/backendtest/controller/UploadController.java)
- [`src/main/java/com/materimperium/backendtest/service/UploadService.java`](src/main/java/com/materimperium/backendtest/service/UploadService.java)
- [`src/main/java/com/materimperium/backendtest/service/ProcessamentoAsyncService.java`](src/main/java/com/materimperium/backendtest/service/ProcessamentoAsyncService.java)
- [`src/main/java/com/materimperium/backendtest/config/SecurityConfig.java`](src/main/java/com/materimperium/backendtest/config/SecurityConfig.java)
- [`src/main/java/com/materimperium/backendtest/exception/GlobalExceptionHandler.java`](src/main/java/com/materimperium/backendtest/exception/GlobalExceptionHandler.java)
- [`src/test/java/com/materimperium/backendtest/UploadApiContractTest.java`](src/test/java/com/materimperium/backendtest/UploadApiContractTest.java)

## Estrutura de Dados

Principais entidades:

- `ArquivoProcessamento`
  Armazena identificador, nome do arquivo, status, mensagem de erro e timestamps do ciclo de processamento.

- `ResumoRegistro`
  Armazena o total de ocorrencias por codigo de registro para cada upload processado.

## Observacoes de implementacao

- O upload valida as duas primeiras linhas antes de aceitar o arquivo.
- O arquivo e copiado para diretorio temporario e processado em background.
- A leitura e feita em streaming, linha a linha, evitando carregar arquivos grandes em memoria.
- O historico de cada upload e mantido em banco relacional.

## Evolucao Para Producao

Se a aplicacao precisasse evoluir para um ambiente produtivo com volume maior, os proximos passos mais naturais seriam:

- mover o arquivo temporario para storage externo, como S3
- desacoplar o processamento usando fila, como SQS ou Kafka
- adicionar observabilidade com correlation id, metricas e logs estruturados
- usar autenticacao real com JWT ou provedor de identidade
- adicionar migrations versionadas de banco
- reforcar health checks e readiness checks
