# Requirements Document

## Introduction

A TaskFlow_API é uma API REST para gerenciamento de tarefas com priorização automática. O sistema permite que clientes criem, consultem, atualizem, concluam e removam tarefas, e calcula automaticamente um score de prioridade para cada tarefa ativa com base em fatores objetivos (prazo, criticidade declarada e idade da tarefa). A prioridade nunca é informada manualmente pelo cliente; é sempre derivada pelo próprio sistema.

A solução faz parte de um produto educacional/profissional que adota Java 21, Spring Boot 3.x, PostgreSQL e Maven, com exigências de qualidade que incluem testes unitários e pipeline de CI/CD.

Este documento captura os requisitos funcionais (gerenciamento de tarefas, regras de priorização, validação e tratamento de erros) e os requisitos não-funcionais (persistência, stack tecnológica, qualidade e entrega contínua).

## Glossário

- **TaskFlow_API**: Sistema (API REST) responsável pelo gerenciamento de tarefas com priorização automática.
- **Cliente_API**: Aplicação ou usuário que consome a TaskFlow_API por meio de requisições HTTP.
- **Tarefa**: Entidade de domínio que representa uma unidade de trabalho a ser gerenciada. Possui, no mínimo, identificador, título, descrição opcional, prazo (data e hora limite), criticidade declarada, status e timestamps de criação e atualização.
- **Status_Tarefa**: Estado da Tarefa. Valores válidos: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`.
- **Criticidade**: Importância declarada pelo Cliente_API no momento de criação ou atualização da Tarefa. Valores válidos: `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`.
- **Score_Prioridade**: Valor numérico inteiro, entre 0 e 100 inclusive, calculado pela TaskFlow_API para cada Tarefa ativa. Quanto maior o valor, maior a prioridade.
- **Priorizador**: Componente lógico da TaskFlow_API responsável por calcular o Score_Prioridade.
- **Tarefa_Ativa**: Tarefa cujo Status_Tarefa é `PENDENTE` ou `EM_ANDAMENTO`.
- **Tarefa_Encerrada**: Tarefa cujo Status_Tarefa é `CONCLUIDA` ou `CANCELADA`.
- **Pipeline_CI_CD**: Conjunto de etapas automatizadas de integração e entrega contínuas executadas a cada alteração no repositório de código.
- **Ambiente_Persistencia**: Banco de dados relacional PostgreSQL utilizado pela TaskFlow_API para armazenar Tarefas.

## Requirements

### Requirement 1: Criação de Tarefas

**User Story:** Como Cliente_API, quero criar novas Tarefas informando título, descrição, prazo e criticidade, para que o sistema passe a gerenciá-las e a priorizá-las automaticamente.

#### Acceptance Criteria

1. WHEN o Cliente_API envia uma requisição de criação contendo título não vazio, prazo e criticidade válida, THE TaskFlow_API SHALL persistir uma nova Tarefa com Status_Tarefa igual a `PENDENTE` e retornar o identificador gerado e o Score_Prioridade calculado.
2. WHEN uma Tarefa é criada com sucesso, THE TaskFlow_API SHALL registrar timestamps de criação e atualização em UTC.
3. IF o título da Tarefa estiver ausente ou for uma cadeia vazia após remoção de espaços, THEN THE TaskFlow_API SHALL rejeitar a requisição com erro de validação e não persistir a Tarefa.
4. IF o prazo informado for anterior ao instante atual no momento da criação, THEN THE TaskFlow_API SHALL rejeitar a requisição com erro de validação e não persistir a Tarefa.
5. IF a Criticidade informada não pertencer ao conjunto `{BAIXA, MEDIA, ALTA, URGENTE}`, THEN THE TaskFlow_API SHALL rejeitar a requisição com erro de validação e não persistir a Tarefa.
6. IF o Cliente_API enviar um campo de Score_Prioridade ou de status na requisição de criação, THEN THE TaskFlow_API SHALL ignorar esses campos e calcular o Score_Prioridade e definir o Status_Tarefa internamente.

### Requirement 2: Consulta de Tarefas

**User Story:** Como Cliente_API, quero consultar Tarefas individualmente e em lista ordenada por prioridade, para que eu possa decidir o que executar a seguir.

#### Acceptance Criteria

1. WHEN o Cliente_API solicita uma Tarefa pelo identificador, THE TaskFlow_API SHALL retornar a Tarefa correspondente com todos os seus atributos persistidos, incluindo o Score_Prioridade vigente.
2. IF o identificador informado não corresponder a uma Tarefa existente, THEN THE TaskFlow_API SHALL retornar uma resposta de recurso não encontrado.
3. WHEN o Cliente_API solicita a lista de Tarefas sem filtros, THE TaskFlow_API SHALL retornar todas as Tarefas ordenadas em ordem decrescente de Score_Prioridade como critério primário e em ordem ascendente de prazo como critério secundário em todas as posições da lista.
4. WHEN o Cliente_API solicita a lista de Tarefas, THE TaskFlow_API SHALL aplicar o prazo como critério secundário de ordenação mesmo quando os Score_Prioridade das Tarefas comparadas forem distintos.
5. WHERE o Cliente_API fornece o filtro de Status_Tarefa, THE TaskFlow_API SHALL retornar apenas Tarefas cujo Status_Tarefa coincida com o valor informado.
6. WHERE o Cliente_API fornece o filtro de Criticidade, THE TaskFlow_API SHALL retornar apenas Tarefas cuja Criticidade coincida com o valor informado.
7. WHEN a lista de Tarefas é retornada, THE TaskFlow_API SHALL suportar paginação por meio dos parâmetros de número de página e tamanho de página, com tamanho máximo de página igual a 100.

### Requirement 3: Atualização de Tarefas

**User Story:** Como Cliente_API, quero atualizar atributos de uma Tarefa existente, para que as informações reflitam mudanças de escopo ou planejamento.

#### Acceptance Criteria

1. WHEN o Cliente_API envia uma requisição de atualização para uma Tarefa existente com campos válidos, THE TaskFlow_API SHALL persistir os novos valores de título, descrição, prazo e Criticidade e atualizar o timestamp de atualização.
2. WHEN qualquer campo que influencie o Score_Prioridade for alterado, THE TaskFlow_API SHALL recalcular o Score_Prioridade da Tarefa antes de retornar a resposta.
3. IF a requisição de atualização contiver um Score_Prioridade fornecido pelo Cliente_API, THEN THE TaskFlow_API SHALL ignorar esse valor e manter o cálculo automático.
4. IF o novo prazo informado for anterior ao instante atual, THEN THE TaskFlow_API SHALL rejeitar a requisição com erro de validação e preservar o estado anterior da Tarefa.
5. IF o identificador informado não corresponder a uma Tarefa existente, THEN THE TaskFlow_API SHALL retornar uma resposta de recurso não encontrado e não criar uma nova Tarefa.
6. IF a Tarefa estiver com Status_Tarefa igual a `CONCLUIDA` ou `CANCELADA`, THEN THE TaskFlow_API SHALL rejeitar a atualização de título, descrição, prazo ou Criticidade com erro de regra de negócio.
7. WHILE a Tarefa estiver com Status_Tarefa igual a `PENDENTE` ou `EM_ANDAMENTO`, THE TaskFlow_API SHALL aceitar atualizações de título, descrição, prazo e Criticidade independentemente de quaisquer outros atributos.
8. THE TaskFlow_API SHALL tratar transições de Status_Tarefa exclusivamente conforme o Requirement 4 e SHALL não permitir que atualizações de Status_Tarefa sejam realizadas pelo endpoint de atualização geral de campos.

### Requirement 4: Transição de Status da Tarefa

**User Story:** Como Cliente_API, quero alterar o status de uma Tarefa, para que eu possa registrar progresso, conclusão ou cancelamento.

#### Acceptance Criteria

1. WHEN o Cliente_API solicita a transição do Status_Tarefa de `PENDENTE` para `EM_ANDAMENTO` em uma Tarefa existente, THE TaskFlow_API SHALL aceitar a transição, atualizar o Status_Tarefa para `EM_ANDAMENTO` e atualizar o timestamp de atualização para o instante atual em UTC com precisão de milissegundos.
2. WHEN o Cliente_API solicita a transição para `CONCLUIDA` a partir de `PENDENTE` ou `EM_ANDAMENTO` em uma Tarefa existente, THE TaskFlow_API SHALL aceitar a transição, registrar o instante de conclusão e o timestamp de atualização com o instante atual em UTC com precisão de milissegundos.
3. WHEN o Cliente_API solicita a transição para `CANCELADA` a partir de `PENDENTE` ou `EM_ANDAMENTO` em uma Tarefa existente, THE TaskFlow_API SHALL aceitar a transição e atualizar o timestamp de atualização para o instante atual em UTC com precisão de milissegundos.
4. IF o Cliente_API solicitar uma transição a partir de uma Tarefa_Encerrada (Status_Tarefa igual a `CONCLUIDA` ou `CANCELADA`), THEN THE TaskFlow_API SHALL rejeitar a requisição retornando erro de regra de negócio indicando que a Tarefa está encerrada e SHALL preservar o Status_Tarefa e os demais atributos da Tarefa inalterados.
5. IF a transição solicitada não pertencer ao conjunto `{PENDENTE→EM_ANDAMENTO, PENDENTE→CONCLUIDA, PENDENTE→CANCELADA, EM_ANDAMENTO→CONCLUIDA, EM_ANDAMENTO→CANCELADA}`, THEN THE TaskFlow_API SHALL rejeitar a requisição retornando erro de regra de negócio indicando a transição inválida e SHALL preservar o Status_Tarefa e os demais atributos da Tarefa inalterados.
6. IF o Cliente_API solicitar a transição de status de uma Tarefa cujo identificador não exista, THEN THE TaskFlow_API SHALL rejeitar a requisição retornando erro indicando que a Tarefa não foi encontrada.
7. IF o valor de Status_Tarefa solicitado não pertencer ao conjunto `{PENDENTE, EM_ANDAMENTO, CONCLUIDA, CANCELADA}`, THEN THE TaskFlow_API SHALL rejeitar a requisição retornando erro de validação indicando o valor de status inválido e SHALL preservar o Status_Tarefa e os demais atributos da Tarefa inalterados.

### Requirement 5: Remoção de Tarefas

**User Story:** Como Cliente_API, quero remover Tarefas que não são mais relevantes, para manter o conjunto de Tarefas gerenciado limpo.

#### Acceptance Criteria

1. WHEN o Cliente_API envia uma requisição de remoção informando o identificador de uma Tarefa existente, THE TaskFlow_API SHALL remover a Tarefa do Ambiente_Persistencia de forma permanente e irreversível e retornar uma resposta de sucesso sem corpo.
2. WHEN uma Tarefa é removida, THE TaskFlow_API SHALL responder com recurso não encontrado em qualquer consulta subsequente pelo mesmo identificador.
3. WHEN uma Tarefa é removida, THE TaskFlow_API SHALL excluí-la dos resultados de listagens subsequentes de Tarefas, independentemente dos filtros aplicados.
4. IF o identificador informado não corresponder a uma Tarefa existente, THEN THE TaskFlow_API SHALL retornar uma resposta de recurso não encontrado e preservar o estado do Ambiente_Persistencia sem realizar qualquer alteração.

### Requirement 6: Cálculo Automático de Prioridade

**User Story:** Como Cliente_API, quero que a prioridade de cada Tarefa seja calculada automaticamente pelo sistema, para evitar viés humano e garantir consistência.

#### Acceptance Criteria

1. THE Priorizador SHALL calcular o Score_Prioridade exclusivamente a partir da Criticidade declarada, da proximidade do prazo e da idade da Tarefa, sem aceitar valores informados pelo Cliente_API.
2. THE Priorizador SHALL produzir um Score_Prioridade inteiro entre 0 e 100, inclusive, para qualquer Tarefa_Ativa.
3. WHEN duas Tarefas_Ativas possuem o mesmo prazo e a mesma idade, THE Priorizador SHALL atribuir Score_Prioridade maior àquela com Criticidade mais alta, considerando a ordem `BAIXA < MEDIA < ALTA < URGENTE`.
4. WHEN duas Tarefas_Ativas possuem a mesma Criticidade e a mesma idade, THE Priorizador SHALL atribuir Score_Prioridade maior àquela cujo prazo for mais próximo do instante atual.
5. WHEN duas Tarefas_Ativas possuem a mesma Criticidade e o mesmo prazo, THE Priorizador SHALL atribuir Score_Prioridade maior àquela com maior idade, medida a partir do timestamp de criação.
6. WHEN o prazo de uma Tarefa_Ativa já está vencido em relação ao instante atual, THE Priorizador SHALL atribuir o Score_Prioridade máximo permitido pelo modelo.
7. WHEN uma Tarefa transita para `CONCLUIDA` ou `CANCELADA`, THE TaskFlow_API SHALL congelar o Score_Prioridade no valor calculado imediatamente antes da transição e não recalculá-lo enquanto a Tarefa permanecer encerrada.
8. THE Priorizador SHALL ser determinístico para um mesmo conjunto de entradas e um mesmo instante de referência, produzindo sempre o mesmo Score_Prioridade.

### Requirement 7: Validação de Entrada e Tratamento de Erros

**User Story:** Como Cliente_API, quero receber respostas de erro claras e padronizadas, para que eu possa identificar e corrigir problemas nas minhas requisições.

#### Acceptance Criteria

1. IF uma requisição contiver corpo malformado ou tipos de dados incompatíveis com o contrato, THEN THE TaskFlow_API SHALL responder com erro de requisição inválida e uma mensagem descritiva do problema.
2. IF uma requisição violar regras de validação de campos, THEN THE TaskFlow_API SHALL responder com erro de validação contendo a lista dos campos inválidos e o motivo de cada falha.
3. WHEN uma operação não pode ser concluída por violação de regra de negócio, THE TaskFlow_API SHALL responder com erro de conflito de regra de negócio e uma mensagem descritiva.
4. WHEN uma operação falha por erro inesperado interno, THE TaskFlow_API SHALL responder com erro genérico de servidor sem expor detalhes de implementação ou rastros de pilha.
5. THE TaskFlow_API SHALL utilizar um formato uniforme de resposta de erro contendo, no mínimo, os campos `codigo`, `mensagem`, `timestamp` e, quando aplicável, `detalhes`.

### Requirement 8: Persistência em PostgreSQL

**User Story:** Como mantenedor do produto, quero que as Tarefas sejam persistidas em um banco PostgreSQL, para garantir durabilidade e consultas relacionais consistentes.

#### Acceptance Criteria

1. THE TaskFlow_API SHALL armazenar Tarefas no Ambiente_Persistencia, que SHALL ser um banco de dados PostgreSQL na versão 14 ou superior.
2. WHEN a TaskFlow_API é inicializada, THE TaskFlow_API SHALL aplicar migrações de esquema versionadas antes de aceitar requisições.
3. THE TaskFlow_API SHALL definir o identificador da Tarefa como UUID gerado no momento da criação.
4. THE TaskFlow_API SHALL definir restrições de não nulidade no Ambiente_Persistencia para os campos `id`, `titulo`, `prazo`, `criticidade`, `status` e timestamps de criação e atualização.
5. IF a conexão com o Ambiente_Persistencia estiver indisponível no momento de uma requisição, THEN THE TaskFlow_API SHALL responder com erro de serviço indisponível.

### Requirement 9: Stack Tecnológica e Empacotamento

**User Story:** Como mantenedor do produto, quero que a TaskFlow_API utilize uma stack tecnológica fixa, para garantir compatibilidade, segurança e padronização entre ambientes.

#### Acceptance Criteria

1. THE TaskFlow_API SHALL ser implementada em Java na versão 21.
2. THE TaskFlow_API SHALL utilizar Spring Boot na versão 3.x como framework de aplicação.
3. THE TaskFlow_API SHALL ser construída e gerenciada com Apache Maven.
4. THE TaskFlow_API SHALL ser empacotada como artefato executável compatível com a JVM 21.
5. IF qualquer dependência declarada exigir versão de Java inferior à 21 ou versão de Spring Boot fora da linha 3.x, THEN THE Pipeline_CI_CD SHALL falhar a etapa de build.
6. THE Pipeline_CI_CD SHALL falhar a etapa de build quando dependências incompatíveis forem detectadas, independentemente de quaisquer parâmetros de bypass, perfis de desenvolvimento ou variáveis de ambiente que solicitem ignorar a verificação.

### Requirement 10: Qualidade e Testes Unitários

**User Story:** Como mantenedor do produto, quero exigir uma suíte de testes unitários automatizada, para reduzir regressões e validar regras de negócio.

#### Acceptance Criteria

1. THE TaskFlow_API SHALL possuir testes unitários automatizados para o Priorizador cobrindo, no mínimo, as regras de desempate definidas no Requirement 6.
2. THE TaskFlow_API SHALL possuir testes unitários automatizados para as regras de validação de criação e atualização de Tarefas definidas nos Requirements 1 e 3.
3. THE TaskFlow_API SHALL possuir testes unitários automatizados para as transições válidas e inválidas de Status_Tarefa definidas no Requirement 4.
4. WHEN a suíte de testes unitários é executada, THE TaskFlow_API SHALL atingir cobertura mínima de 80% de linhas no pacote do domínio que contém o Priorizador e as regras de Tarefa.
5. IF a cobertura mínima definida não for alcançada em uma execução do Pipeline_CI_CD, THEN THE Pipeline_CI_CD SHALL falhar a etapa de testes.

### Requirement 11: Pipeline de Integração e Entrega Contínuas

**User Story:** Como mantenedor do produto, quero um Pipeline_CI_CD automatizado, para garantir que cada alteração de código seja validada e que artefatos confiáveis sejam produzidos.

#### Acceptance Criteria

1. WHEN um commit é enviado para a branch principal ou um pull request é aberto, THE Pipeline_CI_CD SHALL executar, em ordem, as etapas de checkout, build com Maven, execução de testes unitários e geração do artefato.
2. IF qualquer etapa do Pipeline_CI_CD falhar, THEN THE Pipeline_CI_CD SHALL marcar a execução como falha e impedir a publicação do artefato.
3. WHEN todas as etapas do Pipeline_CI_CD são concluídas com sucesso na branch principal, THE Pipeline_CI_CD SHALL publicar o artefato gerado em um repositório de artefatos definido pelo projeto.
4. THE Pipeline_CI_CD SHALL utilizar a mesma versão de Java e a mesma versão de Maven declaradas nos arquivos de build do projeto.
5. THE Pipeline_CI_CD SHALL expor o resultado de cada execução por meio de uma interface acessível ao mantenedor do produto.

### Requirement 12: Documentação do Contrato da API

**User Story:** Como Cliente_API, quero acessar uma documentação atualizada do contrato da TaskFlow_API, para que eu possa integrá-la corretamente.

#### Acceptance Criteria

1. THE TaskFlow_API SHALL expor um documento de especificação OpenAPI 3.x descrevendo todos os endpoints públicos, parâmetros e formatos de resposta.
2. WHEN um endpoint é adicionado, alterado ou removido no código-fonte, THE TaskFlow_API SHALL refletir a alteração no documento OpenAPI gerado em prazo não superior a 7 dias corridos a partir da publicação da alteração de código no ambiente de desenvolvimento.
3. THE TaskFlow_API SHALL disponibilizar uma interface navegável da especificação OpenAPI em ambiente de desenvolvimento.
