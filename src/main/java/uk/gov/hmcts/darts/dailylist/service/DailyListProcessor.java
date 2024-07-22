package uk.gov.hmcts.darts.dailylist.service;

public interface DailyListProcessor {

    void processAllDailyListsWithLock(String listingCourthouse);

    void processAllDailyLists();

    void processAllDailyListForListingCourthouse(String listingCourthouse);
}
