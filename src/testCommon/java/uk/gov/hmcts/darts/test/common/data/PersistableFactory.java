package uk.gov.hmcts.darts.test.common.data;


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
}