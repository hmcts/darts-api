package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.service.CourthouseCommonService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtroomCommonServiceImplTest {

    @Mock
    private CourtroomRepository courtroomRepository;

    @Mock
    private CourthouseCommonService courthouseCommonService;

    private CourtroomCommonServiceImpl courtroomService;

    private CourthouseEntity courthouse;
    private UserAccountEntity userAccount;
    private CourtroomEntity existingCourtroom;

    @BeforeEach
    void setUp() {
        courthouse = new CourthouseEntity();
        courthouse.setId(1);
        courthouse.setCourthouseName("Test Courthouse");

        userAccount = new UserAccountEntity();

        existingCourtroom = new CourtroomEntity();
        existingCourtroom.setName("COURTROOM 1");
        existingCourtroom.setCourthouse(courthouse);

        courtroomService = new CourtroomCommonServiceImpl(courtroomRepository, courthouseCommonService);
    }

    @Test
    void retrieveOrCreateCourtroomWithCourthouseExistingCourtroom() {
        when(courtroomRepository.findByNameAndId(1, "COURTROOM 1"))
            .thenReturn(Optional.of(existingCourtroom));

        CourtroomEntity result = courtroomService.retrieveOrCreateCourtroom(courthouse, "Courtroom 1", userAccount);

        assertNotNull(result);
        assertEquals("COURTROOM 1", result.getName());
        assertEquals(courthouse, result.getCourthouse());
        verify(courtroomRepository, never()).saveAndFlush(any(CourtroomEntity.class));
    }

    @Test
    void retrieveOrCreateCourtroomWithCourthouseNewCourtroom() {
        when(courtroomRepository.findByNameAndId(1, "COURTROOM 2"))
            .thenReturn(Optional.empty());
        when(courtroomRepository.saveAndFlush(any(CourtroomEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtroomEntity result = courtroomService.retrieveOrCreateCourtroom(courthouse, "Courtroom 2", userAccount);

        assertNotNull(result);
        assertEquals("COURTROOM 2", result.getName());
        assertEquals(courthouse, result.getCourthouse());
        assertEquals(userAccount, result.getCreatedBy());
        verify(courtroomRepository).saveAndFlush(any(CourtroomEntity.class));
    }

    @Test
    void retrieveOrCreateCourtroomWithCourthouseNameExistingCourtroom() {
        when(courtroomRepository.findByCourthouseNameAndCourtroomName("Test Courthouse", "COURTROOM 1"))
            .thenReturn(Optional.of(existingCourtroom));

        CourtroomEntity result = courtroomService.retrieveOrCreateCourtroom("Test Courthouse", "Courtroom 1", userAccount);

        assertNotNull(result);
        assertEquals("COURTROOM 1", result.getName());
        assertEquals(courthouse, result.getCourthouse());
        verify(courthouseCommonService, never()).retrieveCourthouse(anyString());
        verify(courtroomRepository, never()).saveAndFlush(any(CourtroomEntity.class));
    }

    @Test
    void retrieveOrCreateCourtroomWithCourthouseNameNewCourtroom() {
        when(courtroomRepository.findByCourthouseNameAndCourtroomName("Test Courthouse", "COURTROOM 2"))
            .thenReturn(Optional.empty());
        when(courthouseCommonService.retrieveCourthouse("Test Courthouse"))
            .thenReturn(courthouse);
        when(courtroomRepository.saveAndFlush(any(CourtroomEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtroomEntity result = courtroomService.retrieveOrCreateCourtroom("Test Courthouse", "Courtroom 2", userAccount);

        assertNotNull(result);
        assertEquals("COURTROOM 2", result.getName());
        assertEquals(courthouse, result.getCourthouse());
        assertEquals(userAccount, result.getCreatedBy());
        verify(courthouseCommonService).retrieveCourthouse("Test Courthouse");
        verify(courtroomRepository).saveAndFlush(any(CourtroomEntity.class));
    }

    @Test
    void retrieveOrCreateCourtroomCaseInsensitivity() {
        when(courtroomRepository.findByNameAndId(1, "COURTROOM 1"))
            .thenReturn(Optional.of(existingCourtroom));

        CourtroomEntity result = courtroomService.retrieveOrCreateCourtroom(courthouse, "courtroom 1", userAccount);

        assertNotNull(result);
        assertEquals("COURTROOM 1", result.getName());
        assertEquals(courthouse, result.getCourthouse());
    }
}