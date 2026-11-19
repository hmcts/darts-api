package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.repository.DetsCleanupMaintenanceRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetsCleanupMaintenanceService {

    private final DetsCleanupMaintenanceRepository detsCleanupMaintenanceRepository;

    // PostgreSQL VACUUM must execute outside a transaction, including any caller's transaction.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void vacuumTables(int timeoutSeconds) {
        log.info("Vacuuming and analysing DETS cleanup tables with timeout of {} seconds", timeoutSeconds);
        detsCleanupMaintenanceRepository.vacuumTables(timeoutSeconds);
        log.info("Completed vacuum and analyse of DETS cleanup tables");
    }
}
