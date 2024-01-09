package uk.gov.hmcts.darts.dailylist.service;

public interface DailyListProcessor {

    void processAllDailyLists();

    void processAllDailyListForListingCourthouse(String listingCourthouse);
}
