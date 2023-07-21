package uk.gov.hmcts.darts.event.model;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourtroomCourthouseCourtcaseTest {

    private CourtroomCourthouseCourtcase courtroomCourthouseCourtcase;

    @Mock
    private CourthouseEntity mockCourthouseEntity;
    @Mock
    private CourtroomEntity mockCourtroomEntity;
    @Mock
    private CaseEntity mockCaseEntity;

    @Test
    void getCourthouseEntity() {
        courtroomCourthouseCourtcase = CourtroomCourthouseCourtcase.builder()
            .courthouseEntity(mockCourthouseEntity)
            .build();

        assertEquals(mockCourthouseEntity, courtroomCourthouseCourtcase.getCourthouseEntity());
    }

    @Test
    void getCourtroomEntity() {
        courtroomCourthouseCourtcase = CourtroomCourthouseCourtcase.builder()
            .courtroomEntity(mockCourtroomEntity)
            .build();

        assertEquals(mockCourtroomEntity, courtroomCourthouseCourtcase.getCourtroomEntity());
    }

    @Test
    void getCaseEntity() {
        courtroomCourthouseCourtcase = CourtroomCourthouseCourtcase.builder()
            .caseEntity(mockCaseEntity)
            .build();

        assertEquals(mockCaseEntity, courtroomCourthouseCourtcase.getCaseEntity());
    }

    @Test
    void isHearingNew() {
        boolean hearingIsNew = true;
        courtroomCourthouseCourtcase = CourtroomCourthouseCourtcase.builder()
            .isHearingNew(hearingIsNew)
            .build();

        assertEquals(hearingIsNew, courtroomCourthouseCourtcase.isHearingNew());
    }

    @Test
    void isCourtroomDifferentFromHearing() {
        boolean courtroomDifferentFromHearing = true;
        courtroomCourthouseCourtcase = CourtroomCourthouseCourtcase.builder()
            .isCourtroomDifferentFromHearing(courtroomDifferentFromHearing)
            .build();

        assertEquals(courtroomDifferentFromHearing, courtroomCourthouseCourtcase.isCourtroomDifferentFromHearing());
    }


}
