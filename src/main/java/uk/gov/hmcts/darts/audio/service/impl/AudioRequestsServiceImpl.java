package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity_;
import uk.gov.hmcts.darts.audio.service.AudioRequestsService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioRequestsServiceImpl implements AudioRequestsService {

    private final EntityManager entityManager;

    @Override
    public List<AudioRequestSummaryResult> viewAudioRequests(Integer userId, Boolean expired) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AudioRequestSummaryResult> criteriaQuery = criteriaBuilder.createQuery(AudioRequestSummaryResult.class);

        Root<MediaRequestEntity> mediaRequest = criteriaQuery.from(MediaRequestEntity.class);
        Join<MediaRequestEntity, HearingEntity> hearing = mediaRequest.join(MediaRequestEntity_.hearing);
        Join<HearingEntity, CourtCaseEntity> courtCase = hearing.join(HearingEntity_.courtCase);
        Join<CourtCaseEntity, CourthouseEntity> courthouse = courtCase.join(CourtCaseEntity_.courthouse);

        criteriaQuery.select(criteriaBuilder.construct(
            AudioRequestSummaryResult.class,
            mediaRequest.get(MediaRequestEntity_.id),
            courtCase.get(CourtCaseEntity_.caseNumber),
            courthouse.get(CourthouseEntity_.courthouseName),
            hearing.get(HearingEntity_.hearingDate),
            mediaRequest.get(MediaRequestEntity_.startTime),
            mediaRequest.get(MediaRequestEntity_.endTime),
            mediaRequest.get(MediaRequestEntity_.endTime), //TODO expiry_ts ?
            mediaRequest.get(MediaRequestEntity_.status)
        ));

        ParameterExpression<Integer> paramUserId = criteriaBuilder.parameter(Integer.class);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(mediaRequest.get(MediaRequestEntity_.requestor), paramUserId),
            expiredPredicate(expired, criteriaBuilder, mediaRequest)
        ));

        criteriaQuery.orderBy(List.of(
            criteriaBuilder.asc(courtCase.get(CourtCaseEntity_.caseNumber)),
            criteriaBuilder.asc(mediaRequest.get(MediaRequestEntity_.startTime))
        ));

        TypedQuery<AudioRequestSummaryResult> query = entityManager.createQuery(criteriaQuery);
        query.setParameter(paramUserId, userId);

        return query.getResultList();
    }

    private Predicate expiredPredicate(Boolean expired, CriteriaBuilder criteriaBuilder,
                                       Root<MediaRequestEntity> mediaRequest) {

        final Predicate expiredPredicate;
        if (!expired) {
            expiredPredicate = criteriaBuilder.lessThan(
                mediaRequest.get(MediaRequestEntity_.endTime), //TODO expiry_ts ?
                OffsetDateTime.now(ZoneOffset.UTC)
            );
        } else {
            expiredPredicate = criteriaBuilder.greaterThanOrEqualTo(
                mediaRequest.get(MediaRequestEntity_.endTime), //TODO expiry_ts ?
                OffsetDateTime.now(ZoneOffset.UTC)
            );
        }
        return expiredPredicate;
    }

}
