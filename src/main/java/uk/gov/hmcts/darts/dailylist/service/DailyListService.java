package uk.gov.hmcts.darts.dailylist.service;

import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequest;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;

public interface DailyListService {
    PostDailyListResponse saveDailyListToDatabase(DailyListPostRequestInternal postRequest);

    PostDailyListResponse updateDailyListInDatabase(DailyListPatchRequest patchRequest);

    void runHouseKeeping();
}
