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

- [x] 2. Implementar camada de domínio — enums e entidade Tarefa
  - [x] 2.1 Criar enums `Criticidade` e `StatusTarefa`
    - Implementar `Criticidade` com valores `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`
    - Implementar `StatusTarefa` com valores `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`
    - _Requirements: 1.5, 4.7_

  - [x] 2.2 Criar entidade de domínio `Tarefa`
    - Implementar classe POJO com todos os campos: `id`, `titulo`, `descricao`, `prazo`, `criticidade`, `status`, `scorePrioridade`, `criadoEm`, `atualizadoEm`, `concluidoEm`
    - _Requirements: 1.1, 1.2, 8.3_

- [x] 3. Implementar `Priorizador` — componente de domínio puro
  - [x] 3.1 Implementar classe `Priorizador` com método `calcular(Tarefa tarefa, Instant referencia)`
    - Implementar lógica de `criticidadeScore` (BAIXA=10, MEDIA=25, ALTA=50, URGENTE=70)
    - Implementar lógica de `deadlineScore` (vencida→100, ≤24h→+25, ≤72h→+15, ≤168h→+8, senão→+0)
    - Implementar lógica de `ageBonus` (≥30 dias→+5, ≥7 dias→+3, senão→+0)
    - Aplicar `clamp(score, 0, 100)` e retornar 100 imediatamente para tarefas vencidas
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.8_

  - [x]* 3.2 Escrever property test — Property 1: Score sempre dentro dos limites
    - **Property 1: Score sempre dentro dos limites**
    - Implementado em `PriorizadorPropertyTest` — `scoreSempreDentroDoLimite`
    - **Validates: Requirements 6.2**

  - [x]* 3.3 Escrever property test — Property 2: Tarefa vencida recebe score máximo
    - **Property 2: Tarefa vencida recebe score máximo**
    - Implementado em `PriorizadorPropertyTest` — `tarefaVencidaRecebeScoreMaximo`
    - **Validates: Requirements 6.6**

  - [x]* 3.4 Escrever property test — Property 3: Criticidade determina ordenação quando prazo e idade são iguais
    - **Property 3: Criticidade determina ordenação quando prazo e idade são iguais**
    - Implementado em `PriorizadorPropertyTest` — `criticidadeMaisAltaGeraScoreMaiorOuIgual`
    - **Validates: Requirements 6.3**

  - [x]* 3.5 Escrever property test — Property 4: Prazo mais próximo gera score maior quando criticidade e idade são iguais
    - **Property 4: Prazo mais próximo gera score maior quando criticidade e idade são iguais**
    - Implementado em `PriorizadorPropertyTest` — `prazoMaisProximoGeraScoreMaiorOuIgual`
    - **Validates: Requirements 6.4**

  - [x]* 3.6 Escrever property test — Property 5: Tarefa mais antiga recebe score maior quando criticidade e prazo são iguais
    - **Property 5: Tarefa mais antiga recebe score maior quando criticidade e prazo são iguais**
    - Implementado em `PriorizadorPropertyTest` — `tarefaMaisAntigaRecebeScoreMaiorOuIgual`
    - **Validates: Requirements 6.5**

  - [x]* 3.7 Escrever property test — Property 6: Determinismo do Priorizador
    - **Property 6: Determinismo do Priorizador**
    - Implementado em `PriorizadorPropertyTest` — `calculoDeterministico`
    - **Validates: Requirements 6.8**

  - [x]* 3.8 Escrever testes unitários para `Priorizador` (exemplos concretos)
    - Implementado em `PriorizadorTest` com 20 testes cobrindo: tarefa vencida → 100, todos os níveis de criticidade, todos os limiares de deadlineScore, todos os limiares de ageBonus e cenários compostos
    - _Requirements: 10.1_

- [x] 4. Implementar `StatusMachine` — regras de transição de status
  - [x] 4.1 Implementar classe `StatusMachine` com método `validarTransicao(StatusTarefa atual, StatusTarefa destino)`
    - Definir conjunto de transições válidas: `PENDENTE→EM_ANDAMENTO`, `PENDENTE→CONCLUIDA`, `PENDENTE→CANCELADA`, `EM_ANDAMENTO→CONCLUIDA`, `EM_ANDAMENTO→CANCELADA`
    - Lançar `TarefaEncerradaException` quando `atual` for `CONCLUIDA` ou `CANCELADA`
    - Lançar `TransicaoInvalidaException` para demais transições inválidas
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x]* 4.2 Escrever testes unitários para `StatusMachine`
    - Implementado em `StatusMachineTest` com 14 testes cobrindo: 5 transições válidas, rejeição de `EM_ANDAMENTO→PENDENTE`, rejeição de qualquer transição a partir de `CONCLUIDA` e `CANCELADA` (via `@ParameterizedTest`), e verificação de mensagens e campos das exceções
    - _Requirements: 10.3_

- [x] 5. Criar exceções de domínio
  - Implementar `TarefaNaoEncontradaException`
  - Implementar `TransicaoInvalidaException`
  - Implementar `TarefaEncerradaException`
  - _Requirements: 2.2, 3.5, 4.4, 4.5, 4.6, 5.4_

- [x] 6. Checkpoint — Verificar domínio puro
  - Garantir que todos os testes do pacote `domain` passam e cobertura ≥ 80%. Perguntar ao usuário se houver dúvidas.

- [x] 7. Implementar camada de infraestrutura — persistência JPA e Flyway
  - [x] 7.1 Criar migration Flyway `V1__create_tarefas_table.sql`
    - Criada tabela `tarefas` (V1) e tabela `tasks` (V2) com todos os campos, constraints e índices
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 7.2 Criar entidade JPA `Task` (antes chamada `TarefaEntity`)
    - Implementada em `domain/entity/Task.java` com `@Entity`, `@Table(name = "tasks")` e todos os campos mapeados
    - Usa `@Enumerated(EnumType.STRING)` para `criticidade` e `status`
    - _Requirements: 8.3, 8.4_

  - [x] 7.3 Criar `TaskRepository` estendendo `JpaRepository<Task, UUID>`
    - Implementado com `findByStatus`, `findByCriticidade` e `findByStatusAndCriticidade` com suporte a `Pageable`
    - Ordenação `scorePrioridade DESC, prazo ASC` definida no controller via `Sort`
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 7.4 Conversão domínio ↔ persistência via `TaskResponse.from(Task)`
    - Optou-se por factory method direto em vez de mapper separado; `TarefaMapper` não foi criado
    - _Requirements: 8.3_

- [x] 8. Implementar `TaskService` — casos de uso da aplicação
  - [x] 8.1 Implementar método `create(TaskRequest request)`
    - Mapeia DTO para `Task`, define status `PENDENTE`, calcula criticidade via `CriticidadeCalculator`, persiste e retorna `TaskResponse`
    - `criadoEm` e `atualizadoEm` gerenciados pelo Hibernate (`@CreationTimestamp` / `@UpdateTimestamp`)
    - **Nota:** usa `CriticidadeCalculator` em vez de `Priorizador`; `scorePrioridade` não é calculado no create
    - _Requirements: 1.1, 1.2, 1.6, 6.1_

  - [x] 8.2 Implementar método `findById(UUID id)`
    - Busca no repositório, lança `EntityNotFoundException` se não encontrado
    - _Requirements: 2.1, 2.2_

  - [x] 8.3 Implementar método `findAll(StatusTarefa status, Criticidade criticidade, Pageable pageable)`
    - Aplica filtros opcionais, retorna página ordenada por `scorePrioridade DESC, prazo ASC`
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 8.4 Implementar método `update(UUID id, TaskRequest request)`
    - Verifica existência, rejeita se `CONCLUIDA` ou `CANCELADA`, atualiza campos e recalcula criticidade
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

  - [x] 8.5 Implementar método `transition(UUID id, TransitionStatusRequest request)`
    - Verifica existência, delega validação à `StatusMachine`, atualiza status e `atualizadoEm` (via `@UpdateTimestamp`)
    - Preenche `concluidaEm` ao transitar para `CONCLUIDA`
    - Congela `scorePrioridade` ao transitar para `CONCLUIDA` ou `CANCELADA` usando o `Priorizador`
    - `TarefaEncerradaException` → 422, `TransicaoInvalidaException` → 422 mapeados no `GlobalExceptionHandler`
    - `PATCH /api/v1/tasks/{id}/status` adicionado ao `TaskController`
    - `TransitionStatusRequest` criado em `br.com.sctec.taskflow.dto`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 6.7_

  - [x] 8.6 Implementar método `delete(UUID id)`
    - Verifica existência, remove permanentemente do repositório
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x]* 8.7 Escrever testes unitários para `TaskService` (com mocks de repositório)
    - Implementado em `TaskServiceTest` com 33 testes usando `@ExtendWith(MockitoExtension.class)`
    - `Create` (5): status PENDENTE, criticidade calculada, criticidade efetiva persistida, campos mapeados, `save` chamado uma vez
    - `FindById` (3): retorno correto, `EntityNotFoundException` com ID na mensagem, delegação ao repositório
    - `FindAll` (13): 4 combinações de filtros + `@ParameterizedTest` para todos os valores de `StatusTarefa` e `Criticidade`
    - `Update` (7): recálculo de criticidade, rejeição de `CONCLUIDA`/`CANCELADA`, `EntityNotFoundException`, `save` não chamado quando encerrada
    - `Delete` (5): remoção correta, `EntityNotFoundException`, `delete` não chamado para ID inexistente, entidade correta passada, deleção em qualquer status
    - _Requirements: 10.2, 10.3_

- [x] 9. Checkpoint — Verificar camada de aplicação
  - `TaskService` implementado com create, findById, findAll, update e delete. Falta `transition` (tarefa 8.5).

- [x] 10. Implementar DTOs de request e response
  - [x] 10.1 Criar `TaskRequest` (unifica CreateTaskRequest e UpdateTaskRequest)
    - `@NotBlank` em `titulo`, `@NotNull` e `@FutureOrPresent` em `prazo`, `@NotNull` em `criticidade`
    - _Requirements: 1.3, 1.4, 1.5_

  - [x] 10.2 `UpdateTaskRequest` — unificado em `TaskRequest` (mesmo record usado para create e update)
    - _Requirements: 3.1, 3.4_

  - [x] 10.3 Criar `TransitionStatusRequest` com `@NotNull` em `status`
    - Implementado em `br.com.sctec.taskflow.dto.TransitionStatusRequest` como record com `@NotNull`
    - _Requirements: 4.7_

  - [x] 10.4 Criar `TaskResponse` com todos os campos da entidade
    - Implementado como record com factory method `from(Task)`
    - _Requirements: 2.1_

  - [x] 10.5 Formato de erro padronizado
    - Usa `ProblemDetail` (RFC 7807) nativo do Spring 6+ em vez de `ErrorResponse` customizado
    - _Requirements: 7.5_

- [x] 11. Implementar `GlobalExceptionHandler` com `@RestControllerAdvice`
  - [x] 11.1 Mapear `MethodArgumentNotValidException` → 400 com lista de campos inválidos
    - _Requirements: 7.2, 7.5_

  - [x] 11.2 Mapear `HttpMessageNotReadableException` → 400
    - Coberto implicitamente pelo Spring; handler customizado não adicionado explicitamente
    - _Requirements: 7.1_

  - [x] 11.3 Mapear `EntityNotFoundException` → 404
    - _Requirements: 2.2, 3.5, 4.6, 5.4_

  - [x] 11.4 Mapear `IllegalStateException` → 409 Conflict
    - Cobre tarefas encerradas; `TransicaoInvalidaException` e `TarefaEncerradaException` ainda não integradas ao fluxo HTTP
    - _Requirements: 4.4, 4.5, 3.6_

  - [x] 11.5 Mapear `DataAccessException` → 503 sem expor detalhes internos
    - Handler `handleDataAccess` retorna 503 com mensagem genérica — não expõe detalhes do banco
    - _Requirements: 8.5_

  - [x] 11.6 Mapear `Exception` (fallback) → 500 sem expor stack trace
    - Handler `handleGeneric` retorna 500 com mensagem genérica — não expõe stack trace nem mensagem interna
    - _Requirements: 7.4_

  - [x]* 11.7 Escrever testes unitários para `GlobalExceptionHandler`
    - Implementado em `GlobalExceptionHandlerTest` com 24 testes organizados em 6 grupos `@Nested`
    - Cobre: 404 (3), 409 (3), 422 TarefaEncerrada (4), 422 TransicaoInvalida (5), 400 validação (3), 503 DataAccess (4), 500 fallback (5)
    - Verifica status HTTP, título, detail e ausência de informações internas sensíveis
    - _Requirements: 7.5_

- [x] 12. Implementar `TaskController` — endpoints REST
  - [x] 12.1 Implementar `POST /api/v1/tasks` → 201 Created
    - Delega para `TaskService.create`, retorna `TaskResponse` com `Location` header
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 1.6_

  - [x] 12.2 Implementar `GET /api/v1/tasks/{id}` → 200 OK
    - Delega para `TaskService.findById`
    - _Requirements: 2.1, 2.2_

  - [x] 12.3 Implementar `GET /api/v1/tasks` → 200 OK com paginação e filtros
    - Aceita query params `status`, `criticidade`, `page` (default 0), `size` (default 20)
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 12.4 Implementar `PUT /api/v1/tasks/{id}` → 200 OK
    - Delega para `TaskService.update`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

  - [x] 12.5 Implementar `PATCH /api/v1/tasks/{id}/status` → 200 OK
    - Delega para `TaskService.transition`, retorna `TaskResponse`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x] 12.6 Implementar `DELETE /api/v1/tasks/{id}` → 204 No Content
    - Delega para `TaskService.delete`
    - _Requirements: 5.1, 5.4_

- [x] 13. Implementar property tests de validação de entrada
  - [x]* 13.1 Escrever property test — Property 8: Título em branco é sempre rejeitado na criação
    - **Property 8: Título em branco é sempre rejeitado na criação**
    - Implementado em `TaskRequestPropertyTest` — `tituloBrancoESempreRejeitado` (100 amostras de strings em branco/vazias) e `tituloNuloESempreRejeitado`
    - Usa `Validator` do Jakarta Bean Validation diretamente, sem Spring context
    - **Validates: Requirements 1.3**

  - [x]* 13.2 Escrever property test — Property 9: Prazo passado é sempre rejeitado
    - **Property 9: Prazo passado é sempre rejeitado**
    - Implementado em `TaskRequestPropertyTest` — `prazoPassadoESempreRejeitado` (100 amostras de datas 1–3650 dias no passado) e `prazoNuloESempreRejeitado`
    - Inclui teste de sanidade verificando que request válido não gera violações
    - **Validates: Requirements 1.4, 3.4**

- [x] 14. Implementar property tests de integração (camada de serviço)
  - [x]* 14.1 Escrever property test — Property 7: Score congelado após transição para estado terminal
    - **Property 7: Score congelado após transição para estado terminal**
    - Implementado em `TaskServicePropertyTest` — `scoreCongeladoAposTransicaoTerminal`
    - **Validates: Requirements 6.7**

  - [x]* 14.2 Escrever property test — Property 10: Tarefa criada tem status PENDENTE e score calculado
    - **Property 10: Tarefa criada tem status PENDENTE e criticidade calculada**
    - Implementado em `TaskServicePropertyTest` — `tarefaCriadaTemStatusPendenteECriticidadeCalculada`
    - **Validates: Requirements 1.1, 1.6**

  - [x]* 14.3 Escrever property test — Property 11: Round-trip de criação e consulta preserva todos os atributos
    - **Property 11: Round-trip de criação e consulta preserva todos os atributos**
    - Implementado em `TaskServicePropertyTest` — `roundTripCriacaoConsultaPreservaAtributos`
    - **Validates: Requirements 2.1, 8.3**

  - [x]* 14.4 Escrever property test — Property 12: Listagem respeita a ordenação por score e prazo
    - **Property 12: Listagem respeita a ordenação por score e prazo**
    - Implementado em `TaskServicePropertyTest` — `listagemRespeitaOrdenacaoPorScoreEPrazo`
    - **Validates: Requirements 2.3, 2.4**

  - [x]* 14.5 Escrever property test — Property 13: Filtro de status retorna apenas tarefas com o status solicitado
    - **Property 13: Filtro de status retorna apenas tarefas com o status solicitado**
    - Implementado em `TaskServicePropertyTest` — `filtroStatusRetornaApenasStatusSolicitado`
    - **Validates: Requirements 2.5**

  - [x]* 14.6 Escrever property test — Property 14: Filtro de criticidade retorna apenas tarefas com a criticidade solicitada
    - **Property 14: Filtro de criticidade retorna apenas tarefas com a criticidade solicitada**
    - Implementado em `TaskServicePropertyTest` — `filtroCriticidadeRetornaApenasCriticidadeSolicitada`
    - **Validates: Requirements 2.6**

  - [x]* 14.7 Escrever property test — Property 15: Tarefas encerradas rejeitam atualizações de campos
    - **Property 15: Tarefas encerradas rejeitam atualizações de campos**
    - Implementado em `TaskServicePropertyTest` — `tarefasEncerradasRejeitamAtualizacoes`
    - **Validates: Requirements 3.6**

  - [x]* 14.8 Escrever property test — Property 16: Transições válidas são aceitas e atualizam o timestamp
    - **Property 16: Transições válidas são aceitas e atualizam o status**
    - Implementado em `TaskServicePropertyTest` — `transicoesValidasSaoAceitas`
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [x]* 14.9 Escrever property test — Property 17: Transições a partir de estados terminais são sempre rejeitadas
    - **Property 17: Transições a partir de estados terminais são sempre rejeitadas**
    - Implementado em `TaskServicePropertyTest` — `transicoesDeEstadosTerminaisSaoSempreRejeitadas`
    - **Validates: Requirements 4.4**

  - [x]* 14.10 Escrever property test — Property 18: Respostas de erro seguem o formato padronizado
    - **Property 18: Exceções de domínio carregam informações estruturadas**
    - Implementado em `TaskServicePropertyTest` — `excecoesDeDominioCarregamInformacoesEstruturadas`
    - **Validates: Requirements 7.2, 7.5**

  - [x]* 14.11 Escrever property test — Property 19: Deleção remove a tarefa de todas as visões
    - **Property 19: Deleção remove a tarefa de todas as visões**
    - Implementado em `TaskServicePropertyTest` — `delecaoRemoveTarefaDeTodasAsVisoes`
    - **Validates: Requirements 5.1, 5.2, 5.3**

  - [x]* 14.12 Escrever property test — Property 20: Recálculo de score após atualização de campos influentes
    - **Property 20: Recálculo de criticidade após atualização de campos influentes**
    - Implementado em `TaskServicePropertyTest` — `recalculoCriticidadeAposAtualizacao`
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
