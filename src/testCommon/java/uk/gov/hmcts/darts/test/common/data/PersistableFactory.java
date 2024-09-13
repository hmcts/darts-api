package uk.gov.hmcts.darts.test.common.data;


import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;

public class PersistableFactory {
    private static PersistableFactory instance;

    private PersistableFactory() {

    }

    public static synchronized PersistableFactory getInstance() {
        if (instance == null) {
            instance = new PersistableFactory();
        }
        return instance;
    }

    public static ExternalObjectDirectoryTestData getExternalObjectDirectoryTestData() {
        return new ExternalObjectDirectoryTestData();
    }

    public static MediaTestData getMediaTestData() {
        return new MediaTestData();
    }

    public static MediaRequestTestData getMediaRequestTestData() {
        return new MediaRequestTestData();
    }

    public static TranscriptionTestData getTranscriptionTestData() {
        return new TranscriptionTestData();
    }

    public static TranscriptionDocumentTestData getTranscriptionDocument() {
        return new TranscriptionDocumentTestData();
    }

    public static AnnotationTestData getAnnotationTestData() {
        return new AnnotationTestData();
    }

    public static AnnotationDocumentTestData getAnnotationDocumentTestData() {
        return new AnnotationDocumentTestData();
    }

    public static CaseTestData getCourtCaseTestData() {
        return new CaseTestData();
    }

    public static CaseDocumentTestData getCaseDocumentTestData() {
        return new CaseDocumentTestData();
    }
}