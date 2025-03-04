package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.AWAITING_VERIFICATION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.NEW;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.util.EodHelper.armLocation;
import static uk.gov.hmcts.darts.common.util.EodHelper.awaitingVerificationStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.inboundLocation;
import static uk.gov.hmcts.darts.common.util.EodHelper.storedStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.unstructuredLocation;

class EodRepositoryHasMediaNotBeenCopiedFromInboundStorageIntTest extends IntegrationBase {

    MediaEntity media;
    MediaEntity anotherMedia;

    @Autowired
    ExternalObjectDirectoryStub eodStub;

    @Autowired
    ExternalObjectDirectoryRepository eodRepo;

    @BeforeEach
    void setup() {
        media = dartsDatabase.getMediaStub().createAndSaveMedia();
        anotherMedia = dartsDatabase.getMediaStub().createAndSaveMedia();
    }

    @Test
    void testMediaInInboundStored() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isTrue();
    }

    @Test
    void testMediaInInboundAndUnstructured() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(media, STORED, UNSTRUCTURED);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isFalse();
    }

    @Test
    void testMediaInInboundAndUnstructuredWithFailure() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(media, FAILURE, UNSTRUCTURED);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation()));

        assertThat(result).isFalse();
    }

    @Test
    void testMediaInInboundAndArm() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(media, ARM_INGESTION, ARM);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isFalse();
    }

    @Test
    void testMediaInInboundNotStoredAndArm() {
        eodStub.createAndSaveEod(media, NEW, INBOUND);
        eodStub.createAndSaveEod(media, ARM_INGESTION, ARM);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isFalse();
    }

    @Test
    void testMediaInInboundAndUnstructuredAndArm() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(media, STORED, UNSTRUCTURED);
        eodStub.createAndSaveEod(media, ARM_INGESTION, ARM);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isFalse();
    }

    @Test
    void testMediaInInboundAndUnstructuredAwaitingVerification() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(media, AWAITING_VERIFICATION, UNSTRUCTURED);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isTrue();
    }

    @Test
    void testMediaInInboundAndUnstructuredAwaitingVerificationAndArm() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(media, AWAITING_VERIFICATION, UNSTRUCTURED);
        eodStub.createAndSaveEod(media, ARM_INGESTION, ARM);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isFalse();
    }

    @Test
    void testMediaInInboundAndOtherStorageThanUnstructuredOrArm() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(media, STORED, DETS);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isTrue();
    }

    @Test
    void testMediaInInboundButNotStored() {
        eodStub.createAndSaveEod(media, NEW, INBOUND);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isFalse();
    }

    @Test
    void testDifferentMediaThanRequested() {
        eodStub.createAndSaveEod(media, STORED, INBOUND);
        eodStub.createAndSaveEod(anotherMedia, STORED, UNSTRUCTURED);

        var result = eodRepo.hasMediaNotBeenCopiedFromInboundStorage(media,
                                                                     storedStatus(), inboundLocation(),
                                                                     awaitingVerificationStatus(),
                                                                     List.of(unstructuredLocation(), armLocation()));

        assertThat(result).isTrue();
    }
}
