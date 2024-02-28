package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.log.service.DailyListLogJobService;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;

@Service
@AllArgsConstructor
@Slf4j
public class DailyListLogJobServiceImpl implements DailyListLogJobService {

    @Override
    public void logJobReport(DailyListLogJobReport report) {
        if (report.haveAllExpectedResults()) {
            log.info(String.valueOf(report));
        } else {
            log.error(String.valueOf(report));
        }
    }
}