package uk.gov.hmcts.darts.annotation.builders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
class ExternalObjectDirectoryBuilderTest {

    private static final String SOME_EXTERNAL_LOCATION = UUID.randomUUID().toString();
    private static final ObjectRecordStatusEntity OBJECT_RECORD_STATUS = new ObjectRecordStatusEntity();
    private static final ExternalLocationTypeEntity EXTERNAL_LOCATION_TYPE = new ExternalLocationTypeEntity();

    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;

    private ExternalObjectDirectoryBuilder externalObjectDirectoryBuilder;

    @BeforeEach
    void setUp() {
        externalObjectDirectoryBuilder = new ExternalObjectDirectoryBuilder(objectRecordStatusRepository, externalLocationTypeRepository);
    }

    @ParameterizedTest
    @EnumSource(ExternalLocationTypeEnum.class)
    void buildsExternalObjectDirectoryCorrectly(ExternalLocationTypeEnum externalLocationType) {
        when(objectRecordStatusRepository.getReferenceById(STORED.getId())).thenReturn(OBJECT_RECORD_STATUS);
        when(externalLocationTypeRepository.getReferenceById(any())).thenReturn(EXTERNAL_LOCATION_TYPE);
        var annotationDocumentEntity = someAnnotationDocument();

        assertThat(externalObjectDirectoryBuilder.buildFrom(annotationDocumentEntity, SOME_EXTERNAL_LOCATION, externalLocationType))
            .hasFieldOrPropertyWithValue("status", OBJECT_RECORD_STATUS)
            .hasFieldOrPropertyWithValue("externalLocationType", EXTERNAL_LOCATION_TYPE)
            .hasFieldOrPropertyWithValue("externalLocation", SOME_EXTERNAL_LOCATION)
            .hasFieldOrPropertyWithValue("checksum", annotationDocumentEntity.getChecksum())
            .hasFieldOrPropertyWithValue("createdById", annotationDocumentEntity.getUploadedBy().getId())
            .hasFieldOrPropertyWithValue("verificationAttempts", 1)
            .hasFieldOrPropertyWithValue("lastModifiedById", annotationDocumentEntity.getUploadedBy().getId());

        verify(externalLocationTypeRepository, times(1)).getReferenceById(externalLocationType.getId());
    }

    private AnnotationDocumentEntity someAnnotationDocument() {
        var annotationDocument = new AnnotationDocumentEntity();
        annotationDocument.setChecksum("some-checksum");
        annotationDocument.setUploadedBy(new UserAccountEntity());
        return annotationDocument;
    }
}
