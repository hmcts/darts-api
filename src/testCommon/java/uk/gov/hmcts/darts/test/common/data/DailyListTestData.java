package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

public final class DailyListTestData {

    private DailyListTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static DailyListEntity createDailyList(LocalTime time, String source, String listingCourthouse, String fileLocation) throws IOException {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setStartDate(LocalDate.now());
        dailyListEntity.setEndDate(LocalDate.now());

        dailyListEntity.setListingCourthouse(listingCourthouse);
        String fileContents = getContentsFromFile(fileLocation);
        fileContents = fileContents.replace("${COURTHOUSE}", listingCourthouse);

        dailyListEntity.setContent(TestUtils.substituteHearingDateWithToday(fileContents));
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(source);
        return dailyListEntity;
    }

    public static DailyListEntity minimalDailyList() {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        return dailyListEntity;
    }
}