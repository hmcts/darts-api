package uk.gov.hmcts.darts.arm.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.GrantType;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadReadingBodyException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmApiServiceImpl implements ArmApiService {

    private final ArmApiConfigurationProperties armApiConfigurationProperties;
    private final ArmTokenClient armTokenClient;
    private final ArmApiClient armApiClient;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp, int retConfScore, String retConfReason) {

        UpdateMetadataRequest armUpdateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(eventTimestamp)
                          .retConfReason(retConfReason)
                          .retConfScore(retConfScore)
                          .build())
            .useGuidsForFields(false)
            .build();

        try {
            return armApiClient.updateMetadata(getArmBearerToken(), armUpdateMetadataRequest);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error("Error during ARM update metadata: Detail: {}", e.contentUTF8());
            throw e;
        }
    }

    @Override
    @SuppressWarnings({"PMD.CloseResource"})
    public DownloadResponseMetaData downloadArmData(String externalRecordId, String externalFileId) throws FileNotDownloadedException {
        FileBasedDownloadResponseMetaData responseMetaData = new FileBasedDownloadResponseMetaData();

        feign.Response response = armApiClient.downloadArmData(
            getArmBearerToken(),
            armApiConfigurationProperties.getCabinetId(),
            externalRecordId,
            externalFileId
        );

        // on any error occurring return a download failure
        if (!HttpStatus.valueOf(response.status()).is2xxSuccessful()) {
            String message = ("Arm file failed to download, cabinet: %s, record id: %s, " +
                "file id: %s. Failure response: %s").formatted(armApiConfigurationProperties.getCabinetId(),
                                                               externalRecordId, externalFileId, response.status());
            log.error(message);
            throw new FileNotDownloadedException(message);
        }

        try {
            responseMetaData.setContainerTypeUsedToDownload(DatastoreContainerType.ARM);
            responseMetaData.setInputStream(response.body().asInputStream(), armDataManagementConfiguration);
        } catch (Exception e) {
            String message = ("Arm file failed to download due to body stream, " +
                "cabinet: %s, " +
                "record id: %s, file id: %s. Failure response: %s")
                .formatted(armApiConfigurationProperties.getCabinetId(), externalRecordId, externalFileId, response.status());
            log.error(message, e);
            throw new FileNotDownloadReadingBodyException(message, e);
        }

        log.debug("Successfully downloaded ARM data for recordId: {}, fileId: {}", externalRecordId, externalFileId);
        return responseMetaData;
    }

    @Override
    public String getArmBearerToken() {
        log.debug("Get ARM Bearer Token with Username: {}, Password: {}", armApiConfigurationProperties.getArmUsername(),
                  armApiConfigurationProperties.getArmPassword());
        String accessToken = null;
        ArmTokenResponse armTokenResponse = armTokenClient.getToken(new ArmTokenRequest(
            armApiConfigurationProperties.getArmUsername(),
            armApiConfigurationProperties.getArmPassword(),
            GrantType.PASSWORD.getValue()
        ));

        if (StringUtils.isNotEmpty(armTokenResponse.getAccessToken())) {
            String bearerToken = String.format("Bearer %s", armTokenResponse.getAccessToken());
            log.debug("Fetched ARM Bearer Token from /token: {}", bearerToken);
            AvailableEntitlementProfile availableEntitlementProfile = armTokenClient.availableEntitlementProfiles(bearerToken);
            if (!availableEntitlementProfile.isError()) {
                Optional<String> profileId = availableEntitlementProfile.getProfiles().stream()
                    .filter(p -> armApiConfigurationProperties.getArmServiceProfile().equalsIgnoreCase(p.getProfileName()))
                    .map(p -> p.getProfileId())
                    .findAny();
                if (profileId.isPresent()) {
                    log.debug("Found DARTS ARM Service Profile Id: {}", profileId.get());
                    ArmTokenResponse tokenResponse = armTokenClient.selectEntitlementProfile(bearerToken, profileId.get());
                    accessToken = tokenResponse.getAccessToken();
                }
            }
        }

        log.debug("Fetched ARM Bearer Token : {}", armTokenResponse.getAccessToken());
        return String.format("Bearer %s", accessToken);
    }

}