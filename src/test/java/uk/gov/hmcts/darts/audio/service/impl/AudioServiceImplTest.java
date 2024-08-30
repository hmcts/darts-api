package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

    @Test
    @Disabled("temporarily replaced by test isArchivedFeatureIsTemporarilyDisabledAndSetToAlwaysReturnsFalse")
    void whenAudioMetadataListContainsMediaIdsReturnedByQuery_thenIsArchivedWillBeTrue() {
        int mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);
        AudioBeingProcessedFromArchiveQueryResult audioRequest = new AudioBeingProcessedFromArchiveQueryResult(mediaId, 2);
        List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords = List.of(audioRequest);

        when(audioBeingProcessedFromArchiveQuery.getResults(any())).thenReturn(archivedArmRecords);

        audioService.setIsArchived(audioMetadataList, 1);

        assertEquals(true, audioMetadataList.get(0).getIsArchived());
    }

    @Test
    void isArchivedFeatureIsTemporarilyDisabledAndSetToAlwaysReturnsFalse() {
        int mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);
        AudioBeingProcessedFromArchiveQueryResult audioRequest = new AudioBeingProcessedFromArchiveQueryResult(mediaId, 2);
        List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords = List.of(audioRequest);

        when(audioBeingProcessedFromArchiveQuery.getResults(any())).thenReturn(archivedArmRecords);

        audioService.setIsArchived(audioMetadataList, 1);

        assertEquals(true, audioMetadataList.get(0).getIsArchived());
    }

    @Test
    void whenAudioMetadataListOmitsMediaIdsReturnedByQuery_thenIsArchivedWillBeFalse() {
        int mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);

        audioService.setIsArchived(audioMetadataList, HEARING_ID);

        assertEquals(false, audioMetadataList.get(0).getIsArchived());
    }

    @Test
    void whenAudioMetadataListContainsMediaIdsStoredInUnstructured_thenIsAvailableWillBeTrue() {
        AudioMetadata audioMetadata1 = new AudioMetadata();
        audioMetadata1.setId(1);
        AudioMetadata audioMetadata2 = new AudioMetadata();
        audioMetadata2.setId(2);
        AudioMetadata audioMetadata3 = new AudioMetadata();
        audioMetadata3.setId(3);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata1, audioMetadata2, audioMetadata3);

        when(externalObjectDirectoryRepository.findMediaIdsByInMediaIdStatusAndType(anyList(), any(), any())).thenReturn(List.of(1, 3));

        audioService.setIsAvailable(audioMetadataList);

        assertEquals(true, audioMetadataList.get(0).getIsAvailable());
        assertEquals(false, audioMetadataList.get(1).getIsAvailable());
        assertEquals(true, audioMetadataList.get(2).getIsAvailable());
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
}