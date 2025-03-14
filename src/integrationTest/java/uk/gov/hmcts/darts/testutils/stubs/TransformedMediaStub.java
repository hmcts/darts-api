package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaSubStringQueryEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Deprecated
public class TransformedMediaStub {

    private final DartsPersistence dartsPersistence;
    private final TransformedMediaRepository transformedMediaRepository;
    private final UserAccountStub userAccountStub;
    private final MediaRequestStub mediaRequestStub;

    private static final String FILE_NAME_PREFIX = "FileName";

    private static final String CASE_NUMBER_PREFIX = "CaseNumber";

    public TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequestEntity) {
        return createTransformedMediaEntity(mediaRequestEntity, null, null, null, null, null);
    }

    public TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequestEntity, String filename, OffsetDateTime expiry,
                                                               OffsetDateTime lastAccessed) {
        return createTransformedMediaEntity(mediaRequestEntity, filename, expiry, lastAccessed, null, null);
    }

    public TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequestEntity, String filename, OffsetDateTime expiry,
                                                               OffsetDateTime lastAccessed, AudioRequestOutputFormat fileFormat, Integer fileSizeBytes) {
        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setMediaRequest(mediaRequestEntity);
        transformedMediaEntity.setStartTime(mediaRequestEntity.getStartTime());
        transformedMediaEntity.setEndTime(mediaRequestEntity.getEndTime());

        if (mediaRequestEntity.getLastModifiedBy() != null) {
            transformedMediaEntity.setLastModifiedBy(mediaRequestEntity.getLastModifiedBy());
        } else {
            transformedMediaEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        }

        if (mediaRequestEntity.getCreatedBy() != null) {
            transformedMediaEntity.setCreatedBy(mediaRequestEntity.getCreatedBy());
        } else {
            transformedMediaEntity.setCreatedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        }

        transformedMediaEntity.setCreatedDateTime(mediaRequestEntity.getCreatedDateTime());
        transformedMediaEntity.setOutputFilename(filename);
        transformedMediaEntity.setOutputFilesize(fileSizeBytes);
        transformedMediaEntity.setOutputFormat(fileFormat);
        if (filename != null) {
            transformedMediaEntity.setOutputFormat(AudioRequestOutputFormat.ZIP);
        }

        transformedMediaEntity.setExpiryTime(expiry);
        transformedMediaEntity.setLastAccessed(lastAccessed);
        return dartsPersistence.save(transformedMediaEntity);
    }

    /**
     * generates test data. The following will be used for generation:-
     * Unique owner and requested by users for each transformed media record
     * Unique court house with unique name for each transformed media record
     * Unique case number with unique case number for each transformed media record
     * Unique hearing date starting with today with an incrementing day for each transformed media record
     * Unique requested date with an incrementing hour for each transformed media record
     * Unique file name with unique name for each transformed media record
     *
     * @param count The number of transformed media objects that are to be generated
     * @return The list of generated media entities in chronological order
     */
    public List<TransformedMediaEntity> generateTransformedMediaEntities(int count) {
        List<TransformedMediaEntity> retTransformerMediaLst = new ArrayList<>();
        OffsetDateTime hoursBefore = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime hoursAfter = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime requestedDate = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDateTime hearingDate = LocalDateTime.now(ZoneOffset.UTC);

        int fileSize = 1;
        for (int transformedMediaCount = 0; transformedMediaCount < count; transformedMediaCount++) {
            UserAccountEntity owner = userAccountStub.createSystemUserAccount(
                TransformedMediaSubStringQueryEnum.OWNER.getQueryString(Integer.toString(transformedMediaCount)));
            UserAccountEntity requestedBy = userAccountStub.createSystemUserAccount(
                TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(transformedMediaCount)));

            String courtName = TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryString(Integer.toString(transformedMediaCount));
            String caseNumber = CASE_NUMBER_PREFIX + transformedMediaCount;
            String fileName = FILE_NAME_PREFIX + transformedMediaCount + ".txt";
            AudioRequestOutputFormat fileFormat = AudioRequestOutputFormat.ZIP;

            var mediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                owner,
                requestedBy,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.COMPLETED,
                courtName,
                caseNumber,
                hearingDate,
                hoursBefore,
                hoursAfter,
                requestedDate
            );

            retTransformerMediaLst.add(createTransformedMediaEntity(mediaRequest, fileName, null, requestedDate, fileFormat, fileSize));
            fileSize = fileSize + 1;
            hoursBefore = hoursBefore.minusHours(1);
            hoursAfter = hoursAfter.plusHours(1);
            hearingDate = hearingDate.plusDays(count);
            requestedDate = requestedDate.plusDays(1);

        }
        return retTransformerMediaLst;
    }

    public List<Integer> getExpectedStartingFrom(int startingFromIndex, List<TransformedMediaEntity> generatedMediaEntities) {
        List<Integer> fndMediaIds = new ArrayList<>();
        for (int position = 0; position < generatedMediaEntities.size(); position++) {
            if (position >= startingFromIndex) {
                fndMediaIds.add(generatedMediaEntities.get(position).getId());
            }
        }

        return fndMediaIds;
    }

    public List<Integer> getExpectedTo(int toIndex, List<TransformedMediaEntity> generatedMediaEntities) {
        List<Integer> fndMediaIds = new ArrayList<>();
        for (int position = 0; position < generatedMediaEntities.size(); position++) {
            if (position <= toIndex) {
                fndMediaIds.add(generatedMediaEntities.get(position).getId());
            }
        }

        return fndMediaIds;
    }

    public List<Integer> getTransformedMediaIds(List<TransformedMediaEntity> entities) {
        return entities.stream().map(e -> e.getId()).collect(Collectors.toList());
    }

}