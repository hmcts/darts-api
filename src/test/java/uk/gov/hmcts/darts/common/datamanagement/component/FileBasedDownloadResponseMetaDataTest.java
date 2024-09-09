package uk.gov.hmcts.darts.common.datamanagement.component;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

class FileBasedDownloadResponseMetaDataTest {

    @TempDir
    private File tempDirectory;

    @Test
    void testFileMetaData() throws Exception {
        StorageConfiguration configuration = new StorageConfiguration();
        configuration.setTempBlobWorkspace(tempDirectory.getAbsolutePath());

        int fileCount;
        String byteToWrite = "test";
        try (FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData()) {
            try (OutputStream outputStream = fileBasedDownloadResponseMetaData.getOutputStream(configuration)) {
                outputStream.write(byteToWrite.getBytes());

                fileCount = new File(configuration.getTempBlobWorkspace()).list().length;

                fileBasedDownloadResponseMetaData.setContainerTypeUsedToDownload(DatastoreContainerType.ARM);

                Assertions.assertEquals(DatastoreContainerType.ARM, fileBasedDownloadResponseMetaData.getContainerTypeUsedToDownload());

                // ensure we do not generate new is or os
                Assertions.assertSame(fileBasedDownloadResponseMetaData.getOutputStream(configuration),
                                      fileBasedDownloadResponseMetaData.getOutputStream(configuration));
                Assertions.assertEquals(byteToWrite, new String(fileBasedDownloadResponseMetaData.getResource().getInputStream().readAllBytes()));
            }
        }

        // assert the file gets cleared up
        int fileCountPostCleanup = new File(configuration.getTempBlobWorkspace()).list().length;
        Assertions.assertEquals(fileCount - 1, fileCountPostCleanup);
    }

    @Test
    void testFileMetaDataSetInputStream() throws Exception {

        try (FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData()) {
            StorageConfiguration configuration = new StorageConfiguration();
            configuration.setTempBlobWorkspace(tempDirectory.getAbsolutePath());
            int fileCountPostCleanupBefore = new File(configuration.getTempBlobWorkspace()).list().length;
            fileBasedDownloadResponseMetaData.setInputStream(new ByteArrayInputStream("test".getBytes()), configuration);

            Assertions.assertEquals(fileCountPostCleanupBefore + 1, new File(configuration.getTempBlobWorkspace()).list().length);

            try (InputStream inputStream = fileBasedDownloadResponseMetaData.getResource().getInputStream()) {
                String content = IOUtils.toString(inputStream);
                Assertions.assertEquals("test", content);
            }

            int fileCountPostCleanup = new File(configuration.getTempBlobWorkspace()).list().length;
            Assertions.assertEquals(fileCountPostCleanupBefore, fileCountPostCleanup);
        }
    }
}