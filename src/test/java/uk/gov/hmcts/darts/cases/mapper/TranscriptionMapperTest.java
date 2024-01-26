package uk.gov.hmcts.darts.cases.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptionMapperTest {

    @Test
    void happyPath() {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1);

        List<Transcript> transcripts = TranscriptionMapper.mapResponse(transcriptionList);
        Transcript transcript = transcripts.get(0);
        assertEquals(1, transcript.getTranscriptionId());
        assertEquals(102, transcript.getHearingId());
        assertEquals(LocalDate.of(2023, 6, 20), transcript.getHearingDate());
        assertEquals("SENTENCING_REMARKS", transcript.getType());
        assertEquals(OffsetDateTime.of(2020, 6, 20, 10, 10, 0, 0, ZoneOffset.UTC), transcript.getRequestedOn());
        assertEquals("testUsername", transcript.getRequestedByName());
        assertEquals("APPROVED", transcript.getStatus());
    }

    @Test
    void happyPathLegacy() {
        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.addCase(CommonTestDataUtil.createCase("theCase"));
        TranscriptionTypeEntity transcriptionType = new TranscriptionTypeEntity();
        transcriptionType.setId(1);
        transcriptionType.setDescription(TranscriptionTypeEnum.SENTENCING_REMARKS.name());
        transcription.setTranscriptionType(transcriptionType);
        transcription.setCreatedDateTime(OffsetDateTime.of(2020, 6, 20, 10, 10, 0, 0, ZoneOffset.UTC));
        transcription.setId(1);
        transcription.setHearingDate(LocalDate.of(2023, 6, 20));
        transcription.setRequestor("someLegacyRequestor");

        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(TranscriptionStatusEnum.APPROVED.getId());
        transcriptionStatus.setStatusType(TranscriptionStatusEnum.APPROVED.name());
        transcription.setTranscriptionStatus(transcriptionStatus);

        List<Transcript> transcripts = TranscriptionMapper.mapResponse(List.of(transcription));
        Transcript transcript = transcripts.get(0);
        assertEquals(1, transcript.getTranscriptionId());
        assertEquals(LocalDate.of(2023, 6, 20), transcript.getHearingDate());
        assertEquals("SENTENCING_REMARKS", transcript.getType());
        assertEquals(OffsetDateTime.of(2020, 6, 20, 10, 10, 0, 0, ZoneOffset.UTC), transcript.getRequestedOn());
        assertEquals("someLegacyRequestor", transcript.getRequestedByName());
        assertEquals("APPROVED", transcript.getStatus());
    }
}
