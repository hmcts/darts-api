package uk.gov.hmcts.darts.cases.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.service.ClosedCasesToArmProcessor;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClosedCasesToArmProcessorImpl implements ClosedCasesToArmProcessor {

    private final CaseRepository caseRepository;

    @Override
    @Transactional
    public void closedCasesToArm() {
        List<CourtCaseEntity> courtCaseEntityList = caseRepository.findClosedCases(OffsetDateTime.now().minusDays(7));
        courtCaseEntityList.forEach(this::createJsonStringFromCourtCaseEntity);
    }

    private void createJsonStringFromCourtCaseEntity(CourtCaseEntity courtCaseEntity) {

        ObjectMapper mapper = getObjectMapper();

        courtCaseEntity.setUserAccountCourtCaseEntities(null);

        String json = "";
        try {
            json = mapper.writeValueAsString(courtCaseEntity);
        } catch (Exception ex) {
            log.error("error");
        }
        log.info(json);
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }





}
