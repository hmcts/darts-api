package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.component.AudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AddAudioResponse;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;

@ExtendWith(MockitoExtension.class)
class AudioRequestResponseMapperImplTest {

    private static final OffsetDateTime START_TIME = OffsetDateTime.now();
    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10Z");
    private static final String COURTHOUSE = "SWANSEA";
    private static final String CASE_NUMBER = "T20190024";
    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 1, 1);
    private static final int CASE_ID = 123;

    private AudioRequestResponseMapper audioRequestResponseMapper;

    @BeforeEach
    void setUp() {
        audioRequestResponseMapper = new AudioRequestResponseMapperImpl();
    }

    @Test
    void mapToAddAudioResponseShouldMapToExpectedStructure() {
        MediaRequestEntity audioRequest = createDummyMediaRequestEntity();

        AddAudioResponse addAudioResponse = audioRequestResponseMapper.mapToAddAudioResponse(audioRequest);

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
