package uk.gov.hmcts.darts.test.common.data;

public final class PersistableFactory {
    private static PersistableFactory instance;

    private PersistableFactory() {
        // This constructor is intentionally empty. Nothing special is needed here.
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

    public static TranscriptionCommentTestData getTranscriptionCommentTestData() {
        return new TranscriptionCommentTestData();
    }

    public static TranscriptionWorkflowTestData getTranscriptionWorkflowTestData() {
        return new TranscriptionWorkflowTestData();
    }

    public static HearingTestData getHearingTestData() {
        return new HearingTestData();
    }

    public static ArmRpoExecutionDetailTestData getArmRpoExecutionDetailTestData() {
        return new ArmRpoExecutionDetailTestData();
    }

    public static RetentionConfidenceCategoryMapperTestData getRetentionConfidenceCategoryMapperTestData() {
        return new RetentionConfidenceCategoryMapperTestData();
    }

    public static UserAccountTestData getUserAccountTestData() {
        return new UserAccountTestData();
    }

    public static CourthouseTestData getCourthouseTestData() {
        return new CourthouseTestData();
    }

    public static CourtroomTestData getCourtroomTestData() {
        return new CourtroomTestData();
    }

    public static ObjectAdminActionTestData getObjectAdminActionTestData() {
        return new ObjectAdminActionTestData();
    }

    public static EventTestData getEventTestData() {
        return new EventTestData();
    }
}