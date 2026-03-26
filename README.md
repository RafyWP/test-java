# Teste Tecnico Backend MaterImperium

API em Java 21 com Spring Boot para upload, processamento assíncrono e consulta de resultado de arquivos.

## Requisitos

- Docker
- O script [`mvn-dev-docker`](/saas/test-java/mvn-dev-docker) incluido no repositorio

## Ambientes

- `docker-compose.dev.yml`: sobe apenas o PostgreSQL para desenvolvimento
- `docker-compose.prod.yml`: sobe PostgreSQL + API empacotada em Docker
- [`mvn-dev-docker`](/saas/test-java/mvn-dev-docker): wrapper para rodar Maven em container no fluxo de desenvolvimento

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

## Producao / Demo

Subindo a stack completa:

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

Se quiser mudar as portas publicadas no host:

```bash
POSTGRES_PORT=5434 APP_PORT=8083 docker compose -f docker-compose.prod.yml up --build -d
```

## Tokens disponiveis

- `token-envio` -> role `ENVIO`
- `token-consulta` -> role `CONSULTA`
- `token-full` -> roles `ENVIO` e `CONSULTA`

## Postman

Arquivos disponiveis:

- collection: [postman/Test-RafyWP.postman_collection.json](/saas/test-java/postman/Test-RafyWP.postman_collection.json)
- environment: [postman/Test-RafyWP-Local.postman_environment.json](/saas/test-java/postman/Test-RafyWP-Local.postman_environment.json)

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

## Observacoes de implementacao

- O upload valida as duas primeiras linhas antes de aceitar o arquivo.
- O arquivo e copiado para diretorio temporario e processado em background.
- A leitura e feita em streaming, linha a linha, evitando carregar arquivos grandes em memoria.
- O historico de cada upload e mantido em banco relacional.
