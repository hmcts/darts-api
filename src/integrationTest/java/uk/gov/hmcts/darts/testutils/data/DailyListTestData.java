package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.testutils.TestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

public class DailyListTestData {

    public static DailyListEntity createDailyList(LocalTime time, String source, CourthouseEntity courthouse, String fileLocation) throws IOException {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setStartDate(LocalDate.now());
        dailyListEntity.setEndDate(LocalDate.now());
        dailyListEntity.setListingCourthouse(courthouse.getCourthouseName());
        dailyListEntity.setContent(TestUtils.substituteHearingDateWithToday(getContentsFromFile(fileLocation)));
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
