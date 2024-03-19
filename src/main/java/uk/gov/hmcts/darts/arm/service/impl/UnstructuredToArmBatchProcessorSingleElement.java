package uk.gov.hmcts.darts.arm.service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UnstructuredToArmBatchProcessorSingleElement {


    @Transactional
    public void insertArmRecord(Integer eodId, String manifestFileName) {

    }
}
