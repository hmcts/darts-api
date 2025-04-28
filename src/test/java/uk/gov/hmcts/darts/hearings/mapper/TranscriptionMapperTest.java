package uk.gov.hmcts.darts.hearings.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.model.Transcript;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptionMapperTest {

    private HearingTranscriptionMapper hearingTranscriptionMapper;

    @BeforeEach
    public void beforeTest() {
        hearingTranscriptionMapper = new HearingTranscriptionMapper();
    }

    @Test
    void happyPath() {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1);

        List<Transcript> transcripts = hearingTranscriptionMapper.getTranscriptList(hearingTranscriptionMapper.mapResponse(transcriptionList));
        Transcript transcript = transcripts.getFirst();
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
        transcription.setId(1L);
        transcription.setHearingDate(LocalDate.of(2023, 6, 20));
        transcription.setRequestedBy(CommonTestDataUtil.createUserAccount("someLegacyRequestor"));
        transcription.setIsCurrent(true);

        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(TranscriptionStatusEnum.APPROVED.getId());
        transcriptionStatus.setStatusType(TranscriptionStatusEnum.APPROVED.name());
        transcription.setTranscriptionStatus(transcriptionStatus);

        List<Transcript> transcripts = hearingTranscriptionMapper.getTranscriptList(hearingTranscriptionMapper.mapResponse(List.of(transcription)));
        Transcript transcript = transcripts.getFirst();
        assertEquals(1, transcript.getTranscriptionId());
        assertEquals(LocalDate.of(2023, 6, 20), transcript.getHearingDate());
        assertEquals("SENTENCING_REMARKS", transcript.getType());
        assertEquals(OffsetDateTime.of(2020, 6, 20, 10, 10, 0, 0, ZoneOffset.UTC), transcript.getRequestedOn());

        assertEquals("someLegacyRequestor", transcript.getRequestedByName());
        assertEquals("APPROVED", transcript.getStatus());
    }

    @Test
    void happyPathDoNotIncludeTranscriptionsIsCurrentFalse() {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1);

        TestUtils.getFirstLong(transcriptionList).setIsCurrent(false);

        List<Transcript> transcripts = hearingTranscriptionMapper.getTranscriptList(hearingTranscriptionMapper.mapResponse(transcriptionList));
        assertEquals(0, transcripts.size());
    }
}