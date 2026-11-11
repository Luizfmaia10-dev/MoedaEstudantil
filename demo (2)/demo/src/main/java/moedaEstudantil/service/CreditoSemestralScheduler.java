package moedaEstudantil.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CreditoSemestralScheduler {
    private final CreditoSemestralProfessorService service;

    public CreditoSemestralScheduler(CreditoSemestralProfessorService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() { processar(); }

    @Scheduled(cron = "${app.credito-semestral.cron:0 10 0 * * *}", zone = "America/Sao_Paulo")
    public void diariamente() { processar(); }

    private void processar() { service.processar(LocalDate.now()); }
}
