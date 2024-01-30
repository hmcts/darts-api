package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {
    "darts.storage.arm-api.url=http://localhost:4551"
})
class ArmApiServiceIntTest extends IntegrationBase {

    @Autowired
    private ArmApiService armApiService;

    @Test
    void updateMetadata() {
        var externalRecordId = "7683ee65-c7a7-7343-be80-018b8ac13602";
        var eventTimestamp = OffsetDateTime.now().plusYears(7);
        var expected = UpdateMetadataResponse.builder()
            .itemId(UUID.fromString(externalRecordId))
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();

        UpdateMetadataResponse updateMetadataResponse = armApiService.updateMetadata(externalRecordId, eventTimestamp);

        assertEquals(expected, updateMetadataResponse);
    }

}
