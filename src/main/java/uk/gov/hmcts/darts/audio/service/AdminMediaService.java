package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.audio.model.AdminVersionedMediaResponse;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.audio.model.MediaApproveMarkedForDeletionResponse;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideResponse;
import uk.gov.hmcts.darts.audio.model.PatchAdminMediasByIdRequest;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchRequest;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchResponseItem;

import java.time.OffsetDateTime;
import java.util.List;

public interface AdminMediaService {

    AdminMediaResponse getMediasById(Long id);

    List<GetAdminMediaResponseItem> filterMedias(Integer transformedMediaId, List<Integer> hearingIds, OffsetDateTime startAt,
                                                 OffsetDateTime endAt);

    default List<GetAdminMediaResponseItem> filterMediasWithTransformedMediaId(Integer transformedMediaId) {
        return filterMedias(transformedMediaId, null, null, null);
    }

    List<PostAdminMediasSearchResponseItem> performAdminMediasSearchPost(PostAdminMediasSearchRequest adminMediasSearchRequest);

    MediaHideResponse adminHideOrShowMediaById(Long mediaId, MediaHideRequest mediaHideRequest);

    List<GetAdminMediasMarkedForDeletionItem> getMediasMarkedForDeletion();

    MediaApproveMarkedForDeletionResponse adminApproveMediaMarkedForDeletion(Long mediaId);

    AdminVersionedMediaResponse getMediaVersionsById(Long id);

    void patchMediasById(Long id, PatchAdminMediasByIdRequest patchAdminMediasByIdRequest);
}