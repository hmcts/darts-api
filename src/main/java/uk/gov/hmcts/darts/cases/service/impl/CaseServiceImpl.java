package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.mapper.AdvancedSearchRequestMapper;
import uk.gov.hmcts.darts.cases.mapper.AdvancedSearchResponseMapper;
import uk.gov.hmcts.darts.cases.mapper.GetCasesMapper;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.SQLQueryModel;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.api.CommonApi;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final CommonApi commonApi;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<ScheduledCase> getCases(GetCasesRequest request) {

        List<HearingEntity> hearings = hearingRepository.findByCourthouseCourtroomAndDate(
            request.getCourthouse(),
            request.getCourtroom(),
            request.getDate()
        );
        createCourtroomIfMissing(hearings, request);
        return GetCasesMapper.mapToCourtCases(hearings);

    }

    private void createCourtroomIfMissing(List<HearingEntity> hearings, GetCasesRequest request) {
        if (CollectionUtils.isEmpty(hearings)) {
            //find out if courthouse or courtroom are missing.
            commonApi.retrieveOrCreateCourtroom(request.getCourthouse(), request.getCourtroom());
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request) {
        AdvancedSearchRequestMapper mapper = new AdvancedSearchRequestMapper();
        SQLQueryModel getCaseIdsQueryModel = mapper.mapToSQL(request);
        Query caseIdQuery
            = entityManager.createNativeQuery(getCaseIdsQueryModel.toString(), String.class);
        getCaseIdsQueryModel.populateParameters(caseIdQuery);
        List<String> caseIdsStr = caseIdQuery.getResultList();
        List<Integer> caseIds = caseIdsStr.stream().map(caseId -> Integer.parseInt(caseId)).toList();
        List<HearingEntity> hearings = hearingRepository.findByCaseIds(caseIds);
        List<AdvancedSearchResult> advancedSearchResults = AdvancedSearchResponseMapper.mapResponse(hearings);

        return advancedSearchResults;

    }


}
