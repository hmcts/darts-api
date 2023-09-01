package uk.gov.hmcts.darts.hearings.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetHearingResponseMapperTest {

    @Test
    void okMapper() {
        HearingEntity hearing = CommonTestDataUtil.createHearing("TestCase", LocalTime.of(10, 0, 0));

        GetHearingResponse response = GetHearingResponseMapper.map(hearing);
        assertEquals(response.getHearingId(), 102);
        assertEquals(response.getCourthouse(), "SWANSEA");
        assertEquals(response.getCourtroom(), "1");
        assertEquals(response.getHearingDate(), LocalDate.of(2023, 6, 20));
        assertEquals(response.getCaseNumber(), "TestCase");
        assertEquals(response.getCaseId(), 101);
        assertEquals(response.getJudges(), List.of("Judge_1", "Judge_2"));
        assertEquals(response.getTranscriptionCount(), 1);
    }
}
