package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.TestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PARTIALLY_PROCESSED;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PROCESSED;

@ExtendWith(MockitoExtension.class)
class DailyListUpdaterTest {

    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private CourthouseRepository courthouseRepository;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private SystemUserHelper systemUserHelper;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private DailyListUpdater dailyListUpdater;

    LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        dailyListUpdater = new DailyListUpdater(retrieveCoreObjectService, courthouseRepository,
                                                hearingRepository, objectMapper, systemUserHelper, currentTimeHelper);
    }

    @Test
    void handlesCaseNumberMissingForCpp() throws IOException {

        var dailyListUser = new UserAccountEntity();
        when(systemUserHelper.getDailyListProcessorUser()).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseNameIgnoreCase("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);

        DailyListEntity dailyList = setUpDailyList("dailyListWithoutUrn.json");
        dailyListUpdater.processDailyList(dailyList);

        verifyNoInteractions(retrieveCoreObjectService);
        assertThat(dailyList.getStatus()).isEqualTo(PARTIALLY_PROCESSED);
    }

    @Test
    void handlesCaseNumberForCpp() throws IOException {

        var dailyListUser = new UserAccountEntity();
        when(systemUserHelper.getDailyListProcessorUser()).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseNameIgnoreCase("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);
        when(retrieveCoreObjectService.retrieveOrCreateHearing("SWANSEA", "1A", "42GD2391421", today, dailyListUser))
            .thenReturn(hearing);

        DailyListEntity dailyList = setUpDailyList("dailyList.json");
        dailyListUpdater.processDailyList(dailyList);

        assertThat(dailyList.getStatus()).isEqualTo(PROCESSED);
    }

    private DailyListEntity setUpDailyList(String filename) throws IOException {
        String dailyListJson = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListUpdaterTest/" + filename);
        dailyListJson = dailyListJson.replace("todays_date", today.toString());

        DailyListEntity dailyList = new DailyListEntity();
        dailyList.setId(1);
        dailyList.setSource("CPP");
        dailyList.setContent(dailyListJson);
        return dailyList;
    }

}