package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class TransformedMediaTestData {

    private TransformedMediaTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static TransformedMediaEntity minimalTransformedMedia() {
        var transformedMedia = new TransformedMediaEntity();
        transformedMedia.setMediaRequest(PersistableFactory.getMediaRequestTestData().someMinimalRequestData());
        transformedMedia.setStartTime(OffsetDateTime.now());
        transformedMedia.setEndTime(OffsetDateTime.now().plusHours(1));
        transformedMedia.setCreatedBy(minimalUserAccount());
        transformedMedia.setLastModifiedBy(minimalUserAccount());
        transformedMedia.setLastModifiedDateTime(OffsetDateTime.now());
        transformedMedia.setCreatedDateTime(OffsetDateTime.now());
        return transformedMedia;
    }
}