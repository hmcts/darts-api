package uk.gov.hmcts.darts.common.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType.AUDIO_LINKING_TASK;

@ExtendWith(MockitoExtension.class)
class MediaLinkedCaseHelperTest {

    @Mock
    private MediaLinkedCaseRepository mediaLinkedCaseRepository;
    @Mock
    private HearingRepository hearingRepository;
    @InjectMocks
    @Spy
    private MediaLinkedCaseHelper mediaLinkedCaseHelper;


    @Test
    void positiveProcessMedia() {
        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        HearingEntity hearingEntity3 = mock(HearingEntity.class);


        when(hearingEntity1.containsMedia(any())).thenReturn(false);
        when(hearingEntity2.containsMedia(any())).thenReturn(false);
        when(hearingEntity3.containsMedia(any())).thenReturn(false);

        CourtCaseEntity courtCaseEntity1 = mock(CourtCaseEntity.class);
        CourtCaseEntity courtCaseEntity2 = mock(CourtCaseEntity.class);

        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity2);


        List<HearingEntity> hearingEntities = List.of(hearingEntity1, hearingEntity2, hearingEntity3);

        EventEntity event = mock(EventEntity.class);
        when(event.getHearingEntities()).thenReturn(hearingEntities);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        doNothing().when(mediaLinkedCaseHelper).addCase(any(), any(), any(), any());

        MediaEntity mediaEntity = mock(MediaEntity.class);
        mediaLinkedCaseHelper.linkMediaByEvent(event, mediaEntity, AUDIO_LINKING_TASK, userAccount);


        verify(hearingEntity1, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity2, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity3, times(1)).containsMedia(mediaEntity);

        verify(hearingEntity1, times(1)).addMedia(mediaEntity);
        verify(hearingEntity2, times(1)).addMedia(mediaEntity);
        verify(hearingEntity3, times(1)).addMedia(mediaEntity);

        verify(hearingEntity1, times(1)).getCourtCase();
        verify(hearingEntity2, times(1)).getCourtCase();
        verify(hearingEntity3, times(1)).getCourtCase();

        Set<HearingEntity> savedHearingEntities = new HashSet<>();
        savedHearingEntities.add(hearingEntity1);
        savedHearingEntities.add(hearingEntity2);
        savedHearingEntities.add(hearingEntity3);
        verify(hearingRepository, times(1))
            .saveAll(savedHearingEntities);

        Set<MediaLinkedCaseEntity> savedMediaLinkedCaseEntity = new HashSet<>();
        savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity1, userAccount, AUDIO_LINKING_TASK));
        savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity2, userAccount, AUDIO_LINKING_TASK));

        verify(mediaLinkedCaseHelper, times(2)).addCase(mediaEntity, courtCaseEntity1, AUDIO_LINKING_TASK, userAccount);
        verify(mediaLinkedCaseHelper).addCase(mediaEntity, courtCaseEntity2, AUDIO_LINKING_TASK, userAccount);
    }


    @Test
    void positiveProcessMediaHearingAlreadyContainsMedia() {
        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        HearingEntity hearingEntity3 = mock(HearingEntity.class);


        when(hearingEntity1.containsMedia(any())).thenReturn(false);
        when(hearingEntity2.containsMedia(any())).thenReturn(true);
        when(hearingEntity3.containsMedia(any())).thenReturn(true);

        CourtCaseEntity courtCaseEntity1 = mock(CourtCaseEntity.class);
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);

        List<HearingEntity> hearingEntities = List.of(hearingEntity1, hearingEntity2, hearingEntity3);

        EventEntity event = mock(EventEntity.class);
        when(event.getHearingEntities()).thenReturn(hearingEntities);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        doNothing().when(mediaLinkedCaseHelper).addCase(any(), any(), any(), any());

        MediaEntity mediaEntity = mock(MediaEntity.class);
        mediaLinkedCaseHelper.linkMediaByEvent(event, mediaEntity, AUDIO_LINKING_TASK, userAccount);

        verify(hearingEntity1, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity2, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity3, times(1)).containsMedia(mediaEntity);

        verify(hearingEntity1, times(1)).addMedia(mediaEntity);
        verify(hearingEntity2, never()).addMedia(mediaEntity);
        verify(hearingEntity3, never()).addMedia(mediaEntity);

        verify(hearingEntity1, times(1)).getCourtCase();
        verify(hearingEntity2, never()).getCourtCase();
        verify(hearingEntity3, never()).getCourtCase();

        Set<HearingEntity> savedHearingEntities = new HashSet<>();
        savedHearingEntities.add(hearingEntity1);
        verify(hearingRepository, times(1))
            .saveAll(savedHearingEntities);

        verify(mediaLinkedCaseHelper).addCase(mediaEntity, courtCaseEntity1, AUDIO_LINKING_TASK, userAccount);
    }
}
