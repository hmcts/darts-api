package uk.gov.hmcts.darts.dailylist.service;

import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequest;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;

public interface DailyListService {
    PostDailyListResponse saveDailyListToDatabase(DailyListPostRequest postRequest);

    PostDailyListResponse updateDailyListInDatabase(DailyListPatchRequest patchRequest);

    void runHouseKeeping();

    void runHouseKeepingNow();
}
