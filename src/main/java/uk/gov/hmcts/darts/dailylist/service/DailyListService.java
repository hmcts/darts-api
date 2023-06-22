package uk.gov.hmcts.darts.dailylist.service;

import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;

public interface DailyListService {
    void processIncomingDailyList(DailyListPostRequest postRequest);
}
