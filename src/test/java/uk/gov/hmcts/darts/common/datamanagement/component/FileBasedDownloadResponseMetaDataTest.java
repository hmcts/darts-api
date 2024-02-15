package uk.gov.hmcts.darts.common.datamanagement.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

class FileBasedDownloadResponseMetaDataTest {
    @Test
    void testFileMetaData() throws Exception {
        StorageConfiguration configuration = new StorageConfiguration();
        configuration.setTempBlobWorkspace(System.getenv("java.home") + "/" + "temp");

        int fileCount;
        String byteToWrite = "test";
        try (FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData()) {
            try (OutputStream outputStream = fileBasedDownloadResponseMetaData.getOutputStream(configuration)) {
                outputStream.write(byteToWrite.getBytes());

                fileCount = new File(configuration.getTempBlobWorkspace()).list().length;

                Assertions.assertFalse(fileBasedDownloadResponseMetaData.isProcessedByContainer());
                Assertions.assertFalse(fileBasedDownloadResponseMetaData.isSuccessfulDownload());

                fileBasedDownloadResponseMetaData.markSuccess(DatastoreContainerType.ARM);

                Assertions.assertTrue(fileBasedDownloadResponseMetaData.isSuccessfulDownload());
                Assertions.assertEquals(DatastoreContainerType.ARM, fileBasedDownloadResponseMetaData.getContainerTypeUsedToDownload());

                fileBasedDownloadResponseMetaData.markFailure(DatastoreContainerType.ARM);

                Assertions.assertFalse(fileBasedDownloadResponseMetaData.isSuccessfulDownload());
                Assertions.assertEquals(DatastoreContainerType.ARM, fileBasedDownloadResponseMetaData.getContainerTypeUsedToDownload());

                // ensure we do not generate new is or os
                Assertions.assertSame(fileBasedDownloadResponseMetaData.getInputStream(),
                                      fileBasedDownloadResponseMetaData.getInputStream());
                Assertions.assertSame(fileBasedDownloadResponseMetaData.getOutputStream(configuration),
                                      fileBasedDownloadResponseMetaData.getOutputStream(configuration));
                Assertions.assertEquals(byteToWrite, new String(fileBasedDownloadResponseMetaData.getInputStream().readAllBytes()));

                try (FileInputStream fis = Mockito.mock(FileInputStream.class)) {
                    fileBasedDownloadResponseMetaData.markInputStream(fis);
                    Assertions.assertSame(fis, fileBasedDownloadResponseMetaData.getInputStream());
                }
            }
        }

        // assert the file gets cleared up
        int fileCountPostCleanup = new File(configuration.getTempBlobWorkspace()).list().length;
        Assertions.assertEquals(fileCount - 1, fileCountPostCleanup);
    }
}