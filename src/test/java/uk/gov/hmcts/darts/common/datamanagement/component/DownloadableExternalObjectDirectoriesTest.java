package uk.gov.hmcts.darts.common.datamanagement.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.Arrays;

class DownloadableExternalObjectDirectoriesTest {

    @Test
    void testGetDownloadableExternalObjectDirectories() throws Exception {
        ExternalObjectDirectoryEntity entity = Mockito.mock(ExternalObjectDirectoryEntity.class);
        DownloadableExternalObjectDirectories download =  DownloadableExternalObjectDirectories.getFileBasedDownload(Arrays.asList(entity));

        try (DownloadResponseMetaData metaData = download.getResponse()) {
            String content = "content";

            StorageConfiguration configuration = new StorageConfiguration();
            String location = System.getProperty("user.home");
            configuration.setTempBlobWorkspace(location);

            metaData.getOutputStream(configuration).write(content.getBytes());
            metaData.markSuccess(DatastoreContainerType.ARM);

            Assertions.assertTrue(metaData.isProcessedByContainer());
            Assertions.assertEquals(1, download.getEntities().size());
            Assertions.assertEquals(content, new String(metaData.getInputStream().readAllBytes()));
            Assertions.assertNotNull(metaData.getOutputStream(configuration));
            Assertions.assertEquals(DatastoreContainerType.ARM, metaData.getContainerTypeUsedToDownload());
        }
    }
}