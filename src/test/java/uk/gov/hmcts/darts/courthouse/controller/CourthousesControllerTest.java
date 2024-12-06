package uk.gov.hmcts.darts.courthouse.controller;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.CourthouseTitleErrors;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class CourthousesControllerTest {

    @InjectMocks
    private CourthousesController controller;

    @Mock
    private CourthouseService courthouseService;

    @Test
    void testGetCourthouseByIdSuccess() {
        var cth = new AdminCourthouse();
        cth.setId(1);
        Mockito.when(courthouseService.getAdminCourtHouseById(1)).thenReturn(cth);
        ResponseEntity<AdminCourthouse> response = controller.adminCourthousesCourthouseIdGet(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetCourthouseByIdNotFound() {
        Mockito.when(courthouseService.getAdminCourtHouseById(1)).thenThrow(new EntityNotFoundException());
        var exception = assertThrows(
            DartsApiException.class,
            () -> controller.adminCourthousesCourthouseIdGet(1)
        );

        assertEquals(CourthouseTitleErrors.COURTHOUSE_NOT_FOUND.toString(), exception.getMessage());
        assertEquals(COURTHOUSE_NOT_FOUND, exception.getError());
    }

    @Test
    void adminCourthousesPost_shouldThrowDartsApiException_whenCourthouseNameIsLowercase() {
        var request = new CourthousePost();
        request.setCourthouseName("london crown court");

        var exception = assertThrows(
            DartsApiException.class,
            () -> controller.adminCourthousesPost(request)
        );

        assertEquals(CourthouseApiError.INVALID_REQUEST, exception.getError());
    }

    @Test
    void updateCourthouse_shouldThrowDartsApiException_whenCourthouseNameIsLowercase() {
        var request = new CourthousePatch();
        request.setCourthouseName("london crown court");

        var exception = assertThrows(
            DartsApiException.class,
            () -> controller.updateCourthouse(1, request)
        );

        assertEquals(CourthouseApiError.INVALID_REQUEST, exception.getError());
    }
}
