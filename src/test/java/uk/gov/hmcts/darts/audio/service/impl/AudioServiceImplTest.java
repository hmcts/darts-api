package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.darts.audio.component.AudioBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioServiceImplTest {
    public static final int HEARING_ID = 1;
    @Mock
    private AudioTransformationService audioTransformationService;
    @Mock
    private AudioOperationService audioOperationService;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    AudioBeingProcessedFromArchiveQuery audioBeingProcessedFromArchiveQuery;
    @Mock
    private LogApi logApi;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private AudioService audioService;

    private EodHelperMocks eodHelperMocks;

    @BeforeEach
    void setUp() {
        audioService = new AudioServiceImpl(
            audioTransformationService,
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            mediaRepository,
            audioOperationService,
            fileOperationService,
            audioBeingProcessedFromArchiveQuery
        );
    }

    @AfterEach
    void afterEach() {
        if (eodHelperMocks != null) {
            eodHelperMocks.close();
        }
    }

    @Test
    void whenAudioMetadataListContainsMediaIdsReturnedByQuery_thenIsArchivedWillBeTrue() {
        long mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);
        AudioBeingProcessedFromArchiveQueryResult audioRequest = new AudioBeingProcessedFromArchiveQueryResult(mediaId, 2L);
        List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords = List.of(audioRequest);

        when(audioBeingProcessedFromArchiveQuery.getResults(any())).thenReturn(archivedArmRecords);

        audioService.setIsArchived(audioMetadataList, 1);

        assertEquals(true, audioMetadataList.getFirst().getIsArchived());
    }

    @Test
    void isArchivedFeatureIsTemporarilyDisabledAndSetToAlwaysReturnsFalse() {
        long mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);
        AudioBeingProcessedFromArchiveQueryResult audioRequest = new AudioBeingProcessedFromArchiveQueryResult(mediaId, 2L);
        List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords = List.of(audioRequest);

        when(audioBeingProcessedFromArchiveQuery.getResults(any())).thenReturn(archivedArmRecords);

        audioService.setIsArchived(audioMetadataList, 1);

        assertEquals(true, audioMetadataList.getFirst().getIsArchived());
    }

    @Test
    void whenAudioMetadataListOmitsMediaIdsReturnedByQuery_thenIsArchivedWillBeFalse() {
        long mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);

        audioService.setIsArchived(audioMetadataList, HEARING_ID);

        assertEquals(false, audioMetadataList.getFirst().getIsArchived());
    }

    @Test
    void whenAudioMetadataListContainsMediaIdsStoredInUnstructured_thenIsAvailableWillBeTrue() {
        AudioMetadata audioMetadata1 = new AudioMetadata();
        audioMetadata1.setId(1L);
        AudioMetadata audioMetadata2 = new AudioMetadata();
        audioMetadata2.setId(2L);
        AudioMetadata audioMetadata3 = new AudioMetadata();
        audioMetadata3.setId(3L);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata1, audioMetadata2, audioMetadata3);

        when(externalObjectDirectoryRepository.findMediaIdsByInMediaIdStatusAndType(anyList(), any(), any(), any())).thenReturn(List.of(1L, 3L));
        eodHelperMocks = new EodHelperMocks();
        eodHelperMocks.simulateInitWithMockedData();
        audioService.setIsAvailable(audioMetadataList);

        assertEquals(true, audioMetadataList.getFirst().getIsAvailable());
        assertEquals(false, audioMetadataList.get(1).getIsAvailable());
        assertEquals(true, audioMetadataList.get(2).getIsAvailable());
        verify(externalObjectDirectoryRepository).findMediaIdsByInMediaIdStatusAndType(
            List.of(1L, 2L, 3L),
            eodHelperMocks.getStoredStatus(),
            eodHelperMocks.getUnstructuredLocation(), eodHelperMocks.getDetsLocation());
    }

    @Test
    void whenAudioMetadataListIsEmpty_thenIsAvailableWontExecute() {
        audioService.setIsAvailable(Collections.emptyList());

        verifyNoInteractions(externalObjectDirectoryRepository);
    }

    @Test
    void whenAudioMetadataListIsNull_thenIsAvailableWontExecute() {
        audioService.setIsAvailable(null);

        verifyNoInteractions(externalObjectDirectoryRepository);
    }

    @Test
    void whenAudioMetadataListIsEmpty_thenIsArchivedWontExecute() {
        audioService.setIsArchived(Collections.emptyList(), HEARING_ID);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void whenAudioMetadataListIsNull_thenIsArchivedWontExecute() {
        audioService.setIsArchived(null, HEARING_ID);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void getMediaEntitiesByHearingAndLowestChannel_shouldReturnEmptyListWhenNoMediaFound() {
        doReturn(List.of()).when(mediaRepository).findAllByHearingIdAndIsCurrentTrue(HEARING_ID);
        List<MediaEntity> mediaEntities = audioService.getMediaEntitiesByHearingAndLowestChannel(HEARING_ID);
        assertThat(mediaEntities).isEmpty();
    }

    @Test
    void getMediaEntitiesByHearingAndLowestChannel_singleChannelGroupReturn_shouldReturnLowest() {
        MediaEntity media1 = mock(MediaEntity.class);
        MediaEntity media2 = mock(MediaEntity.class);

        doReturn(List.of(media1, media2))
            .when(mediaRepository).findAllByHearingIdAndIsCurrentTrue(HEARING_ID);

        List<MediaEntity> mediaEntities = audioService.getMediaEntitiesByHearingAndLowestChannel(HEARING_ID);
        assertThat(mediaEntities)
            .hasSize(2)
            .containsExactlyInAnyOrder(media1, media2);
        verify(mediaRepository).findAllByHearingIdAndIsCurrentTrue(HEARING_ID);
    }
}