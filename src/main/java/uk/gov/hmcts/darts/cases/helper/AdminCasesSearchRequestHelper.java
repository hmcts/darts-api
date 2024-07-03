package uk.gov.hmcts.darts.cases.helper;

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
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@SuppressWarnings({"PMD.TooManyMethods"})
@RequiredArgsConstructor
@Slf4j
public class AdminCasesSearchRequestHelper {
    @PersistenceContext
    private final EntityManager entityManager;

    @Value("${darts.cases.admin-search.max-results}")
    private Integer maxResults;

    public List<Integer> getMatchingCaseIds(AdminCasesSearchRequest request) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
        Root<CourtCaseEntity> caseRoot = criteriaQuery.from(CourtCaseEntity.class);

        List<Predicate> predicates = createPredicates(request, criteriaBuilder, caseRoot);
        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        Path<Integer> namePath = caseRoot.get(CourtCaseEntity_.ID);
        criteriaQuery.select(namePath).distinct(true);
        criteriaQuery.where(finalAndPredicate);

        TypedQuery<Integer> query = entityManager.createQuery(criteriaQuery);
        query.setMaxResults(maxResults + 1);
        return query.getResultList();
    }

    private List<Predicate> createPredicates(AdminCasesSearchRequest request,
                                             CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicates = new ArrayList<>();

        CollectionUtils.addAll(predicates, addCourthouseIdCriteria(caseRoot, request));
        CollectionUtils.addAll(predicates, addCaseNumberCriteria(criteriaBuilder, caseRoot, request));
        CollectionUtils.addAll(predicates, addCourtroomNameCriteria(criteriaBuilder, caseRoot, request));
        CollectionUtils.addAll(predicates, addHearingDateCriteria(criteriaBuilder, caseRoot, request));

        return predicates;
    }

    private List<Predicate> addCourthouseIdCriteria(Root<CourtCaseEntity> caseRoot, AdminCasesSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getCourthouseIds())) {
            predicateList.add(caseRoot.get(CourtroomEntity_.COURTHOUSE).get(CourthouseEntity_.ID).in(request.getCourthouseIds()));
        }
        return predicateList;
    }

    private List<Predicate> addCaseNumberCriteria(CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot,
                                                  AdminCasesSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(caseRoot.get(CourtCaseEntity_.CASE_NUMBER)),
                surroundWithPercentagesUpper(request.getCaseNumber())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addCourtroomNameCriteria(CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot,
                                                     AdminCasesSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCourtroomName())) {
            Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);
            Join<HearingEntity, CourtroomEntity> courtroomJoin = joinCourtroom(hearingJoin);
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(courtroomJoin.get(CourtroomEntity_.NAME)),
                surroundWithPercentagesUpper(request.getCourtroomName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addHearingDateCriteria(CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot,
                                                   AdminCasesSearchRequest request) {
        List<Predicate> predicateList = new ArrayList<>();
        if (request.getHearingStartAt() != null || request.getHearingEndAt() != null) {
            Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);
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

    private Join<HearingEntity, CourtroomEntity> joinCourtroom(Join<CourtCaseEntity, HearingEntity> hearingJoin) {
        return hearingJoin.join(HearingEntity_.COURTROOM, JoinType.INNER);
    }

    @SuppressWarnings("unchecked")
    private Join<CourtCaseEntity, HearingEntity> joinHearing(Root<CourtCaseEntity> caseRoot) {
        Optional<Join<CourtCaseEntity, ?>> foundJoin = caseRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            CourtCaseEntity_.HEARINGS)).findAny();
        return foundJoin.map(join -> (Join<CourtCaseEntity, HearingEntity>) join)
            .orElseGet(() -> caseRoot.join(CourtCaseEntity_.HEARINGS, JoinType.INNER));
    }

}
