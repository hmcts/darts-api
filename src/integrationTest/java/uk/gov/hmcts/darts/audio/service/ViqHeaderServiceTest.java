package uk.gov.hmcts.darts.audio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

@SuppressWarnings({"PMD.TestClassWithoutTestCases"})
@Slf4j
class ViqHeaderServiceTest extends IntegrationBase {
    @Autowired
    ViqHeaderService viqHeaderService;

}
