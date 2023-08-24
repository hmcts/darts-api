package uk.gov.hmcts.darts.dailylist.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.time.LocalDate;

public interface DailyListProcessor {

    void processAllDailyLists(LocalDate date);

    void processAllDailyListForCourthouse(CourthouseEntity courthouseEntity);
}
