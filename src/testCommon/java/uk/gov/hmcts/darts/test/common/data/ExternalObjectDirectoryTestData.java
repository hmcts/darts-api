package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.test.common.data.builder.TestExternalObjectDirectoryEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.ExternalLocationTypeTestData.locationTypeOf;
import static uk.gov.hmcts.darts.test.common.data.ObjectRecordStatusTestData.statusOf;

public final class ExternalObjectDirectoryTestData implements
    Persistable<TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryBuilderRetrieve, ExternalObjectDirectoryEntity,
        TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryEntityBuilder> {

    ExternalObjectDirectoryTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    /**
     * Deprectated.
     * Gets a minimal object directory
     *
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @Deprecated
    public ExternalObjectDirectoryEntity minimalExternalObjectDirectory() {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setStatus(statusOf(STORED));
        externalObjectDirectory.setExternalLocationType(locationTypeOf(UNSTRUCTURED));
        externalObjectDirectory.setExternalLocation(UUID.randomUUID().toString());
        externalObjectDirectory.setVerificationAttempts(1);
        externalObjectDirectory.setCreatedById(0);
        externalObjectDirectory.setLastModifiedById(0);
        return externalObjectDirectory;
    }

    @Override
    public ExternalObjectDirectoryEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }


    public ExternalObjectDirectoryEntity eodStoredInUnstructuredLocationForMedia(MediaEntity media) {
        TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryBuilderRetrieve eod = someMinimalBuilderHolder();
        TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryEntityBuilder builder = eod.getBuilder();

        builder.media(media).status(statusOf(STORED))
            .externalLocationType(locationTypeOf(UNSTRUCTURED))
            .externalLocation(UUID.randomUUID().toString());
        return eod.build().getEntity();
    }


    public ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum locationTypeEnum,
                                                                                 MediaEntity media) {
        var eod = minimalExternalObjectDirectory();
        eod.setMedia(media);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(UUID.randomUUID().toString());
        return eod;
    }

    public ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForAnnotationDocument(ExternalLocationTypeEnum locationTypeEnum,
                                                                                              AnnotationDocumentEntity annotationDocument) {
        var eod = minimalExternalObjectDirectory();
        eod.setAnnotationDocumentEntity(annotationDocument);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(UUID.randomUUID().toString());
        return eod;
    }

    public ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForTranscriptionDocument(ExternalLocationTypeEnum locationTypeEnum,
                                                                                                 TranscriptionDocumentEntity transcriptionDocument) {
        var eod = minimalExternalObjectDirectory();
        eod.setTranscriptionDocumentEntity(transcriptionDocument);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(UUID.randomUUID().toString());
        return eod;
    }

    public ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForCaseDocument(ExternalLocationTypeEnum locationTypeEnum,
                                                                                        CaseDocumentEntity caseDocument) {
        var eod = minimalExternalObjectDirectory();
        eod.setCaseDocument(caseDocument);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(UUID.randomUUID().toString());
        return eod;
    }

    @Override
    public TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryBuilderRetrieve someMinimalBuilderHolder() {
        TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryBuilderRetrieve builder
            = new TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryBuilderRetrieve();

        builder.getBuilder()
            .media(PersistableFactory.getMediaTestData().someMinimal())
            .status(ObjectRecordStatusTestData.statusOf(STORED))
            .externalLocationType(locationTypeOf(UNSTRUCTURED))
            .externalLocation(UUID.randomUUID().toString())
            .verificationAttempts(0)
            .createdById(0)
            .lastModifiedById(0)
            .createdDateTime(OffsetDateTime.now())
            .lastModifiedDateTime(OffsetDateTime.now());

        return builder;
    }

    @Override
    public TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}