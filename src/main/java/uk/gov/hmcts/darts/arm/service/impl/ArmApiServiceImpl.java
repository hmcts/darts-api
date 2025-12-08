package uk.gov.hmcts.darts.arm.service.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.component.ArmAuthTokenCache;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadReadingBodyException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;

@Service
@Slf4j
public class ArmApiServiceImpl implements ArmApiService {

    private final ArmApiConfigurationProperties armApiConfigurationProperties;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArmClientService armClientService;
    private final ArmAuthTokenCache armAuthTokenCache;

    public ArmApiServiceImpl(ArmApiConfigurationProperties armApiConfigurationProperties,
                             ArmDataManagementConfiguration armDataManagementConfiguration,
                             ArmClientService armClientService, ArmAuthTokenCache armAuthTokenCache) {
        this.armApiConfigurationProperties = armApiConfigurationProperties;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        this.armClientService = armClientService;
        this.armAuthTokenCache = armAuthTokenCache;
    }

    @Override
    public UpdateMetadataResponse updateMetadata(String externalRecordId,
                                                 OffsetDateTime eventTimestamp,
                                                 RetentionConfidenceScoreEnum retConfScore,
                                                 String retConfReason) {

        Integer retConfScoreId = nonNull(retConfScore) ? retConfScore.getId() : null;

        UpdateMetadataRequest armUpdateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(eventTimestamp))
                          .retConfReason(retConfReason)
                          .retConfScore(retConfScoreId)
                          .build())
            .useGuidsForFields(false)
            .build();

        try {
            return armClientService.updateMetadata(getArmBearerToken(), armUpdateMetadataRequest);
        } catch (FeignException feignException) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error("Error during ARM update metadata: Detail: {}", feignException.contentUTF8(), feignException);
            int status = feignException.status();
            // If unauthorized or forbidden, retry once with a refreshed token
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                evictToken();
                return armClientService.updateMetadata(getArmBearerToken(), armUpdateMetadataRequest);
            } else {
                throw feignException;
            }
        }
    }

    @Override
    @SuppressWarnings({"PMD.CloseResource"})
    public DownloadResponseMetaData downloadArmData(String externalRecordId, String externalFileId) throws FileNotDownloadedException {
        FileBasedDownloadResponseMetaData responseMetaData = new FileBasedDownloadResponseMetaData();
        feign.Response response;
        try {
            response = armClientService.downloadArmData(
                getArmBearerToken(),
                armApiConfigurationProperties.getCabinetId(),
                externalRecordId,
                externalFileId
            );
        } catch (FeignException feignException) {
            int status = feignException.status();
            // If unauthorized or forbidden, retry once with a refreshed token
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                evictToken();
                response = armClientService.downloadArmData(
                    getArmBearerToken(),
                    armApiConfigurationProperties.getCabinetId(),
                    externalRecordId,
                    externalFileId
                );
            } else {
                throw feignException;
            }
        }

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

        log.info("Successfully downloaded ARM data for recordId: {}, fileId: {}", externalRecordId, externalFileId);
        return responseMetaData;
    }

    @Override
    public String getArmBearerToken() {
        ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
            .username(armApiConfigurationProperties.getArmUsername())
            .password(armApiConfigurationProperties.getArmPassword())
            .build();

        return armAuthTokenCache.getToken(armTokenRequest);
    }

    @Override
    public void evictToken() {
        armAuthTokenCache.evictToken();
    }

    String formatDateTime(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        String dateTime = null;
        if (nonNull(offsetDateTime)) {
            dateTime = offsetDateTime.format(dateTimeFormatter);
        }
        return dateTime;
    }
}