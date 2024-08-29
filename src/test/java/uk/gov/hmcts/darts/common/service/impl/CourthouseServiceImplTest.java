package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourthouseServiceImplTest {

    @Mock
    private CourthouseRepository courthouseRepository;

    private CourthouseServiceImpl courthouseService;

    private CourthouseEntity courthouse;

    @BeforeEach
    void setUp() {
        courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("Test Courthouse");
        courthouseService = new CourthouseServiceImpl(courthouseRepository);
    }

    @Test
    void retrieveCourthouseExistingCourthouseReturnsEntity() {
        when(courthouseRepository.findByCourthouseNameIgnoreCase("Test Courthouse"))
            .thenReturn(Optional.of(courthouse));

        CourthouseEntity result = courthouseService.retrieveCourthouse("Test Courthouse");

        assertNotNull(result);
        assertEquals("Test Courthouse".toUpperCase(), result.getCourthouseName());
        verify(courthouseRepository).findByCourthouseNameIgnoreCase("Test Courthouse");
    }

    @Test
    void retrieveCourthouseNonExistentCourthouseThrowsException() {
        when(courthouseRepository.findByCourthouseNameIgnoreCase("Non-existent Courthouse"))
            .thenReturn(Optional.empty());

        DartsApiException exception = assertThrows(DartsApiException.class,
                                                   () -> courthouseService.retrieveCourthouse("Non-existent Courthouse"));

        assertEquals(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST, exception.getError());
        assertEquals("Provided courthouse does not exist. Courthouse 'Non-existent Courthouse' not found.", exception.getMessage());
        verify(courthouseRepository).findByCourthouseNameIgnoreCase("Non-existent Courthouse");
    }

    @Test
    void retrieveCourthouseCaseInsensitiveSearch() {
        when(courthouseRepository.findByCourthouseNameIgnoreCase("test courthouse"))
            .thenReturn(Optional.of(courthouse));

        CourthouseEntity result = courthouseService.retrieveCourthouse("test courthouse");

        assertNotNull(result);
        assertEquals("Test Courthouse".toUpperCase(), result.getCourthouseName());
        verify(courthouseRepository).findByCourthouseNameIgnoreCase("test courthouse");
    }

    @Test
    void retrieveCourthouseNullInputThrowsException() {
        DartsApiException exception = assertThrows(DartsApiException.class,
                                                      () -> courthouseService.retrieveCourthouse(null));

        assertEquals("Provided courthouse does not exist. Courthouse 'null' not found.", exception.getMessage());
        verify(courthouseRepository).findByCourthouseNameIgnoreCase(null);
    }
}
