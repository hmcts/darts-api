package uk.gov.hmcts.darts.dailylist.service;

import java.time.LocalDate;

public interface DailyListProcessor {

    void processAllDailyLists(LocalDate date);

    void processAllDailyListForCourthouse(Integer courthouseId);
}
