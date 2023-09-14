package uk.gov.hmcts.darts.datamanagement;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

public class DataManagementApiTest extends IntegrationBase {

    @Autowired
    private DataManagementService mockDataManagementService;

    @Test
    void shouldGetMediaLocation() {
        given.setupTest();
        var externalObjectDirectoryEntity =
            given.externalObjectDirForMedia(given.getMediaEntity1());

        assertEquals(
            externalObjectDirectoryEntity.getExternalLocation(),

            audioTransformationService.getMediaLocation(given.getMediaEntity1()).get()
        );
    }

    @Test
    void shouldGetEmptyOptionalMediaLocationWhenNoExternalObjectDirectoryExists() {
        MediaEntity newMedia = new MediaEntity();
        newMedia.setCourtroom(somePersistedCourtroom());
        newMedia.setChannel(1);
        newMedia.setTotalChannels(4);
        newMedia.setStart(OffsetDateTime.parse("2023-07-04T10:00:00Z"));
        newMedia.setEnd(OffsetDateTime.parse("2023-07-04T11:00:00Z"));
        newMedia = dartsDatabase.save(newMedia);

        assertEquals(Optional.empty(), audioTransformationService.getMediaLocation(newMedia));
    }

    @Test
    void shouldGetMediaLocationWithWarningThatMultipleExistByStatusAndType() {

        MediaEntity newMedia = new MediaEntity();
        newMedia.setCourtroom(somePersistedCourtroom());
        newMedia.setChannel(1);
        newMedia.setTotalChannels(4);
        newMedia.setStart(OffsetDateTime.parse("2023-07-04T16:00:00Z"));
        newMedia.setEnd(OffsetDateTime.parse("2023-07-04T17:00:00Z"));
        newMedia = dartsDatabase.save(newMedia);

        ExternalLocationTypeEntity externalLocationTypeEntity =
            dartsDatabase.getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId());
        ObjectDirectoryStatusEntity objectDirectoryStatus =
            dartsDatabase.getObjectDirectoryStatusRepository().getReferenceById(STORED.getId());
        UUID externalLocation1 = UUID.randomUUID();
        UUID externalLocation2 = UUID.randomUUID();
        ExternalObjectDirectoryEntity externalObjectDirectory1 = externalObjectDirectoryStub.createExternalObjectDirectory(
            newMedia,
            objectDirectoryStatus,
            externalLocationTypeEntity,
            externalLocation1
        );
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(externalObjectDirectory1);

        ExternalObjectDirectoryEntity externalObjectDirectory2 = externalObjectDirectoryStub.createExternalObjectDirectory(
            newMedia,
            objectDirectoryStatus,
            externalLocationTypeEntity,
            externalLocation2
        );
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(externalObjectDirectory2);

        assertEquals(
            Optional.of(externalLocation1),
            audioTransformationService.getMediaLocation(newMedia)
        );

    }
}
