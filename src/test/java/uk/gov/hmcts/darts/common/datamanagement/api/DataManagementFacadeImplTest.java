package uk.gov.hmcts.darts.common.datamanagement.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

class DataManagementFacadeImplTest {
    @Test
     void testDownloadOfFacadeWithArm() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, true));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);

        ExternalObjectDirectoryEntity dets = setupEntityPayload(ExternalLocationTypeEnum.ARM, true);

        Collection<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);
        DownloadableExternalObjectDirectories downloadForExternalObjectDirectories
                = DownloadableExternalObjectDirectories.getFileBasedDownload(entitiesToDownload);

        try (OutputStream osFile = downloadForExternalObjectDirectories.getResponse().getOutputStream()) {
            osFile.write("test".getBytes());
        }

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, configuration);

        // make the assertion on the response
        dmFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadForExternalObjectDirectories);
        assertResponse(downloadForExternalObjectDirectories.getResponse(), true, true,
                       "test", DatastoreContainerType.ARM);
    }

    @Test
    void testDownloadOfFacadeWithUnstructured() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.UNSTRUCTURED, true));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);

        ExternalObjectDirectoryEntity dets = setupEntityPayload(ExternalLocationTypeEnum.UNSTRUCTURED, true);

        Collection<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);
        DownloadableExternalObjectDirectories downloadForExternalObjectDirectories
                = DownloadableExternalObjectDirectories.getFileBasedDownload(entitiesToDownload);

        try (OutputStream osFile = downloadForExternalObjectDirectories.getResponse().getOutputStream()) {
            osFile.write("test".getBytes());
        }

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, configuration);

        // make the assertion on the response
        dmFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadForExternalObjectDirectories);
        assertResponse(downloadForExternalObjectDirectories.getResponse(), true, true,
                       "test", DatastoreContainerType.UNSTRUCTURED);
    }

    @Test
    void testDownloadOfFacadeWithDetsEnabled() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.DETS, true));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);

        ExternalObjectDirectoryEntity dets = setupEntityPayload(ExternalLocationTypeEnum.TEMPSTORE, true);

        Collection<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);
        DownloadableExternalObjectDirectories downloadForExternalObjectDirectories
                = DownloadableExternalObjectDirectories.getFileBasedDownload(entitiesToDownload);

        try (OutputStream osFile = downloadForExternalObjectDirectories.getResponse().getOutputStream()) {
            osFile.write("test".getBytes());
        }

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, configuration);

        // make the assertion on the response
        dmFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadForExternalObjectDirectories);
        assertResponse(downloadForExternalObjectDirectories.getResponse(), true, true,
                       "test", DatastoreContainerType.DETS);
    }

    @Test
    void testDownloadOfFacadeWithDetsDisabled() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.DETS, false));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(false);

        ExternalObjectDirectoryEntity dets = setupEntityPayload(ExternalLocationTypeEnum.TEMPSTORE, true);

        Collection<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);
        DownloadableExternalObjectDirectories downloadForExternalObjectDirectories
                = DownloadableExternalObjectDirectories.getFileBasedDownload(entitiesToDownload);

        try (OutputStream osFile = downloadForExternalObjectDirectories.getResponse().getOutputStream()) {
            osFile.write("test".getBytes());
        }

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, configuration);

        // make the assertion on the response
        dmFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadForExternalObjectDirectories);
        assertResponse(downloadForExternalObjectDirectories.getResponse(), false, false, null,
                       null);
    }

    @Test
    void testDownloadOfFacadeWithNoneProcessed() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.INBOUND, false));

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        Mockito.when(downloadable.getContainerName(DatastoreContainerType.INBOUND)).thenReturn(Optional.of("test"));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);
        Mockito.when(downloadable
                             .downloadBlobFromContainer(Mockito.notNull(),
                                                        Mockito.notNull(), Mockito.notNull())).thenReturn(false);

        // create the payload to be tested
        ExternalObjectDirectoryEntity inboundEntity = setupEntityPayload(ExternalLocationTypeEnum.INBOUND, true);

        Collection<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(inboundEntity);
        DownloadableExternalObjectDirectories downloadForExternalObjectDirectories
                = DownloadableExternalObjectDirectories.getFileBasedDownload(entitiesToDownload);

        try (OutputStream osFile = downloadForExternalObjectDirectories.getResponse().getOutputStream()) {
            osFile.write("test".getBytes());
        }

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, configuration);

        // make the assertion on the response
        dmFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadForExternalObjectDirectories);
        assertResponse(downloadForExternalObjectDirectories.getResponse(), false, false, null,
                      null);
    }

    @Test
    void testDownloadOfFacadeWithUnstructuredAndArmProcessedInPriorityOrder() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        // setup the containers to use
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.UNSTRUCTURED, true));
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        // create the payload entities to be tested
        ExternalObjectDirectoryEntity inboundEntity = setupEntityPayload(ExternalLocationTypeEnum.INBOUND, false);
        ExternalObjectDirectoryEntity armDirectoryEntity = setupEntityPayload(ExternalLocationTypeEnum.ARM, true);

        Collection<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(inboundEntity, armDirectoryEntity);
        DownloadableExternalObjectDirectories downloadForExternalObjectDirectories
                = DownloadableExternalObjectDirectories.getFileBasedDownload(entitiesToDownload);

        String outputString = "test";
        try (OutputStream osFile = downloadForExternalObjectDirectories.getResponse().getOutputStream()) {
            osFile.write(outputString.getBytes());
        }

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, configuration);

        // make the assertion on the response
        dmFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadForExternalObjectDirectories);
        assertResponse(downloadForExternalObjectDirectories.getResponse(), false, true, outputString,
                       DatastoreContainerType.ARM);
    }

    private void assertResponse(DownloadResponseMetaData responseMetaData, boolean success, boolean processed,
                                String content, DatastoreContainerType containerType) throws Exception {
        try (DownloadResponseMetaData responseMetaDataToAssert = responseMetaData) {
            boolean processedByContainer = responseMetaDataToAssert.isProcessedByContainer();
            boolean successDownload = responseMetaDataToAssert.isSuccessfullyDownloaded();

            try (InputStream streamOfDownloadedData = responseMetaDataToAssert.getInputStream()) {
                if (content != null) {
                    Assertions.assertEquals(content, new String(streamOfDownloadedData.readAllBytes()));
                    Assertions.assertNotNull(streamOfDownloadedData);
                }
            }

            Assertions.assertEquals(processed, processedByContainer);
            Assertions.assertEquals(success, successDownload);
            Assertions.assertNotNull(responseMetaDataToAssert.getOutputStream());

            if (containerType != null) {
                Assertions.assertEquals(containerType, responseMetaDataToAssert.getContainerTypeUsedToDownload());
            }
        }
    }

    private BlobContainerDownloadable setupDownloadableContainer(DatastoreContainerType containerType,
                                                                  boolean processSuccess) throws Exception {
        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);

        Mockito.when(downloadable.getContainerName(containerType)).thenReturn(Optional.of("test"));
        Mockito.when(downloadable
                             .downloadBlobFromContainer(Mockito.notNull(),
                                                        Mockito.notNull(), Mockito.notNull())).thenReturn(processSuccess);
        return downloadable;
    }

    private ExternalObjectDirectoryEntity setupEntityPayload(ExternalLocationTypeEnum type, boolean match) {
        ExternalObjectDirectoryEntity entity = Mockito.mock(ExternalObjectDirectoryEntity.class);
        ExternalLocationTypeEntity locationTypeEntity = Mockito.mock(ExternalLocationTypeEntity.class);
        Mockito.when(entity.getExternalLocationType()).thenReturn(locationTypeEntity);
        Mockito.when(locationTypeEntity.getId()).thenReturn(type.getId());
        Mockito.when(entity.isForLocationType(type)).thenReturn(match);
        return entity;
    }
}