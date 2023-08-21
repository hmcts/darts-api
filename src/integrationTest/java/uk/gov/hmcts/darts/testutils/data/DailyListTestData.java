package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
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

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class DailyListTestData {
    public DailyListEntity createDailyList(LocalTime time, String source, CourthouseEntity courthouse, String fileLocation) throws IOException {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(String.valueOf(JobStatusType.NEW));
        dailyListEntity.setStartDate(LocalDate.now());
        dailyListEntity.setEndDate(LocalDate.now());
        dailyListEntity.setCourthouse(courthouse);
        dailyListEntity.setContent(TestUtils.substituteHearingDateWithToday(getContentsFromFile(fileLocation)));
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(source);
        return dailyListEntity;
    }
}
