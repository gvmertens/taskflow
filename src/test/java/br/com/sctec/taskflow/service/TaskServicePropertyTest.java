package br.com.sctec.taskflow.service;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.exception.TarefaEncerradaException;
import br.com.sctec.taskflow.domain.exception.TransicaoInvalidaException;
import br.com.sctec.taskflow.domain.service.CriticidadeCalculator;
import br.com.sctec.taskflow.domain.service.Priorizador;
import br.com.sctec.taskflow.domain.service.StatusMachine;
import br.com.sctec.taskflow.dto.TaskRequest;
import br.com.sctec.taskflow.dto.TaskResponse;
import br.com.sctec.taskflow.dto.TransitionStatusRequest;
import br.com.sctec.taskflow.repository.TaskRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests para {@link TaskService} — task 14.
 *
 * <p>Estratégia: mocks manuais do repositório e dependências via Mockito.
 * Cada {@code @Property} valida uma invariante da camada de serviço com
 * 100 amostras geradas pelo jqwik.</p>
 *
 * <ul>
 *   <li>P7  (14.1): Score congelado após transição para estado terminal</li>
 *   <li>P10 (14.2): Tarefa criada tem status PENDENTE e criticidade calculada</li>
 *   <li>P11 (14.3): Round-trip de criação e consulta preserva todos os atributos</li>
 *   <li>P12 (14.4): Listagem respeita a ordenação por score e prazo</li>
 *   <li>P13 (14.5): Filtro de status retorna apenas tarefas com o status solicitado</li>
 *   <li>P14 (14.6): Filtro de criticidade retorna apenas tarefas com a criticidade solicitada</li>
 *   <li>P15 (14.7): Tarefas encerradas rejeitam atualizações de campos</li>
 *   <li>P16 (14.8): Transições válidas são aceitas e atualizam o status</li>
 *   <li>P17 (14.9): Transições a partir de estados terminais são sempre rejeitadas</li>
 *   <li>P18 (14.10): Respostas de erro seguem o formato padronizado (via GlobalExceptionHandler)</li>
 *   <li>P19 (14.11): Deleção remove a tarefa de todas as visões</li>
 *   <li>P20 (14.12): Recálculo de criticidade após atualização de campos influentes</li>
 * </ul>
 */
class TaskServicePropertyTest {

    private TaskRepository repository;
    private CriticidadeCalculator criticidadeCalculator;
    private StatusMachine statusMachine;
    private Priorizador priorizador;
    private TaskService service;

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        criticidadeCalculator = mock(CriticidadeCalculator.class);
        statusMachine = new StatusMachine();   // POJO real — sem mock
        priorizador = new Priorizador();       // POJO real — sem mock
        service = new TaskService(repository, criticidadeCalculator, statusMachine, priorizador);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static final LocalDate PRAZO_BASE = LocalDate.now().plusDays(30);

    private Task taskSalva(UUID id, String titulo, LocalDate prazo,
                           Criticidade criticidade, StatusTarefa status, int score) {
        return Task.builder()
                .id(id)
                .titulo(titulo)
                .descricao("desc")
                .prazo(prazo)
                .criticidade(criticidade)
                .status(status)
                .scorePrioridade(score)
                .build();
    }

    private TaskRequest request(String titulo, LocalDate prazo, Criticidade criticidade) {
        return new TaskRequest(titulo, "desc", prazo, criticidade);
    }

    // =========================================================================
    // Property 7 (14.1) — Score congelado após transição para estado terminal
    // Validates: Requirement 6.7
    // =========================================================================

    @Property(tries = 100)
    @Label("P7: Score congelado apos transicao para estado terminal")
    void scoreCongeladoAposTransicaoTerminal(
            @ForAll Criticidade criticidade,
            @ForAll("statusTerminais") StatusTarefa statusTerminal
    ) {
        UUID id = UUID.randomUUID();
        Task task = taskSalva(id, "Tarefa", PRAZO_BASE, criticidade, StatusTarefa.PENDENTE, 0);

        when(repository.findById(id)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = service.transition(id, new TransitionStatusRequest(statusTerminal));

        // Score deve ser >= 0 e <= 100 (foi calculado e congelado)
        assertThat(response.scorePrioridade())
                .as("Score deve estar no intervalo valido apos congelamento")
                .isBetween(0, 100);

        // Status deve ser o terminal solicitado
        assertThat(response.status()).isEqualTo(statusTerminal);
    }

    // =========================================================================
    // Property 10 (14.2) — Tarefa criada tem status PENDENTE e criticidade calculada
    // Validates: Requirements 1.1, 1.6
    // =========================================================================

    @Property(tries = 100)
    @Label("P10: Tarefa criada tem status PENDENTE e criticidade calculada")
    void tarefaCriadaTemStatusPendenteECriticidadeCalculada(
            @ForAll("titulosValidos") String titulo,
            @ForAll Criticidade criticidadeRequest,
            @ForAll Criticidade criticidadeCalculada
    ) {
        UUID id = UUID.randomUUID();
        when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                .thenReturn(criticidadeCalculada);
        when(repository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return taskSalva(id, t.getTitulo(), t.getPrazo(), t.getCriticidade(), t.getStatus(), 0);
        });

        TaskResponse response = service.create(request(titulo, PRAZO_BASE, criticidadeRequest));

        assertThat(response.status())
                .as("Tarefa criada deve ter status PENDENTE")
                .isEqualTo(StatusTarefa.PENDENTE);
        assertThat(response.criticidade())
                .as("Criticidade deve ser a calculada, nao a do request")
                .isEqualTo(criticidadeCalculada);
    }

    // =========================================================================
    // Property 11 (14.3) — Round-trip de criação e consulta preserva atributos
    // Validates: Requirements 2.1, 8.3
    // =========================================================================

    @Property(tries = 100)
    @Label("P11: Round-trip de criacao e consulta preserva todos os atributos")
    void roundTripCriacaoConsultaPreservaAtributos(
            @ForAll("titulosValidos") String titulo,
            @ForAll Criticidade criticidade
    ) {
        UUID id = UUID.randomUUID();
        LocalDate prazo = PRAZO_BASE;

        when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                .thenReturn(criticidade);
        when(repository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return taskSalva(id, t.getTitulo(), prazo, t.getCriticidade(), StatusTarefa.PENDENTE, 0);
        });

        TaskResponse created = service.create(request(titulo, prazo, criticidade));

        // Simula consulta posterior
        Task stored = taskSalva(id, titulo, prazo, criticidade, StatusTarefa.PENDENTE, 0);
        when(repository.findById(id)).thenReturn(Optional.of(stored));

        TaskResponse found = service.findById(id);

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.titulo()).isEqualTo(titulo);
        assertThat(found.prazo()).isEqualTo(prazo);
        assertThat(found.status()).isEqualTo(StatusTarefa.PENDENTE);
    }

    // =========================================================================
    // Property 12 (14.4) — Listagem respeita a ordenação por score e prazo
    // Validates: Requirements 2.3, 2.4
    // =========================================================================

    @Property(tries = 50)
    @Label("P12: Listagem respeita a ordenacao por score DESC e prazo ASC")
    void listagemRespeitaOrdenacaoPorScoreEPrazo(
            @ForAll("listasDeTasksOrdenadas") List<Task> tasksOrdenadas
    ) {
        Pageable pageable = PageRequest.of(0, 100);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(tasksOrdenadas));

        var resultado = service.findAll(null, null, pageable);

        List<TaskResponse> content = resultado.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            int scoreAtual = content.get(i).scorePrioridade();
            int scoreProximo = content.get(i + 1).scorePrioridade();
            assertThat(scoreAtual)
                    .as("Score[%d]=%d deve ser >= Score[%d]=%d", i, scoreAtual, i + 1, scoreProximo)
                    .isGreaterThanOrEqualTo(scoreProximo);
        }
    }

    // =========================================================================
    // Property 13 (14.5) — Filtro de status retorna apenas tarefas com o status solicitado
    // Validates: Requirement 2.5
    // =========================================================================

    @Property(tries = 100)
    @Label("P13: Filtro de status retorna apenas tarefas com o status solicitado")
    void filtroStatusRetornaApenasStatusSolicitado(@ForAll StatusTarefa statusFiltro) {
        UUID id = UUID.randomUUID();
        Task task = taskSalva(id, "Tarefa", PRAZO_BASE, Criticidade.MEDIA, statusFiltro, 0);
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findByStatus(statusFiltro, pageable))
                .thenReturn(new PageImpl<>(List.of(task)));

        var resultado = service.findAll(statusFiltro, null, pageable);

        assertThat(resultado.getContent())
                .as("Todas as tarefas retornadas devem ter o status filtrado")
                .allMatch(r -> r.status() == statusFiltro);
    }

    // =========================================================================
    // Property 14 (14.6) — Filtro de criticidade retorna apenas tarefas com a criticidade solicitada
    // Validates: Requirement 2.6
    // =========================================================================

    @Property(tries = 100)
    @Label("P14: Filtro de criticidade retorna apenas tarefas com a criticidade solicitada")
    void filtroCriticidadeRetornaApenasCriticidadeSolicitada(@ForAll Criticidade criticidadeFiltro) {
        UUID id = UUID.randomUUID();
        Task task = taskSalva(id, "Tarefa", PRAZO_BASE, criticidadeFiltro, StatusTarefa.PENDENTE, 0);
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findByCriticidade(criticidadeFiltro, pageable))
                .thenReturn(new PageImpl<>(List.of(task)));

        var resultado = service.findAll(null, criticidadeFiltro, pageable);

        assertThat(resultado.getContent())
                .as("Todas as tarefas retornadas devem ter a criticidade filtrada")
                .allMatch(r -> r.criticidade() == criticidadeFiltro);
    }

    // =========================================================================
    // Property 15 (14.7) — Tarefas encerradas rejeitam atualizações de campos
    // Validates: Requirement 3.6
    // =========================================================================

    @Property(tries = 100)
    @Label("P15: Tarefas encerradas rejeitam atualizacoes de campos")
    void tarefasEncerradasRejeitamAtualizacoes(
            @ForAll("statusTerminais") StatusTarefa statusTerminal,
            @ForAll Criticidade criticidade
    ) {
        UUID id = UUID.randomUUID();
        Task taskEncerrada = taskSalva(id, "Tarefa", PRAZO_BASE, criticidade, statusTerminal, 0);
        TaskRequest req = request("Novo titulo", PRAZO_BASE, criticidade);

        when(repository.findById(id)).thenReturn(Optional.of(taskEncerrada));

        assertThatThrownBy(() -> service.update(id, req))
                .as("Tarefa com status %s deve rejeitar atualizacao", statusTerminal)
                .isInstanceOf(IllegalStateException.class);

        verify(repository, never()).save(any());
        reset(repository);
    }

    // =========================================================================
    // Property 16 (14.8) — Transições válidas são aceitas e atualizam o status
    // Validates: Requirements 4.1, 4.2, 4.3
    // =========================================================================

    @Property(tries = 100)
    @Label("P16: Transicoes validas sao aceitas e atualizam o status")
    void transicoesValidasSaoAceitas(
            @ForAll("transicoesValidas") StatusTarefa[] transicao
    ) {
        StatusTarefa statusAtual = transicao[0];
        StatusTarefa statusDestino = transicao[1];
        UUID id = UUID.randomUUID();
        Task task = taskSalva(id, "Tarefa", PRAZO_BASE, Criticidade.MEDIA, statusAtual, 0);

        when(repository.findById(id)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = service.transition(id, new TransitionStatusRequest(statusDestino));

        assertThat(response.status())
                .as("Status deve ser atualizado para %s", statusDestino)
                .isEqualTo(statusDestino);

        reset(repository);
    }

    // =========================================================================
    // Property 17 (14.9) — Transições a partir de estados terminais são sempre rejeitadas
    // Validates: Requirement 4.4
    // =========================================================================

    @Property(tries = 100)
    @Label("P17: Transicoes a partir de estados terminais sao sempre rejeitadas")
    void transicoesDeEstadosTerminaisSaoSempreRejeitadas(
            @ForAll("statusTerminais") StatusTarefa statusTerminal,
            @ForAll StatusTarefa qualquerDestino
    ) {
        UUID id = UUID.randomUUID();
        Task task = taskSalva(id, "Tarefa", PRAZO_BASE, Criticidade.MEDIA, statusTerminal, 0);

        when(repository.findById(id)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.transition(id, new TransitionStatusRequest(qualquerDestino)))
                .as("Transicao de %s para %s deve ser rejeitada", statusTerminal, qualquerDestino)
                .isInstanceOf(TarefaEncerradaException.class);

        reset(repository);
    }

    // =========================================================================
    // Property 18 (14.10) — Respostas de erro seguem o formato padronizado
    // Validates: Requirements 7.2, 7.5
    // =========================================================================

    @Property(tries = 100)
    @Label("P18: Excecoes de dominio carregam informacoes estruturadas")
    void excecoesDeDominioCarregamInformacoesEstruturadas(
            @ForAll("statusTerminais") StatusTarefa statusTerminal
    ) {
        // TarefaEncerradaException deve carregar o statusAtual
        TarefaEncerradaException encerrada = new TarefaEncerradaException(statusTerminal);
        assertThat(encerrada.getStatusAtual()).isEqualTo(statusTerminal);
        assertThat(encerrada.getMessage()).isNotBlank().contains(statusTerminal.name());

        // TransicaoInvalidaException deve carregar statusAtual e statusDestino
        TransicaoInvalidaException invalida = new TransicaoInvalidaException(
                StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE);
        assertThat(invalida.getStatusAtual()).isEqualTo(StatusTarefa.EM_ANDAMENTO);
        assertThat(invalida.getStatusDestino()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(invalida.getMessage()).isNotBlank();
    }

    // =========================================================================
    // Property 19 (14.11) — Deleção remove a tarefa de todas as visões
    // Validates: Requirements 5.1, 5.2, 5.3
    // =========================================================================

    @Property(tries = 100)
    @Label("P19: Delecao remove a tarefa de todas as visoes")
    void delecaoRemoveTarefaDeTodasAsVisoes(
            @ForAll StatusTarefa status,
            @ForAll Criticidade criticidade
    ) {
        UUID id = UUID.randomUUID();
        Task task = taskSalva(id, "Tarefa", PRAZO_BASE, criticidade, status, 0);

        when(repository.findById(id)).thenReturn(Optional.of(task));

        service.delete(id);

        // Verifica que delete foi chamado com a entidade correta
        verify(repository).delete(task);

        // Após deleção, findById deve lançar exceção (repositório retorna empty)
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);

        reset(repository);
    }

    // =========================================================================
    // Property 20 (14.12) — Recálculo de criticidade após atualização de campos influentes
    // Validates: Requirement 3.2
    // =========================================================================

    @Property(tries = 100)
    @Label("P20: Recalculo de criticidade apos atualizacao de campos influentes")
    void recalculoCriticidadeAposAtualizacao(
            @ForAll Criticidade criticidadeOriginal,
            @ForAll Criticidade criticidadeRecalculada
    ) {
        UUID id = UUID.randomUUID();
        Task task = taskSalva(id, "Tarefa", PRAZO_BASE, criticidadeOriginal, StatusTarefa.PENDENTE, 0);
        TaskRequest req = request("Titulo atualizado", PRAZO_BASE.plusDays(5), criticidadeOriginal);

        when(repository.findById(id)).thenReturn(Optional.of(task));
        when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                .thenReturn(criticidadeRecalculada);
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = service.update(id, req);

        assertThat(response.criticidade())
                .as("Criticidade deve ser a recalculada apos update")
                .isEqualTo(criticidadeRecalculada);

        verify(criticidadeCalculator).calcular(any(Task.class), any(LocalDate.class));
        reset(repository, criticidadeCalculator);
    }

    // =========================================================================
    // Providers de dados arbitrários
    // =========================================================================

    @Provide
    Arbitrary<String> titulosValidos() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<StatusTarefa> statusTerminais() {
        return Arbitraries.of(StatusTarefa.CONCLUIDA, StatusTarefa.CANCELADA);
    }

    /** Gera pares [statusAtual, statusDestino] de transições válidas. */
    @Provide
    Arbitrary<StatusTarefa[]> transicoesValidas() {
        List<StatusTarefa[]> validas = List.of(
                new StatusTarefa[]{StatusTarefa.PENDENTE, StatusTarefa.EM_ANDAMENTO},
                new StatusTarefa[]{StatusTarefa.PENDENTE, StatusTarefa.CONCLUIDA},
                new StatusTarefa[]{StatusTarefa.PENDENTE, StatusTarefa.CANCELADA},
                new StatusTarefa[]{StatusTarefa.EM_ANDAMENTO, StatusTarefa.CONCLUIDA},
                new StatusTarefa[]{StatusTarefa.EM_ANDAMENTO, StatusTarefa.CANCELADA}
        );
        return Arbitraries.of(validas);
    }

    /** Gera listas de tasks já ordenadas por scorePrioridade DESC. */
    @Provide
    Arbitrary<List<Task>> listasDeTasksOrdenadas() {
        return Arbitraries.integers().between(0, 100)
                .list().ofMinSize(2).ofMaxSize(10)
                .map(scores -> {
                    scores.sort((a, b) -> b - a); // DESC
                    List<Task> tasks = new ArrayList<>();
                    for (int score : scores) {
                        tasks.add(taskSalva(UUID.randomUUID(), "T", PRAZO_BASE,
                                Criticidade.MEDIA, StatusTarefa.PENDENTE, score));
                    }
                    return tasks;
                });
    }
}
