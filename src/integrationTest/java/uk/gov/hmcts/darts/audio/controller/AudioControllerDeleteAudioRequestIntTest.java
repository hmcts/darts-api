package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.net.URI;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
class AudioControllerDeleteAudioRequestIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio/request");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;

    @Test
    void audioRequestDeleteShouldReturnSuccess() throws Exception {
        var blobId = UUID.randomUUID();

        var systemUser = dartsDatabase.createSystemUserAccountEntity();
        var requestor = dartsDatabase.createIntegrationTestUserAccountEntity(systemUser);
        var mediaRequestEntity = dartsDatabase.createAndLoadCurrentMediaRequestEntity(requestor);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);
        dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectDirectoryStatusEntity,
                blobId
            ));

        MockHttpServletRequestBuilder requestBuilder = delete(ENDPOINT)
            .queryParam("audioRequestId", String.valueOf(mediaRequestEntity.getId()));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk());

    }

    @Test
    void audioRequestDeleteShouldReturnBadRequestWhenNoRequestBodyIsProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete(ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest());
    }

}
