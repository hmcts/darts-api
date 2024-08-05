package uk.gov.hmcts.darts.arm.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.ConfidenceAware;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
@Component
@Slf4j
public class ArmHelper {

    public ConfidenceAware getDocumentConfidence(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        ConfidenceAware retentionDate;
        if (nonNull(externalObjectDirectoryEntity.getMedia())) {
            retentionDate = externalObjectDirectoryEntity.getMedia();
        } else if (nonNull(externalObjectDirectoryEntity.getTranscriptionDocumentEntity())) {
            retentionDate = externalObjectDirectoryEntity.getTranscriptionDocumentEntity();
        } else if (nonNull(externalObjectDirectoryEntity.getAnnotationDocumentEntity())) {
            retentionDate = externalObjectDirectoryEntity.getAnnotationDocumentEntity();
        } else if (nonNull(externalObjectDirectoryEntity.getCaseDocument())) {
            retentionDate = externalObjectDirectoryEntity.getCaseDocument();
        } else {
            throw new DartsException("Unable to get object for EOD " + externalObjectDirectoryEntity.getId());
        }
        return retentionDate;
    }
}