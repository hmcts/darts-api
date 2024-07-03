package uk.gov.hmcts.darts.audio.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity_;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostAdminMediasSearchHelper {

    @PersistenceContext
    private final EntityManager entityManager;

    @Value("${darts.audio.admin-search.max-results}")
    private Integer maxResults;

    public List<MediaEntity> getMatchingMedia(PostAdminMediasSearchRequest request) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MediaEntity> criteriaQuery = criteriaBuilder.createQuery(MediaEntity.class);
        Root<MediaEntity> mediaRoot = criteriaQuery.from(MediaEntity.class);

        List<Predicate> predicates = createPredicates(request, criteriaBuilder, mediaRoot);
        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        Path<MediaEntity> namePath = mediaRoot;
        criteriaQuery.select(namePath).distinct(true);
        criteriaQuery.where(finalAndPredicate);

        TypedQuery<MediaEntity> query = entityManager.createQuery(criteriaQuery);
        query.setMaxResults(maxResults + 1);
        return query.getResultList();
    }

    private List<Predicate> createPredicates(PostAdminMediasSearchRequest request,
                                             CriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot) {
        List<Predicate> predicates = new ArrayList<>();

        CollectionUtils.addAll(predicates, addCourthouseIdCriteria(mediaRoot, request));
        CollectionUtils.addAll(predicates, addCaseNumberCriteria(criteriaBuilder, mediaRoot, request));
        CollectionUtils.addAll(predicates, addCourtroomNameCriteria(criteriaBuilder, mediaRoot, request));
        CollectionUtils.addAll(predicates, addDateCriteria(criteriaBuilder, mediaRoot, request));

        return predicates;
    }

    private List<Predicate> addCourthouseIdCriteria(Root<MediaEntity> mediaRoot, PostAdminMediasSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getCourthouseIds())) {
            Join<MediaEntity, CourtroomEntity> courtroomJoin = getCourtroomJoin(mediaRoot);
            predicateList.add(courtroomJoin.get(CourtroomEntity_.courthouse).get(CourthouseEntity_.ID).in(request.getCourthouseIds()));
        }
        return predicateList;
    }

    private List<Predicate> addCaseNumberCriteria(CriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot,
                                                  PostAdminMediasSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            Join<MediaEntity, CourtCaseEntity> caseJoin = getCaseJoin(mediaRoot);
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(caseJoin.get(CourtCaseEntity_.CASE_NUMBER)),
                surroundWithPercentagesUpper(request.getCaseNumber())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addCourtroomNameCriteria(CriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot,
                                                     PostAdminMediasSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCourtroomName())) {
            Join<MediaEntity, CourtroomEntity> courtroomJoin = getCourtroomJoin(mediaRoot);
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(courtroomJoin.get(CourtroomEntity_.NAME)),
                surroundWithPercentagesUpper(request.getCourtroomName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addDateCriteria(CriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot,
                                            PostAdminMediasSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (request.getHearingStartAt() != null || request.getHearingEndAt() != null) {
            Join<MediaEntity, HearingEntity> hearingJoin = getHearingJoin(mediaRoot);
            if (request.getHearingStartAt() != null) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                    hearingJoin.get(HearingEntity_.HEARING_DATE),
                    request.getHearingStartAt()
                ));
            }
            if (request.getHearingEndAt() != null) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(
                    hearingJoin.get(HearingEntity_.HEARING_DATE),
                    request.getHearingEndAt()
                ));
            }
        }
        return predicateList;
    }

    private String surroundWithPercentagesUpper(String value) {
        return surroundValue(value.toUpperCase(Locale.ROOT), "%");
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    @SuppressWarnings("unchecked")
    private Join<MediaEntity, CourtroomEntity> getCourtroomJoin(Root<MediaEntity> mediaRoot) {
        Optional<Join<MediaEntity, ?>> foundJoin = mediaRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            MediaEntity_.COURTROOM)).findAny();
        return foundJoin.map(join -> (Join<MediaEntity, CourtroomEntity>) join)
            .orElseGet(() -> mediaRoot.join(MediaEntity_.COURTROOM, JoinType.INNER));
    }

    @SuppressWarnings("unchecked")
    private Join<MediaEntity, HearingEntity> getHearingJoin(Root<MediaEntity> mediaRoot) {
        Optional<Join<MediaEntity, ?>> foundJoin = mediaRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            MediaEntity_.HEARING_LIST)).findAny();
        return foundJoin.map(join -> (Join<MediaEntity, HearingEntity>) join)
            .orElseGet(() -> mediaRoot.join(MediaEntity_.HEARING_LIST, JoinType.INNER));
    }

    @SuppressWarnings("unchecked")
    private Join<MediaEntity, CourtCaseEntity> getCaseJoin(Root<MediaEntity> mediaRoot) {
        Join<MediaEntity, HearingEntity> hearingJoin = getHearingJoin(mediaRoot);
        return hearingJoin.join(HearingEntity_.COURT_CASE, JoinType.INNER);
    }

}
