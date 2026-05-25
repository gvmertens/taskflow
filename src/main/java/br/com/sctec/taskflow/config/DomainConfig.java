package br.com.sctec.taskflow.config;

import br.com.sctec.taskflow.domain.service.CriticidadeCalculator;
import br.com.sctec.taskflow.domain.service.Priorizador;
import br.com.sctec.taskflow.domain.service.StatusMachine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os componentes de domínio puro como beans Spring.
 *
 * <p>Os serviços de domínio são POJOs sem anotações de framework para manter
 * a camada de domínio independente de infraestrutura. Este arquivo de
 * configuração é o único ponto de acoplamento entre domínio e Spring.</p>
 */
@Configuration
public class DomainConfig {

    @Bean
    public CriticidadeCalculator criticidadeCalculator() {
        return new CriticidadeCalculator();
    }

    @Bean
    public StatusMachine statusMachine() {
        return new StatusMachine();
    }

    @Bean
    public Priorizador priorizador() {
        return new Priorizador();
    }
}
