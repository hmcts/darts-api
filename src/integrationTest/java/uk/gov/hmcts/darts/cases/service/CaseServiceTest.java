package uk.gov.hmcts.darts.cases.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseServiceTest {

    @Autowired
    CaseService service;

    @Autowired
    CourtroomRepository courtroomRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetCasesOk1() throws IOException {
        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse("Swansea");
        request.setCourtroom("1");
        request.setDate(LocalDate.of(2023, 6, 20));

        List<ScheduledCase> resultList = service.getCases(request);
        String actualResponse = objectMapper.writeValueAsString(resultList);
        String expectedResponse = getContentsFromFile("tests/cases/CaseServiceTest/getCasesOk1/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testGetCasesOk2() throws IOException {
        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse("Swansea");
        request.setCourtroom("2");
        request.setDate(LocalDate.of(2023, 6, 20));

        List<ScheduledCase> resultList = service.getCases(request);
        String actualResponse = objectMapper.writeValueAsString(resultList);
        String expectedResponse = getContentsFromFile("tests/cases/CaseServiceTest/getCasesOk2/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testGetCasesCreateCourtroom() throws IOException {
        String courthouseName = "Swansea";
        String courtroomName = "99";

        CourtroomEntity foundCourtroom = courtroomRepository.findByNames(courthouseName, courtroomName);
        assertNull(foundCourtroom);

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(courthouseName);
        request.setCourtroom(courtroomName);
        request.setDate(LocalDate.of(2023, 6, 20));

        List<ScheduledCase> resultList = service.getCases(request);
        assertEquals(0, resultList.size());
        foundCourtroom = courtroomRepository.findByNames(courthouseName, courtroomName);
        assertEquals(courtroomName.toUpperCase(Locale.ROOT), foundCourtroom.getName());
        assertEquals(courthouseName.toUpperCase(Locale.ROOT), foundCourtroom.getCourthouse().getCourthouseName());
    }
}
