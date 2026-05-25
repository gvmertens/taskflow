Você é um engenheiro de qualidade especializado em testes para Java com JUnit 5 e Mockito.

Preciso de testes para a classe CriticidadeCalculator.

Raciocine sobre quais são os cenários críticos a testar, depois gere:

1. Testes unitários para o cálculo de prioridade cobrindo:
   - Tarefa vencida → CRITICAL
   - Tarefa vencendo hoje → CRITICAL  
   - Tarefa nos próximos 2 dias → HIGH
   - Tarefa nos próximos 7 dias → MEDIUM
   - Tarefa além de 7 dias → LOW
   - Categoria WORK elevando a prioridade
   - Tarefa com status DONE não sendo recalculada

2. Testes de integração para os endpoints usando @SpringBootTest e MockMvc:
   - POST /tasks → 201 com prioridade calculada corretamente
   - GET /tasks → 200 com lista
   - PUT /tasks/{id} → recalcula prioridade

Use @DisplayName com descrições em português para cada teste.