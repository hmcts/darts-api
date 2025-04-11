package uk.gov.hmcts.darts.hearings.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GetHearingResponseMapperTest {

    @Autowired
    GetHearingResponseMapper getHearingResponseMapper;

    @Mock
    HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    @BeforeEach
    void beforeAll() {
        getHearingResponseMapper = new GetHearingResponseMapper(hearingReportingRestrictionsRepository);
        Mockito.when(hearingReportingRestrictionsRepository.findAllByCaseId(101)).thenReturn(Collections.emptyList());
    }

    @Test
    void okMapper() {
        HearingEntity hearing = CommonTestDataUtil.createHearing("TestCase", LocalTime.of(10, 0, 0));

        GetHearingResponse response = getHearingResponseMapper.map(hearing);
        assertEquals(response.getHearingId(), 102);
        assertEquals(response.getCourthouseId(), 1001);
        assertEquals(response.getCourthouse(), "SWANSEA");
        assertEquals(response.getCourtroom(), "1");
        assertEquals(response.getHearingDate(), LocalDate.of(2023, 6, 20));
        assertEquals(response.getCaseNumber(), "TestCase");
        assertEquals(response.getCaseId(), 101);
        assertThat(response.getJudges())
            .containsExactlyInAnyOrder("Judge_1", "Judge_2");
        assertEquals(response.getTranscriptionCount(), 1);
        assertEquals(0, response.getCaseReportingRestrictions().size());
    }

    @Test
    void getHearingResponseMapper_shouldNotIncludeNonCurrentTranscriptions() {
        HearingEntity hearing = CommonTestDataUtil.createHearing("TestCase", LocalTime.of(10, 0, 0));
        hearing.getTranscriptions().getFirst().setIsCurrent(false);

        GetHearingResponse response = getHearingResponseMapper.map(hearing);
        assertEquals(response.getTranscriptionCount(), 0);
    }
}
