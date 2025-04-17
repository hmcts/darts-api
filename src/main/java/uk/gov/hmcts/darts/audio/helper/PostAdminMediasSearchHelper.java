package uk.gov.hmcts.darts.audio.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
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
import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
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
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public class PostAdminMediasSearchHelper {

    @PersistenceContext
    private final EntityManager entityManager;

    @Value("${darts.audio.admin-search.max-results}")
    private Integer maxResults;

    public List<MediaEntity> getMatchingMedia(PostAdminMediasSearchRequest request) {
        HibernateCriteriaBuilder criteriaBuilder = entityManager.unwrap(Session.class).getCriteriaBuilder();
        CriteriaQuery<MediaEntity> criteriaQuery = criteriaBuilder.createQuery(MediaEntity.class);
        Root<MediaEntity> mediaRoot = criteriaQuery.from(MediaEntity.class);

        List<Predicate> predicates = createPredicates(request, criteriaBuilder, mediaRoot);
        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        Path<MediaEntity> namePath = mediaRoot;
        criteriaQuery.select(namePath).distinct(true);
        criteriaQuery.where(finalAndPredicate);
        criteriaQuery.orderBy(criteriaBuilder.desc(mediaRoot.get(MediaEntity_.ID)));

        TypedQuery<MediaEntity> query = entityManager.createQuery(criteriaQuery);
        query.setMaxResults(maxResults + 1);
        return query.getResultList();
    }

    private List<Predicate> createPredicates(PostAdminMediasSearchRequest request,
                                             HibernateCriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot) {
        List<Predicate> predicates = new ArrayList<>();

        CollectionUtils.addAll(predicates, addCourthouseIdCriteria(mediaRoot, request));
        CollectionUtils.addAll(predicates, addCaseNumberCriteria(criteriaBuilder, mediaRoot, request));
        CollectionUtils.addAll(predicates, addCourtroomNameCriteria(criteriaBuilder, mediaRoot, request));
        CollectionUtils.addAll(predicates, addDateCriteria(criteriaBuilder, mediaRoot, request));
        CollectionUtils.addAll(predicates, addCurrentMedia(criteriaBuilder, mediaRoot));

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

    private List<Predicate> addCaseNumberCriteria(HibernateCriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot,
                                                  PostAdminMediasSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            Join<MediaEntity, CourtCaseEntity> caseJoin = getCaseJoin(mediaRoot);
            predicateList.add(criteriaBuilder.ilike(
                caseJoin.get(CourtCaseEntity_.CASE_NUMBER),
                surroundWithPercentages(request.getCaseNumber())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addCourtroomNameCriteria(HibernateCriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot,
                                                     PostAdminMediasSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCourtroomName())) {
            Join<MediaEntity, CourtroomEntity> courtroomJoin = getCourtroomJoin(mediaRoot);
            predicateList.add(criteriaBuilder.ilike(
                courtroomJoin.get(CourtroomEntity_.NAME),
                surroundWithPercentages(request.getCourtroomName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addDateCriteria(HibernateCriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot,
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

    private List<Predicate> addCurrentMedia(HibernateCriteriaBuilder criteriaBuilder, Root<MediaEntity> mediaRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        predicateList.add(criteriaBuilder.isTrue(
            mediaRoot.get(MediaEntity_.IS_CURRENT)
        ));
        return predicateList;
    }

    private String surroundWithPercentages(String value) {
        return surroundValue(value, "%");
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    @SuppressWarnings("unchecked")
    private Join<MediaEntity, CourtroomEntity> getCourtroomJoin(Root<MediaEntity> mediaRoot) {
        Optional<Join<MediaEntity, ?>> foundJoin = mediaRoot.getJoins().stream()
            .filter(join -> MediaEntity_.COURTROOM.equals(join.getAttribute().getName())).findAny();

        return foundJoin.map(join -> (Join<MediaEntity, CourtroomEntity>) join)
            .orElseGet(() -> mediaRoot.join(MediaEntity_.COURTROOM, JoinType.INNER));
    }

    @SuppressWarnings("unchecked")
    private Join<MediaEntity, HearingEntity> getHearingJoin(Root<MediaEntity> mediaRoot) {
        Optional<Join<MediaEntity, ?>> foundJoin = mediaRoot.getJoins().stream()
            .filter(join -> MediaEntity_.HEARINGS.equals(join.getAttribute().getName())).findAny();

        return foundJoin.map(join -> (Join<MediaEntity, HearingEntity>) join)
            .orElseGet(() -> mediaRoot.join(MediaEntity_.HEARINGS, JoinType.INNER));
    }

    private Join<MediaEntity, CourtCaseEntity> getCaseJoin(Root<MediaEntity> mediaRoot) {
        Join<MediaEntity, HearingEntity> hearingJoin = getHearingJoin(mediaRoot);
        return hearingJoin.join(HearingEntity_.COURT_CASE, JoinType.INNER);
    }

}
