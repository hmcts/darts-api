package uk.gov.hmcts.darts.dailylist.service;

import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;

public interface DailyListService {
    PostDailyListResponse saveDailyListToDatabase(DailyListPostRequestInternal postRequest);

    PostDailyListResponse updateDailyListInDatabase(DailyListPatchRequestInternal patchRequest);

    void runHouseKeeping(Integer batchSize);
}
