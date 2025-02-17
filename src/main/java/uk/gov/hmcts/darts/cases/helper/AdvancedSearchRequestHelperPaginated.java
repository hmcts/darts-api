package uk.gov.hmcts.darts.cases.helper;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.mapper.AdvancedSearchResponseMapper;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequestPaginated;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.QDefendantEntity;
import uk.gov.hmcts.darts.common.entity.QEventEntity;
import uk.gov.hmcts.darts.common.entity.QHearingEntity;
import uk.gov.hmcts.darts.common.entity.QJudgeEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.util.paginated.PaginatedList;
import uk.gov.hmcts.darts.common.util.paginated.PaginationUtil;
import uk.gov.hmcts.darts.common.util.paginated.SortMethod;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@Component
@SuppressWarnings({"PMD.TooManyMethods"})
@RequiredArgsConstructor
@Slf4j
public class AdvancedSearchRequestHelperPaginated {
    @PersistenceContext
    private final EntityManager entityManager;

    private final AuthorisationApi authorisationApi;
    private final CourthouseRepository courthouseRepository;

    public PaginatedList<AdvancedSearchResult> getMatchingCourtCases(GetCasesSearchRequestPaginated request, int maxResults) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        JPQLQuery<HearingEntity> query = queryFactory.selectFrom(QHearingEntity.hearingEntity);
        addFilters(query, request);

        return PaginationUtil.toPaginatedList(
            query.select(QHearingEntity.hearingEntity.courtCase),
            request,
            GetCasesSearchRequestPaginated.SortField.CASE_NUMBER,
            SortMethod.DESC,
            courtCase -> AdvancedSearchResponseMapper.mapToAdvancedSearchResult(courtCase),
            Long.valueOf(maxResults)
        );
    }

    private void addFilters(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        addCourthouseAccessCriteria(query);
        addHearingIsActualCriteria(query);
        addUseInterpreterCriteria(query);
        addCaseCriteria(query, request);
        addHearingDateCriteria(query, request);
        addCourtroomNameCriteria(query, request);
        addCourthouseIdCriteria(query, request);
        addJudgeCriteria(query, request);
        addDefendantCriteria(query, request);
        addEventCriteria(query, request);
    }


    private void addCourtroomNameCriteria(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        if (StringUtils.isNotBlank(request.getCourtroom())) {
            query.where(QHearingEntity.hearingEntity.courtroom.name.toUpperCase().like(surroundWithPercentages(request.getCourtroom(), true)));
        }
    }

    private void addCourthouseIdCriteria(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        if (StringUtils.isNotBlank(request.getCourthouse())) {
            query.where(Expressions.anyOf(
                QHearingEntity.hearingEntity.courtCase.courthouse.courthouseName.toUpperCase().like(surroundWithPercentages(request.getCourthouse(), true)),
                QHearingEntity.hearingEntity.courtCase.courthouse.displayName.toUpperCase().like(surroundWithPercentages(request.getCourthouse(), true))
            ));
        }
    }

    private void addCourthouseAccessCriteria(JPQLQuery<HearingEntity> query) {
        //add courthouse permissions
        List<Integer> courthouseIdsUserHasAccessTo = authorisationApi.getListOfCourthouseIdsUserHasAccessTo();
        query.where(QHearingEntity.hearingEntity.courtCase.courthouse.id.in(courthouseIdsUserHasAccessTo));
    }

    private void addCaseCriteria(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            query.where(QHearingEntity.hearingEntity.courtCase.caseNumber.toUpperCase().like(surroundWithPercentages(request.getCaseNumber(), true)));
        }
    }

    private String surroundWithPercentages(String value, boolean makeUpperCase) {
        String updatedValue = surroundValue(value, "%");
        if (makeUpperCase) {
            updatedValue = updatedValue.toUpperCase();
        }
        return updatedValue;
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    private void addDefendantCriteria(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        if (StringUtils.isNotBlank(request.getDefendantName())) {
            query = joinDefendantEntity(query);
            query.where(QDefendantEntity.defendantEntity.name.toUpperCase().like(surroundWithPercentages(request.getDefendantName(), true)));
        }
    }

    private void addEventCriteria(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        if (StringUtils.isNotBlank(request.getEventTextContains())) {
            query = joinEventEntity(query);
            query.where(QEventEntity.eventEntity.eventText.toUpperCase().like(surroundWithPercentages(request.getEventTextContains(), true)));
        }
    }

    private void addJudgeCriteria(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        if (StringUtils.isNotBlank(request.getJudgeName())) {
            query = joinJudge(query);
            query.where(QJudgeEntity.judgeEntity.name.toUpperCase().like(surroundWithPercentages(request.getJudgeName(), true)));
        }
    }

    private void addHearingDateCriteria(JPQLQuery<HearingEntity> query, GetCasesSearchRequestPaginated request) {
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            if (request.getDateFrom() != null) {
                query.where(QHearingEntity.hearingEntity.hearingDate.goe(request.getDateFrom()));
            }
            if (request.getDateTo() != null) {
                query.where(QHearingEntity.hearingEntity.hearingDate.loe(request.getDateTo()));
            }
        }
    }

    private void addUseInterpreterCriteria(JPQLQuery<HearingEntity> query) {
        if (authorisationApi.userHasOneOfRoles(List.of(TRANSLATION_QA))) {
            query.where(QHearingEntity.hearingEntity.courtCase.interpreterUsed.isTrue());
        }
    }

    private void addHearingIsActualCriteria(JPQLQuery<HearingEntity> query) {
        query.where(QHearingEntity.hearingEntity.hearingIsActual.isTrue());
    }


    private JPQLQuery<HearingEntity> joinJudge(JPQLQuery<HearingEntity> query) {
        return query.join(QJudgeEntity.judgeEntity).on(QHearingEntity.hearingEntity.courtCase.judges.contains(QJudgeEntity.judgeEntity));
    }


    private JPQLQuery<HearingEntity> joinDefendantEntity(JPQLQuery<HearingEntity> query) {
        return query.join(QDefendantEntity.defendantEntity).on(QHearingEntity.hearingEntity.courtCase.defendantList.contains(QDefendantEntity.defendantEntity));
    }

    private JPQLQuery<HearingEntity> joinEventEntity(JPQLQuery<HearingEntity> query) {
        return query.join(QEventEntity.eventEntity).on(QHearingEntity.hearingEntity.eventList.contains(QEventEntity.eventEntity));
    }
}
