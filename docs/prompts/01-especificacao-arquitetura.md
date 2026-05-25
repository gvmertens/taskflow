Você é um arquiteto de software sênior especializado em APIs REST com Java e Spring Boot.

Preciso planejar a arquitetura de uma API REST chamada TaskFlow, cujo objetivo é 
gerenciar tarefas com priorização automática.

Raciocine passo a passo sobre:
1. Qual padrão arquitetural é mais adequado (Layered, Hexagonal, etc.) e por quê
2. Quais entidades e relacionamentos são necessários
3. Como modelar a lógica de priorização automática no domínio
4. Quais endpoints REST devem existir e o contrato de cada um
5. Quais dependências do Spring Boot são necessárias (starters)
6. Como organizar os pacotes do projeto

Restrições:
- Java 21, Spring Boot 3.x, PostgreSQL, Maven
- A priorização deve ser calculada automaticamente pelo sistema, não inserida manualmente
- O projeto precisa ter testes unitários e pipeline de CI/CD

Ao final, gere um diagrama de arquitetura em formato Mermaid.