package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.log.service.LogJobService;
import uk.gov.hmcts.darts.log.util.LogJobReport;

@Service
@AllArgsConstructor
@Slf4j
public class LogJobServiceImpl implements LogJobService {

    @Override
    public void logJobReport(LogJobReport report) {
        if (report.haveAllProcessed()) {
            log.info(String.valueOf(report));
        } else {
            log.error(String.valueOf(report));
        }
    }
}