Você é um desenvolvedor Java sênior. Vou te mostrar o padrão que quero seguir 
e depois peço que gere o código.

Exemplo de DTO que sigo:
public record TaskRequest(
    @NotBlank String title,
    @NotNull LocalDate dueDate,
    @NotNull Category category
) {}

Exemplo de Service que sigo (sem lógica ainda):
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository repository;
    // métodos aqui
}

Agora gere para mim:
1. A entidade Task com todos os campos definidos na arquitetura
2. O enum Status (TODO, IN_PROGRESS, DONE) e Category (WORK, PERSONAL, STUDY)
3. O enum Priority (LOW, MEDIUM, HIGH, CRITICAL)
4. TaskRequest e TaskResponse como records
5. TaskRepository estendendo JpaRepository
6. TaskService com os métodos: create, findAll, findById, update, delete

Restrições:
- Use Lombok (@RequiredArgsConstructor, @Builder, etc.)
- Validações com Bean Validation nas requests
- Timestamps automáticos com @CreationTimestamp e @UpdateTimestamp
- Não implemente a priorização ainda — deixe um método stub comentado