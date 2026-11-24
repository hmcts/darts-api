package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIAL_CONDUCT;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;

@AutoConfigureMockMvc
class JudicialConductSearchIntTest extends IntegrationBase {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 20);
    private static final String CASE_NUMBER_PREFIX = "JCO-GLOBAL-CASE";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GivenBuilder given;

    private CourthouseEntity firstCourthouse;
    private int courtroomIndex;

    @BeforeEach
    void setUp() {
        firstCourthouse = createCourthouseWithName("JCO-COURTHOUSE-ONE");
        CourthouseEntity secondCourthouse = createCourthouseWithName("JCO-COURTHOUSE-TWO");

        createCaseWithHearing(firstCourthouse, "JCO-SIMPLE-CASE", false);
        createCaseWithHearing(firstCourthouse, CASE_NUMBER_PREFIX + "-ONE", false);
        createCaseWithHearing(secondCourthouse, CASE_NUMBER_PREFIX + "-TWO", false);
        createCaseWithHearing(secondCourthouse, CASE_NUMBER_PREFIX + "-INTERPRETER", true);

        given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIAL_CONDUCT);
    }

    @Test
    void casesSearchPost_shouldReturnSimpleSearchResults_whenUserHasOnlyJudicialConductRole() throws Exception {
        mockMvc.perform(post("/cases/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "case_number": "JCO-SIMPLE-CASE"
                                }
                                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].case_number").value("JCO-SIMPLE-CASE"));
    }

    @Test
    void casesSearchPost_shouldReturnAdvancedSearchResults_whenUserHasOnlyJudicialConductRole() throws Exception {
        mockMvc.perform(post("/cases/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "courthouse_ids": [%d],
                                  "date_from": "2023-06-20",
                                  "date_to": "2023-06-20"
                                }
                                """.formatted(firstCourthouse.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].case_number", containsInAnyOrder("JCO-SIMPLE-CASE", CASE_NUMBER_PREFIX + "-ONE")));
    }

    @Test
    void casesSearchPost_shouldReturnSearchError_whenUserHasOnlyJudicialConductRole() throws Exception {
        mockMvc.perform(post("/cases/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "courthouse_ids": [%d],
                                  "date_from": "2023-06-21",
                                  "date_to": "2023-06-20"
                                }
                                """.formatted(firstCourthouse.getId())))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void casesSearchPost_shouldReturnAllNonInterpreterCasesAcrossCourts_whenUserHasOnlyJudicialConductRole() throws Exception {
        mockMvc.perform(post("/cases/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "case_number": "JCO-GLOBAL-CASE"
                                }
                                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].case_number", containsInAnyOrder(
                CASE_NUMBER_PREFIX + "-ONE",
                CASE_NUMBER_PREFIX + "-TWO"
            )))
            .andExpect(jsonPath("$[*].courthouse", containsInAnyOrder("JCO-COURTHOUSE-ONE", "JCO-COURTHOUSE-TWO")));
    }

    private void createCaseWithHearing(CourthouseEntity courthouse, String caseNumber, boolean interpreterUsed) {
        CourtCaseEntity courtCase = PersistableFactory.getCourtCaseTestData().createCaseAt(courthouse, caseNumber);
        courtCase.setInterpreterUsed(interpreterUsed);
        CourtroomEntity courtroom = createCourtRoomWithNameAtCourthouse(courthouse, "JCO-ROOM-" + courtroomIndex++);
        HearingEntity hearing = PersistableFactory.getHearingTestData().createHearingWith(courtCase, courtroom, HEARING_DATE);
        dartsPersistence.save(hearing);
    }
}
