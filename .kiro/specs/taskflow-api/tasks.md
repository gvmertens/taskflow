# Implementation Plan: TaskFlow API

## Overview

Implementação incremental da TaskFlow API em Java 21 + Spring Boot 3.x + PostgreSQL. O plano segue a arquitetura em camadas definida no design (Presentation → Application → Domain → Infrastructure), começando pela estrutura do projeto e domínio puro, avançando para infraestrutura e persistência, e finalizando com a camada de apresentação e pipeline de CI/CD.

## Tasks

- [x] 1. Configurar estrutura do projeto Maven e dependências
  - Criar `pom.xml` com Java 21, Spring Boot 3.x, dependências de web, validation, data-jpa, postgresql, flyway, springdoc-openapi e jqwik
  - Configurar plugin JaCoCo com regra de cobertura mínima de 80% no pacote `br.com.taskflow.domain`
  - Criar classe principal `TaskFlowApplication.java` com `@SpringBootApplication`
  - Criar `application.properties` com configurações de datasource, JPA e Flyway
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 10.4_

- [ ] 2. Implementar camada de domínio — enums e entidade Tarefa
  - [ ] 2.1 Criar enums `Criticidade` e `StatusTarefa`
    - Implementar `Criticidade` com valores `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`
    - Implementar `StatusTarefa` com valores `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`
    - _Requirements: 1.5, 4.7_

  - [ ] 2.2 Criar entidade de domínio `Tarefa`
    - Implementar classe POJO com todos os campos: `id`, `titulo`, `descricao`, `prazo`, `criticidade`, `status`, `scorePrioridade`, `criadoEm`, `atualizadoEm`, `concluidoEm`
    - _Requirements: 1.1, 1.2, 8.3_

- [ ] 3. Implementar `Priorizador` — componente de domínio puro
  - [ ] 3.1 Implementar classe `Priorizador` com método `calcular(Tarefa tarefa, Instant referencia)`
    - Implementar lógica de `criticidadeScore` (BAIXA=10, MEDIA=25, ALTA=50, URGENTE=70)
    - Implementar lógica de `deadlineScore` (vencida→100, ≤24h→+25, ≤72h→+15, ≤168h→+8, senão→+0)
    - Implementar lógica de `ageBonus` (≥30 dias→+5, ≥7 dias→+3, senão→+0)
    - Aplicar `clamp(score, 0, 100)` e retornar 100 imediatamente para tarefas vencidas
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.8_

  - [ ]* 3.2 Escrever property test — Property 1: Score sempre dentro dos limites
    - **Property 1: Score sempre dentro dos limites**
    - **Validates: Requirements 6.2**

  - [ ]* 3.3 Escrever property test — Property 2: Tarefa vencida recebe score máximo
    - **Property 2: Tarefa vencida recebe score máximo**
    - **Validates: Requirements 6.6**

  - [ ]* 3.4 Escrever property test — Property 3: Criticidade determina ordenação quando prazo e idade são iguais
    - **Property 3: Criticidade determina ordenação quando prazo e idade são iguais**
    - **Validates: Requirements 6.3**

  - [ ]* 3.5 Escrever property test — Property 4: Prazo mais próximo gera score maior quando criticidade e idade são iguais
    - **Property 4: Prazo mais próximo gera score maior quando criticidade e idade são iguais**
    - **Validates: Requirements 6.4**

  - [ ]* 3.6 Escrever property test — Property 5: Tarefa mais antiga recebe score maior quando criticidade e prazo são iguais
    - **Property 5: Tarefa mais antiga recebe score maior quando criticidade e prazo são iguais**
    - **Validates: Requirements 6.5**

  - [ ]* 3.7 Escrever property test — Property 6: Determinismo do Priorizador
    - **Property 6: Determinismo do Priorizador**
    - **Validates: Requirements 6.8**

  - [ ]* 3.8 Escrever testes unitários para `Priorizador` (exemplos concretos)
    - Testar tarefa vencida retorna 100
    - Testar URGENTE com prazo em 1h
    - Testar MEDIA com prazo em 5 dias e 10 dias de idade
    - _Requirements: 10.1_

- [ ] 4. Implementar `StatusMachine` — regras de transição de status
  - [ ] 4.1 Implementar classe `StatusMachine` com método `validarTransicao(StatusTarefa atual, StatusTarefa destino)`
    - Definir conjunto de transições válidas: `PENDENTE→EM_ANDAMENTO`, `PENDENTE→CONCLUIDA`, `PENDENTE→CANCELADA`, `EM_ANDAMENTO→CONCLUIDA`, `EM_ANDAMENTO→CANCELADA`
    - Lançar `TarefaEncerradaException` quando `atual` for `CONCLUIDA` ou `CANCELADA`
    - Lançar `TransicaoInvalidaException` para demais transições inválidas
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 4.2 Escrever testes unitários para `StatusMachine`
    - Testar todas as 5 transições válidas
    - Testar rejeição de `EM_ANDAMENTO→PENDENTE`
    - Testar rejeição de qualquer transição a partir de `CONCLUIDA` e `CANCELADA`
    - _Requirements: 10.3_

- [ ] 5. Criar exceções de domínio
  - Implementar `TarefaNaoEncontradaException`
  - Implementar `TransicaoInvalidaException`
  - Implementar `TarefaEncerradaException`
  - _Requirements: 2.2, 3.5, 4.4, 4.5, 4.6, 5.4_

- [ ] 6. Checkpoint — Verificar domínio puro
  - Garantir que todos os testes do pacote `domain` passam e cobertura ≥ 80%. Perguntar ao usuário se houver dúvidas.

- [ ] 7. Implementar camada de infraestrutura — persistência JPA e Flyway
  - [ ] 7.1 Criar migration Flyway `V1__create_tarefas_table.sql`
    - Criar tabela `tarefas` com todos os campos, constraints e índices conforme o design
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ] 7.2 Criar entidade JPA `TarefaEntity`
    - Anotar com `@Entity`, `@Table(name = "tarefas")` e mapear todos os campos
    - Usar `@Enumerated(EnumType.STRING)` para `criticidade` e `status`
    - _Requirements: 8.3, 8.4_

  - [ ] 7.3 Criar `TaskJpaRepository` estendendo `JpaRepository<TarefaEntity, UUID>`
    - Adicionar método de consulta com filtros opcionais de `status` e `criticidade` e suporte a `Pageable`
    - Definir ordenação padrão `score_prioridade DESC, prazo ASC` via `@Query` ou `Sort`
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7_

  - [ ] 7.4 Criar `TarefaMapper` para conversão entre `Tarefa` (domínio) e `TarefaEntity` (persistência)
    - Implementar métodos `toEntity(Tarefa)` e `toDomain(TarefaEntity)`
    - _Requirements: 8.3_

- [ ] 8. Implementar `TaskService` — casos de uso da aplicação
  - [ ] 8.1 Implementar método `create(CreateTaskRequest request)`
    - Mapear DTO para domínio, definir status `PENDENTE`, calcular score via `Priorizador`, persistir e retornar `TaskResponse`
    - Registrar `criadoEm` e `atualizadoEm` em UTC
    - _Requirements: 1.1, 1.2, 1.6, 6.1_

  - [ ] 8.2 Implementar método `findById(UUID id)`
    - Buscar no repositório, lançar `TarefaNaoEncontradaException` se não encontrado
    - _Requirements: 2.1, 2.2_

  - [ ] 8.3 Implementar método `findAll(StatusTarefa status, Criticidade criticidade, Pageable pageable)`
    - Aplicar filtros opcionais, retornar página ordenada por `score DESC, prazo ASC`
    - Validar tamanho máximo de página = 100
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7_

  - [ ] 8.4 Implementar método `update(UUID id, UpdateTaskRequest request)`
    - Verificar existência, rejeitar se `CONCLUIDA` ou `CANCELADA`, atualizar campos, recalcular score, atualizar `atualizadoEm`
    - Ignorar `scorePrioridade` fornecido pelo cliente
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

  - [ ] 8.5 Implementar método `transition(UUID id, TransitionStatusRequest request)`
    - Verificar existência, delegar validação à `StatusMachine`, atualizar status e `atualizadoEm`
    - Preencher `concluidoEm` ao transitar para `CONCLUIDA`
    - Congelar `scorePrioridade` ao transitar para `CONCLUIDA` ou `CANCELADA`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 6.7_

  - [ ] 8.6 Implementar método `delete(UUID id)`
    - Verificar existência, remover permanentemente do repositório
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 8.7 Escrever testes unitários para `TaskService` (com mocks de repositório)
    - Testar `create`: status PENDENTE, score calculado, timestamps UTC (Property 10)
    - Testar `findById`: retorno correto e exceção para ID inexistente
    - Testar `findAll`: ordenação e filtros (Properties 12, 13, 14)
    - Testar `update`: recálculo de score, rejeição de tarefa encerrada (Properties 15, 20)
    - Testar `transition`: congelamento de score (Property 7), round-trip (Property 11)
    - Testar `delete`: remoção de todas as visões (Property 19)
    - _Requirements: 10.2, 10.3_

- [ ] 9. Checkpoint — Verificar camada de aplicação
  - Garantir que todos os testes de `TaskService` passam. Perguntar ao usuário se houver dúvidas.

- [ ] 10. Implementar DTOs de request e response
  - [ ] 10.1 Criar `CreateTaskRequest` com anotações de validação Bean Validation
    - `@NotBlank` em `titulo`, `@NotNull` e `@Future` em `prazo`, `@NotNull` em `criticidade`
    - _Requirements: 1.3, 1.4, 1.5_

  - [ ] 10.2 Criar `UpdateTaskRequest` com campos opcionais e validações condicionais
    - `@NotBlank` em `titulo` se presente, `@Future` em `prazo` se presente
    - _Requirements: 3.1, 3.4_

  - [ ] 10.3 Criar `TransitionStatusRequest` com `@NotNull` em `status`
    - _Requirements: 4.7_

  - [ ] 10.4 Criar `TaskResponse` com todos os campos da entidade de domínio
    - _Requirements: 2.1_

  - [ ] 10.5 Criar `ErrorResponse` com campos `codigo`, `mensagem`, `timestamp` e `detalhes`
    - _Requirements: 7.5_

- [ ] 11. Implementar `GlobalExceptionHandler` com `@ControllerAdvice`
  - [ ] 11.1 Mapear `MethodArgumentNotValidException` → 400 com lista de campos inválidos
    - _Requirements: 7.2, 7.5_

  - [ ] 11.2 Mapear `HttpMessageNotReadableException` → 400 com mensagem descritiva
    - _Requirements: 7.1_

  - [ ] 11.3 Mapear `TarefaNaoEncontradaException` → 404
    - _Requirements: 2.2, 3.5, 4.6, 5.4_

  - [ ] 11.4 Mapear `TransicaoInvalidaException` e `TarefaEncerradaException` → 422
    - _Requirements: 4.4, 4.5, 3.6_

  - [ ] 11.5 Mapear `DataAccessException` → 503 sem expor detalhes internos
    - _Requirements: 8.5_

  - [ ] 11.6 Mapear `Exception` (fallback) → 500 sem expor stack trace
    - _Requirements: 7.4_

  - [ ]* 11.7 Escrever testes unitários para `GlobalExceptionHandler`
    - Verificar formato padronizado de `ErrorResponse` para cada tipo de exceção (Property 18)
    - _Requirements: 7.5_

- [ ] 12. Implementar `TaskController` — endpoints REST
  - [ ] 12.1 Implementar `POST /api/v1/tasks` → 201 Created
    - Delegar para `TaskService.create`, retornar `TaskResponse` com `Location` header
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 1.6_

  - [ ] 12.2 Implementar `GET /api/v1/tasks/{id}` → 200 OK
    - Delegar para `TaskService.findById`
    - _Requirements: 2.1, 2.2_

  - [ ] 12.3 Implementar `GET /api/v1/tasks` → 200 OK com paginação e filtros
    - Aceitar query params `status`, `criticidade`, `page` (default 0), `size` (default 20, max 100)
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7_

  - [ ] 12.4 Implementar `PUT /api/v1/tasks/{id}` → 200 OK
    - Delegar para `TaskService.update`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

  - [ ] 12.5 Implementar `PATCH /api/v1/tasks/{id}/status` → 200 OK
    - Delegar para `TaskService.transition`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [ ] 12.6 Implementar `DELETE /api/v1/tasks/{id}` → 204 No Content
    - Delegar para `TaskService.delete`
    - _Requirements: 5.1, 5.4_

- [ ] 13. Implementar property tests de validação de entrada
  - [ ]* 13.1 Escrever property test — Property 8: Título em branco é sempre rejeitado na criação
    - **Property 8: Título em branco é sempre rejeitado na criação**
    - **Validates: Requirements 1.3**

  - [ ]* 13.2 Escrever property test — Property 9: Prazo passado é sempre rejeitado
    - **Property 9: Prazo passado é sempre rejeitado**
    - **Validates: Requirements 1.4, 3.4**

- [ ] 14. Implementar property tests de integração (camada de serviço)
  - [ ]* 14.1 Escrever property test — Property 7: Score congelado após transição para estado terminal
    - **Property 7: Score congelado após transição para estado terminal**
    - **Validates: Requirements 6.7**

  - [ ]* 14.2 Escrever property test — Property 10: Tarefa criada tem status PENDENTE e score calculado
    - **Property 10: Tarefa criada tem status PENDENTE e score calculado**
    - **Validates: Requirements 1.1, 1.6**

  - [ ]* 14.3 Escrever property test — Property 11: Round-trip de criação e consulta preserva todos os atributos
    - **Property 11: Round-trip de criação e consulta preserva todos os atributos**
    - **Validates: Requirements 2.1, 8.3**

  - [ ]* 14.4 Escrever property test — Property 12: Listagem respeita a ordenação por score e prazo
    - **Property 12: Listagem respeita a ordenação por score e prazo**
    - **Validates: Requirements 2.3, 2.4**

  - [ ]* 14.5 Escrever property test — Property 13: Filtro de status retorna apenas tarefas com o status solicitado
    - **Property 13: Filtro de status retorna apenas tarefas com o status solicitado**
    - **Validates: Requirements 2.5**

  - [ ]* 14.6 Escrever property test — Property 14: Filtro de criticidade retorna apenas tarefas com a criticidade solicitada
    - **Property 14: Filtro de criticidade retorna apenas tarefas com a criticidade solicitada**
    - **Validates: Requirements 2.6**

  - [ ]* 14.7 Escrever property test — Property 15: Tarefas encerradas rejeitam atualizações de campos
    - **Property 15: Tarefas encerradas rejeitam atualizações de campos**
    - **Validates: Requirements 3.6**

  - [ ]* 14.8 Escrever property test — Property 16: Transições válidas são aceitas e atualizam o timestamp
    - **Property 16: Transições válidas são aceitas e atualizam o timestamp**
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [ ]* 14.9 Escrever property test — Property 17: Transições a partir de estados terminais são sempre rejeitadas
    - **Property 17: Transições a partir de estados terminais são sempre rejeitadas**
    - **Validates: Requirements 4.4**

  - [ ]* 14.10 Escrever property test — Property 18: Respostas de erro seguem o formato padronizado
    - **Property 18: Respostas de erro seguem o formato padronizado**
    - **Validates: Requirements 7.2, 7.5**

  - [ ]* 14.11 Escrever property test — Property 19: Deleção remove a tarefa de todas as visões
    - **Property 19: Deleção remove a tarefa de todas as visões**
    - **Validates: Requirements 5.1, 5.2, 5.3**

  - [ ]* 14.12 Escrever property test — Property 20: Recálculo de score após atualização de campos influentes
    - **Property 20: Recálculo de score após atualização de campos influentes**
    - **Validates: Requirements 3.2**

- [ ] 15. Configurar OpenAPI e Swagger UI
  - [ ] 15.1 Criar `OpenApiConfig.java` com metadados da API (título, versão, descrição)
    - Anotar endpoints do `TaskController` com `@Operation`, `@ApiResponse` e `@Parameter`
    - _Requirements: 12.1, 12.3_

- [ ] 16. Configurar pipeline de CI/CD com GitHub Actions
  - [ ] 16.1 Criar `.github/workflows/ci.yml`
    - Configurar trigger em push para `main` e pull requests para `main`
    - Configurar serviço PostgreSQL 14 no job
    - Definir etapas: checkout → setup-java 21 (temurin) → `mvn --batch-mode verify` → upload do relatório JaCoCo → deploy na `main`
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 9.5, 9.6, 10.5_

- [ ] 17. Checkpoint final — Garantir qualidade e integração completa
  - Executar `mvn verify` localmente e garantir que todos os testes passam e cobertura ≥ 80% no pacote `domain`. Perguntar ao usuário se houver dúvidas.

## Notes

- Tarefas marcadas com `*` são opcionais e podem ser puladas para um MVP mais rápido
- Cada tarefa referencia requisitos específicos para rastreabilidade
- Os checkpoints garantem validação incremental a cada fase
- Os property tests usam **jqwik 1.8.x** com `@Property(tries = 100)` e integração nativa com JUnit 5
- Os testes unitários usam JUnit 5 + Mockito
- A cobertura mínima de 80% é verificada pelo JaCoCo no pacote `br.com.taskflow.domain`
- O `Priorizador` e a `StatusMachine` são POJOs puros sem dependência de Spring ou JPA — testáveis de forma isolada

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "4.1", "5.1"] },
    { "id": 3, "tasks": ["3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8", "4.2", "7.1"] },
    { "id": 4, "tasks": ["7.2", "7.3", "7.4"] },
    { "id": 5, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6", "10.1", "10.2", "10.3", "10.4", "10.5"] },
    { "id": 6, "tasks": ["8.7", "11.1", "11.2", "11.3", "11.4", "11.5", "11.6", "13.1", "13.2", "14.1", "14.2", "14.3", "14.4", "14.5", "14.6", "14.7", "14.8", "14.9", "14.10", "14.11", "14.12"] },
    { "id": 7, "tasks": ["11.7", "12.1", "12.2", "12.3", "12.4", "12.5", "12.6"] },
    { "id": 8, "tasks": ["15.1"] },
    { "id": 9, "tasks": ["16.1"] }
  ]
}
```
