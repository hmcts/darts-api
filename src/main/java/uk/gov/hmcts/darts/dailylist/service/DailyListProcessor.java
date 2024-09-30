package uk.gov.hmcts.darts.dailylist.service;

public interface DailyListProcessor {

    void processAllDailyListsWithLock(String listingCourthouse, boolean async);

    void processAllDailyLists();

    void processAllDailyListForListingCourthouse(String listingCourthouse);
}