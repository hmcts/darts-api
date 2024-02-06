package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.enums.GrantType;
import uk.gov.hmcts.darts.arm.service.ArmApiService;

import java.io.InputStream;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmApiServiceImpl implements ArmApiService {

    private final ArmApiConfigurationProperties armApiConfigurationProperties;
    private final ArmTokenClient armTokenClient;
    private final ArmApiClient armApiClient;

    @Override
    public UpdateMetadataResponse updateMetadata(String externalRecordId, OffsetDateTime eventTimestamp) {

        UpdateMetadataRequest armUpdateMetadataRequest = UpdateMetadataRequest.builder()
              .itemId(externalRecordId)
              .manifest(UpdateMetadataRequest.Manifest.builder()
                    .eventDate(eventTimestamp)
                    .build())
              .useGuidsForFields(false)
              .build();

        return armApiClient.updateMetadata(
              getArmBearerToken(),
              armUpdateMetadataRequest
        );
    }

    @Override
    @SneakyThrows
    public InputStream downloadArmData(String externalRecordId, String externalFileId) {
        feign.Response response = armApiClient.downloadArmData(
              getArmBearerToken(),
              armApiConfigurationProperties.getCabinetId(),
              externalRecordId,
              externalFileId
        );
        log.debug("Successfully downloaded ARM data for recordId: {}, fileId: {}", externalRecordId, externalFileId);
        return response.body().asInputStream();
    }

    private String getArmBearerToken() {
        ArmTokenResponse armTokenResponse = armTokenClient.getToken(new ArmTokenRequest(
              armApiConfigurationProperties.getArmUsername(),
              armApiConfigurationProperties.getArmPassword(),
              GrantType.PASSWORD.getValue()
        ));
        return String.format("Bearer %s", armTokenResponse.getAccessToken());
    }

}
