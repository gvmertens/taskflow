Analise o seguinte método de priorização automática de tarefas e raciocine 
passo a passo para implementá-lo corretamente.

Regras de negócio da priorização:
- Se o prazo é hoje ou já passou → CRITICAL
- Se o prazo é nos próximos 2 dias → HIGH
- Se o prazo é nos próximos 7 dias → MEDIUM
- Se o prazo é depois de 7 dias → LOW
- Categoria WORK aumenta a prioridade em um nível (ex: MEDIUM vira HIGH)
- Tarefas com status DONE nunca são recalculadas

Raciocine sobre:
1. Onde essa lógica deve ficar na arquitetura (Service? Domain? Separate class?)
2. Como evitar violação do Single Responsibility Principle
3. Como garantir que o cálculo aconteça automaticamente no create e no update

Implemente a solução completa, incluindo a classe responsável pelo cálculo e 
a integração com o TaskService.