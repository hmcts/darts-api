package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DailyListStub {
    private final DailyListRepository dailyListRepository;

    public void createEmptyDailyList(LocalDate date, String courthouse) {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStartDate(date);
        dailyListEntity.setListingCourthouse(courthouse);
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListRepository.saveAndFlush(dailyListEntity);
    }

}
