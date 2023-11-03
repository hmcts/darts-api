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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity_;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity_;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity_;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity_;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@SuppressWarnings({"PMD.TooManyMethods"})
@RequiredArgsConstructor
public class AdvancedSearchRequestHelper {
    @PersistenceContext
    private final EntityManager entityManager;

    private final UserIdentity userIdentity;

    public List<Integer> getMatchingCourtCases(GetCasesSearchRequest request) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
        Root<CourtCaseEntity> caseRoot = criteriaQuery.from(CourtCaseEntity.class);
        List<Predicate> predicates = createPredicates(request, criteriaBuilder, caseRoot);

        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        criteriaQuery.where(finalAndPredicate);
        Path<Integer> namePath = caseRoot.get(CourtCaseEntity_.ID);
        criteriaQuery.select(namePath);

        TypedQuery<Integer> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    private List<Predicate> createPredicates(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicates = new ArrayList<>();
        CollectionUtils.addAll(predicates, createCaseCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addHearingDateCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addCourthouseCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addCourtroomCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addJudgeCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addDefendantCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addEventCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addUserSecurityRolesCriteria(criteriaBuilder, caseRoot));
        return predicates;

    }

    private List<Predicate> createCaseCriteria(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(caseRoot.get(CourtCaseEntity_.CASE_NUMBER)),
                surroundWithPercentagesUpper(request.getCaseNumber())
            ));
        }
        return predicateList;
    }

    private String surroundWithPercentagesUpper(String value) {
        return surroundValue(value.toUpperCase(Locale.ROOT), "%");
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    private List<Predicate> addCourtroomCriteria(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCourtroom())) {
            Join<HearingEntity, CourtroomEntity> courtroomJoin = joinCourtroom(caseRoot);

            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(courtroomJoin.get(CourtroomEntity_.NAME)),
                surroundWithPercentagesUpper(request.getCourtroom())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addDefendantCriteria(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getDefendantName())) {
            Join<CourtCaseEntity, DefendantEntity> defendantJoin = joinDefendantEntity(caseRoot);

            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(defendantJoin.get(DefendantEntity_.NAME)),
                surroundWithPercentagesUpper(request.getDefendantName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addEventCriteria(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getEventTextContains())) {
            Join<CourtCaseEntity, EventEntity> eventJoin = joinEventEntity(caseRoot);

            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(eventJoin.get(EventEntity_.EVENT_TEXT)),
                surroundWithPercentagesUpper(request.getEventTextContains())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addCourthouseCriteria(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCourthouse())) {
            Join<CourtCaseEntity, CourthouseEntity> courthouseJoin = joinCourthouse(caseRoot);
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(courthouseJoin.get(CourthouseEntity_.COURTHOUSE_NAME)),
                surroundWithPercentagesUpper(request.getCourthouse())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addUserSecurityRolesCriteria(CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {

        List<Predicate> predicateList = new ArrayList<>();
        Join<CourtCaseEntity, UserAccountEntity> userJoin = joinUser(caseRoot);
        predicateList.add(criteriaBuilder.equal(
                              criteriaBuilder.lower(userJoin.get(UserAccountEntity_.EMAIL_ADDRESS)),
                              userIdentity.getEmailAddress().toLowerCase()
                          )
        );
        return predicateList;
    }

    private List<Predicate> addJudgeCriteria(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getJudgeName())) {
            Join<CourtCaseEntity, JudgeEntity> judgeJoin = joinJudge(caseRoot);
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(judgeJoin.get(JudgeEntity_.NAME)),
                surroundWithPercentagesUpper(request.getJudgeName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addHearingDateCriteria(GetCasesSearchRequest request, CriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);
            if (request.getDateFrom() != null) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                    hearingJoin.get(HearingEntity_.HEARING_DATE),
                    request.getDateFrom()
                ));
            }
            if (request.getDateTo() != null) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(
                    hearingJoin.get(HearingEntity_.HEARING_DATE),
                    request.getDateTo()
                ));
            }
        }
        return predicateList;
    }


    @SuppressWarnings("unchecked")
    private Join<CourtCaseEntity, HearingEntity> joinHearing(Root<CourtCaseEntity> caseRoot) {
        Optional<Join<CourtCaseEntity, ?>> foundJoin = caseRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            CourtCaseEntity_.HEARINGS)).findAny();
        return foundJoin.map(courtCaseEntityJoin -> (Join<CourtCaseEntity, HearingEntity>) courtCaseEntityJoin)
            .orElseGet(() -> caseRoot.join(CourtCaseEntity_.hearings, JoinType.INNER));
    }

    private Join<CourtCaseEntity, JudgeEntity> joinJudge(Root<CourtCaseEntity> caseRoot) {
        return caseRoot.join(CourtCaseEntity_.JUDGES, JoinType.INNER);
    }

    private Join<CourtCaseEntity, UserAccountEntity> joinUser(Root<CourtCaseEntity> caseRoot) {
        //case -> courthouse -> securityGroups -> user
        Join<CourtCaseEntity, CourthouseEntity> courthouseJoin = joinCourthouse(caseRoot);

        Join<CourthouseEntity, SecurityGroupEntity> securityGroupJoin = courthouseJoin.join(
            CourthouseEntity_.SECURITY_GROUPS,
            JoinType.INNER
        );
        return securityGroupJoin.join(SecurityGroupEntity_.USERS, JoinType.INNER);
    }


    @SuppressWarnings("unchecked")
    private Join<HearingEntity, CourtroomEntity> joinCourtroom(Root<CourtCaseEntity> caseRoot) {
        Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);

        Optional<Join<HearingEntity, ?>> foundJoin = hearingJoin.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            HearingEntity_.COURTROOM)).findAny();
        return foundJoin.map(hearingEntityJoin -> (Join<HearingEntity, CourtroomEntity>) hearingEntityJoin)
            .orElseGet(() -> hearingJoin.join(HearingEntity_.COURTROOM, JoinType.INNER));

    }

    @SuppressWarnings("unchecked")
    private Join<CourtCaseEntity, CourthouseEntity> joinCourthouse(Root<CourtCaseEntity> caseRoot) {
        Optional<Join<CourtCaseEntity, ?>> foundJoin = caseRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            CourtroomEntity_.COURTHOUSE)).findAny();
        return foundJoin.map(join -> (Join<CourtCaseEntity, CourthouseEntity>) join)
            .orElseGet(() -> caseRoot.join(CourtroomEntity_.COURTHOUSE, JoinType.INNER));
    }

    private Join<CourtCaseEntity, DefendantEntity> joinDefendantEntity(Root<CourtCaseEntity> caseRoot) {
        return caseRoot.join(CourtCaseEntity_.DEFENDANT_LIST, JoinType.INNER);
    }

    private Join<CourtCaseEntity, EventEntity> joinEventEntity(Root<CourtCaseEntity> caseRoot) {
        Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);
        return hearingJoin.join(HearingEntity_.EVENT_LIST, JoinType.INNER);
    }
}
