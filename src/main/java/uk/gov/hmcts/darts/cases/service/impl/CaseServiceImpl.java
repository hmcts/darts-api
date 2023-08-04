package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.exception.CaseError;
import uk.gov.hmcts.darts.cases.helper.AdvancedSearchRequestHelper;
import uk.gov.hmcts.darts.cases.mapper.AdvancedSearchResponseMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.mapper.HearingEntityToCaseHearing;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.util.CourtCaseUtil;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class CaseServiceImpl implements CaseService {

    private static final int MAX_RESULTS = 500;
    private final CasesMapper casesMapper;

    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final AdvancedSearchRequestHelper advancedSearchRequestHelper;

    @Override
    @Transactional
    public List<ScheduledCase> getHearings(GetCasesRequest request) {

        List<HearingEntity> hearings = hearingRepository.findByCourthouseCourtroomAndDate(
            request.getCourthouse(),
            request.getCourtroom(),
            request.getDate()
        );
        createCourtroomIfMissing(hearings, request);
        return casesMapper.mapToCourtCases(hearings);

    }

    @Override
    public List<Hearing> getCaseHearings(Integer caseId) {

        List<HearingEntity> hearingList = hearingRepository.findByCaseIds(List.of(caseId));

        return HearingEntityToCaseHearing.mapToHearingList(hearingList);

    }

    @Override
    public SingleCase getCasesById(Integer caseId) {

        Optional<CourtCaseEntity> caseEntity = caseRepository.findById(caseId);

        if (caseEntity.isEmpty()) {
            throw new DartsApiException(CaseError.CASE_NOT_FOUND);
        }

        return casesMapper.mapToSingleCase(caseEntity.get());
    }

    private void createCourtroomIfMissing(List<HearingEntity> hearings, GetCasesRequest request) {
        if (CollectionUtils.isEmpty(hearings)) {
            //find out if courthouse or courtroom are missing.
            retrieveCoreObjectService.retrieveOrCreateCourtroom(request.getCourthouse(), request.getCourtroom());
        }
        // return casesMapper.mapToCourtCases(hearings);
    }


    private ScheduledCase addCase(AddCaseRequest addCaseRequest) {
        CourtCaseEntity caseEntity = saveNewCaseEntity(addCaseRequest);
        return casesMapper.mapToCourtCase(caseEntity);
    }

    @Transactional
    @Override
    public ScheduledCase addCaseOrUpdate(AddCaseRequest addCaseRequest) {
        Optional<CourtCaseEntity> existingCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseName(
            addCaseRequest.getCaseNumber(), addCaseRequest.getCourthouse());
        if (existingCase.isPresent()) {
            return updateCase(addCaseRequest, existingCase.get());
        } else {
            return addCase(addCaseRequest);
        }
    }

    private ScheduledCase updateCase(AddCaseRequest addCaseRequest, CourtCaseEntity existingCase) {
        CourtCaseEntity updatedCaseEntity = updateCaseEntity(addCaseRequest, existingCase);

        return casesMapper.mapToCourtCase(updatedCaseEntity);
    }


    private CourtCaseEntity updateCaseEntity(AddCaseRequest addCaseRequest, CourtCaseEntity existingCase) {
        return caseRepository.saveAndFlush(casesMapper.mapAddCaseRequestToCaseEntity(addCaseRequest, existingCase));
    }

    private CourtCaseEntity saveNewCaseEntity(AddCaseRequest addCaseRequest) {
        CourtCaseEntity caseEntity = casesMapper.mapAddCaseRequestToCaseEntity(addCaseRequest, new CourtCaseEntity());
        return caseRepository.saveAndFlush(caseEntity);
    }

    @Override
    public List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request) {
        List<CourtCaseEntity> courtCaseEntities = advancedSearchRequestHelper.getMatchingCourtCases(request);
        if (courtCaseEntities.size() > MAX_RESULTS) {
            throw new DartsApiException(CaseError.TOO_MANY_RESULTS);
        }
        if (courtCaseEntities.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> caseIds = CourtCaseUtil.getCaseIdList(courtCaseEntities);
        List<HearingEntity> hearings = hearingRepository.findByCaseIds(caseIds);
        return AdvancedSearchResponseMapper.mapResponse(hearings);
    }


}
