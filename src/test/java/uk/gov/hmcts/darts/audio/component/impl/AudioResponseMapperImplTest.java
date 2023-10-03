package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioResponse;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.model.AudioRequestType.DOWNLOAD;

class AudioResponseMapperImplTest {

    private static final OffsetDateTime START_TIME = OffsetDateTime.now();
    private static final OffsetDateTime END_TIME = START_TIME.plusHours(1);
    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10Z");
    private static final String COURTHOUSE = "SWANSEA";
    private static final String CASE_NUMBER = "T20190024";
    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 1, 1);
    private static final int CASE_ID = 123;

    private AudioResponseMapper audioResponseMapper;

    @BeforeEach
    void setUp() {
        audioResponseMapper = new AudioResponseMapperImpl();
    }

    @Test
    void mapToAudioMetadataShouldMapToExpectedStructure() {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);

        List<AudioMetadata> mappedAudioMetadatas = audioResponseMapper.mapToAudioMetadata(Collections.singletonList(mediaEntity));

        assertEquals(mappedAudioMetadatas.size(), 1);
        AudioMetadata mappedAudioMetaData = mappedAudioMetadatas.get(0);

        assertEquals(1, mappedAudioMetaData.getId());
        assertEquals(START_TIME, mappedAudioMetaData.getMediaStartTimestamp());
        assertEquals(END_TIME, mappedAudioMetaData.getMediaEndTimestamp());
    }

    @Test
    void mapToAddAudioResponseShouldMapToExpectedStructure() {
        MediaRequestEntity audioRequest = createDummyMediaRequestEntity();

        AddAudioResponse addAudioResponse = audioResponseMapper.mapToAddAudioResponse(audioRequest);

        assertEquals(2023, addAudioResponse.getRequestId());
        assertEquals(CASE_ID, addAudioResponse.getCaseId());
        assertEquals(CASE_NUMBER, addAudioResponse.getCaseNumber());
        assertEquals(COURTHOUSE, addAudioResponse.getCourthouseName());
        assertEquals(HEARING_DATE, addAudioResponse.getHearingDate());
        assertEquals(TIME_12_00, addAudioResponse.getStartTime());
        assertEquals(TIME_12_10, addAudioResponse.getEndTime());

    }

    private MediaRequestEntity createDummyMediaRequestEntity() {

        HearingEntity mockHearingEntity = mock(HearingEntity.class);
        CourtCaseEntity mockCourtCaseEntity = mock(CourtCaseEntity.class);
        CourtroomEntity mockCourtroomEntity = mock(CourtroomEntity.class);
        when(mockHearingEntity.getCourtroom()).thenReturn(mockCourtroomEntity);
        when(mockHearingEntity.getCourtCase()).thenReturn(mockCourtCaseEntity);
        when(mockHearingEntity.getHearingDate()).thenReturn(HEARING_DATE);

        CourthouseEntity mockCourthouseEntity = mock(CourthouseEntity.class);
        when(mockCourtroomEntity.getCourthouse()).thenReturn(mockCourthouseEntity);
        when(mockCourthouseEntity.getCourthouseName()).thenReturn(COURTHOUSE);
        when(mockCourtCaseEntity.getCaseNumber()).thenReturn(CASE_NUMBER);
        when(mockCourtCaseEntity.getId()).thenReturn(CASE_ID);
        UserAccountEntity mockUserAccountEntity = mock(UserAccountEntity.class);

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setId(2023);
        mediaRequestEntity.setHearing(mockHearingEntity);

        mediaRequestEntity.setStartTime(TIME_12_00);
        mediaRequestEntity.setEndTime(TIME_12_10);
        mediaRequestEntity.setRequestor(mockUserAccountEntity);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setCreatedDateTime(START_TIME);
        mediaRequestEntity.setCreatedBy(mockUserAccountEntity);
        mediaRequestEntity.setLastModifiedDateTime(START_TIME);
        mediaRequestEntity.setLastModifiedBy(mockUserAccountEntity);
        return mediaRequestEntity;
    }
}
