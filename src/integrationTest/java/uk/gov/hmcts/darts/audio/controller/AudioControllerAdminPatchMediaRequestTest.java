package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchResponse;
import uk.gov.hmcts.darts.audiorequests.model.Problem;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;

import java.net.URI;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class AudioControllerAdminPatchMediaRequestTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/media-requests/");

    @Autowired
    private MediaRequestStub mediaStub;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EXCLUDE)
    void disallowsAllUsersExceptSuperAdmin(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MediaPatchRequest request = new MediaPatchRequest();
        mockMvc.perform(
                patch(ENDPOINT + "1").content(objectMapper.writeValueAsString(request))
                    .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void patchSuccessOwnerChanged() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(userAccountEntity);

        UserAccountEntity newOwner = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(newOwner);

        MediaRequestEntity mediaRequestEntity = mediaStub.createAndLoadMediaRequestEntity(userAccountEntity,
                                                                              AudioRequestType.DOWNLOAD,
                                                                              MediaRequestStatus.COMPLETED);

        MediaPatchRequest request = new MediaPatchRequest();
        request.setOwnerId(newOwner.getId());

        MvcResult result = mockMvc.perform(
                patch(ENDPOINT + String.valueOf(mediaRequestEntity.getId()))
                    .contentType("application/json").content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        mediaRequestEntity = mediaStub.getFindId(mediaRequestEntity).get();

        MediaPatchResponse mediaPatchResponse
            = objectMapper.readValue(result.getResponse().getContentAsString(), MediaPatchResponse.class);
        Assertions.assertEquals(newOwner.getId(), mediaRequestEntity.getCurrentOwner().getId());
        Assertions.assertEquals(mediaRequestEntity.getId(), mediaPatchResponse.getId());
        Assertions.assertEquals(mediaRequestEntity.getStartTime(), mediaPatchResponse.getStartAt());
        Assertions.assertEquals(mediaRequestEntity.getCreatedDateTime(), mediaPatchResponse.getRequestedAt());
        Assertions.assertEquals(mediaRequestEntity.getRequestor().getId(), mediaPatchResponse.getRequestedById());
        Assertions.assertEquals(mediaRequestEntity.getCurrentOwner().getId(), mediaPatchResponse.getOwnerId());
    }

    @Test
    void patchSuccessOwnerNotSupplied() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(userAccountEntity);

        MediaRequestEntity mediaRequestEntity = mediaStub.createAndLoadMediaRequestEntity(userAccountEntity,
                                                                                          AudioRequestType.DOWNLOAD,
                                                                                          MediaRequestStatus.COMPLETED);

        MediaPatchRequest request = new MediaPatchRequest();

        MvcResult result = mockMvc.perform(
                patch(ENDPOINT + String.valueOf(mediaRequestEntity.getId()))
                    .contentType("application/json").content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        mediaRequestEntity = mediaStub.getFindId(mediaRequestEntity).get();

        MediaPatchResponse mediaPatchResponse
            = objectMapper.readValue(result.getResponse().getContentAsString(), MediaPatchResponse.class);
        Assertions.assertEquals(mediaRequestEntity.getId(), mediaPatchResponse.getId());
        Assertions.assertEquals(mediaRequestEntity.getStartTime(), mediaPatchResponse.getStartAt());
        Assertions.assertEquals(mediaRequestEntity.getCreatedDateTime(), mediaPatchResponse.getRequestedAt());
        Assertions.assertEquals(mediaRequestEntity.getRequestor().getId(), mediaPatchResponse.getRequestedById());
        Assertions.assertEquals(mediaRequestEntity.getCurrentOwner().getId(), mediaPatchResponse.getOwnerId());
    }

    @Test
    void throwsNotFoundWhenMediaRequestIdDoesntExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MediaPatchRequest request = new MediaPatchRequest();

        MvcResult result = mockMvc.perform(
                patch(ENDPOINT + String.valueOf(100))
                    .contentType("application/json").content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andReturn();

        Problem problemResponse
            = objectMapper.readValue(result.getResponse().getContentAsString(), Problem.class);
        Assertions.assertEquals(AudioRequestsApiError.MEDIA_REQUEST_NOT_FOUND.getType(), problemResponse.getType());
    }

    @Test
    void throwsUnprocessableEntityWhenMediaRequestOwnerIdDoesntExist() throws Exception {

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(userAccountEntity);

        MediaRequestEntity mediaRequestEntity = mediaStub.createAndLoadMediaRequestEntity(userAccountEntity,
                                                                                          AudioRequestType.DOWNLOAD,
                                                                                          MediaRequestStatus.COMPLETED);

        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MediaPatchRequest request = new MediaPatchRequest();
        request.setOwnerId(343_434);

        MvcResult result = mockMvc.perform(
                patch(ENDPOINT + String.valueOf(mediaRequestEntity.getId()))
                    .contentType("application/json").content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        Problem problemResponse
            = objectMapper.readValue(result.getResponse().getContentAsString(), Problem.class);
        Assertions.assertEquals(AudioRequestsApiError.USER_IS_NOT_FOUND.getType(), problemResponse.getType());
    }
}