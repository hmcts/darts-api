package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseItem;

import java.time.OffsetDateTime;
import java.util.List;

public interface AdminMediaService {

    AdminMediaResponse getMediasById(Integer id);

    List<AdminMediaSearchResponseItem> filterMedias(Integer transformedMediaId, List<Integer> hearingIds, OffsetDateTime startAt,
                                                        OffsetDateTime endAt);

    default List<AdminMediaSearchResponseItem> filterMediasWithTransformedMediaId(Integer transformedMediaId) {
        return filterMedias(transformedMediaId, null,null, null);
    }
}