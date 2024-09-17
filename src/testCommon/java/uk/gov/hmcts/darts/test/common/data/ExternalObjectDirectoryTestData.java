package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;

import static java.util.UUID.randomUUID;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.ExternalLocationTypeTestData.locationTypeOf;
import static uk.gov.hmcts.darts.test.common.data.ObjectRecordStatusTestData.statusOf;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class ExternalObjectDirectoryTestData {

    public static ExternalObjectDirectoryEntity minimalExternalObjectDirectory() {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setStatus(statusOf(STORED));
        externalObjectDirectory.setExternalLocationType(locationTypeOf(UNSTRUCTURED));
        externalObjectDirectory.setExternalLocation(randomUUID());
        externalObjectDirectory.setVerificationAttempts(1);
        var userAccount = minimalUserAccount();
        externalObjectDirectory.setCreatedBy(userAccount);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        return externalObjectDirectory;
    }

    public static ExternalObjectDirectoryEntity eodStoredInUnstructuredLocationForMedia(MediaEntity media) {
        var eod = minimalExternalObjectDirectory();
        eod.setMedia(media);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(UNSTRUCTURED));
        eod.setExternalLocation(randomUUID());
        return eod;
    }

    public static ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum locationTypeEnum,
                                                                                        MediaEntity media) {
        var eod = minimalExternalObjectDirectory();
        eod.setMedia(media);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(randomUUID());
        return eod;
    }

    public static ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForAnnotationDocument(ExternalLocationTypeEnum locationTypeEnum,
                                                                                                     AnnotationDocumentEntity annotationDocument) {
        var eod = minimalExternalObjectDirectory();
        eod.setAnnotationDocumentEntity(annotationDocument);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(randomUUID());
        return eod;
    }

    public static ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForTranscriptionDocument(ExternalLocationTypeEnum locationTypeEnum,
                                                                                                        TranscriptionDocumentEntity transcriptionDocument) {
        var eod = minimalExternalObjectDirectory();
        eod.setTranscriptionDocumentEntity(transcriptionDocument);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(randomUUID());
        return eod;
    }

    public static ExternalObjectDirectoryEntity eodStoredInExternalLocationTypeForCaseDocument(ExternalLocationTypeEnum locationTypeEnum,
                                                                                               CaseDocumentEntity caseDocument) {
        var eod = minimalExternalObjectDirectory();
        eod.setCaseDocument(caseDocument);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(locationTypeEnum));
        eod.setExternalLocation(randomUUID());
        return eod;
    }


}
