package com.barbearia.barbearia.modules.googlecalender.listener;

import com.barbearia.barbearia.modules.googlecalender.service.GoogleCalenderService;
import com.barbearia.barbearia.modules.scheduling.event.SchedulingCompletedEvent;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.scheduling.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulingCalendarListener {

    private final SchedulingRepository schedulingRepository;
    private final GoogleCalenderService googleCalenderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onSchedulingCompleted(SchedulingCompletedEvent event) {
        try {
            Scheduling scheduling = schedulingRepository
                    .findDeatilByIdAndBusinessId(event.schedulingid(), event.businessId())
                    .orElse(null);

            if (scheduling == null) {
                log.warn("Scheduling {} não encontrado para sync de calendário.", event.schedulingid());
                return;
            }

            googleCalenderService.syncSchedulingUpdated(scheduling);
        } catch (Exception ex) {
            log.warn("Falha ao sincronizar Google Calendar para scheduling {}: {}",
                    event.schedulingid(), ex.getMessage());
        }
    }
}
