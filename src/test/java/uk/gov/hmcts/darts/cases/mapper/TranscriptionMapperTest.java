package uk.gov.hmcts.darts.cases.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptionMapperTest {

    @Test
    void happyPath() {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1);

        List<Transcript> transcripts = TranscriptionMapper.mapResponse(transcriptionList);
        Transcript transcript = transcripts.get(0);
        assertEquals(1, transcript.getTraId());
        assertEquals(102, transcript.getHeaId());
        assertEquals(LocalDate.of(2023, 6, 20), transcript.getHearingDate());
        assertEquals("SENTENCING_REMARKS", transcript.getType());
        assertEquals(LocalDate.of(2020, 6, 20), transcript.getRequestedOn());
        assertEquals("testUsername", transcript.getRequestedByName());
        assertEquals("APPROVED", transcript.getStatus());
    }
}
