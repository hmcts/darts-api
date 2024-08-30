package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeCommonServiceImplTest {

    @Mock
    private JudgeRepository judgeRepository;

    private JudgeCommonServiceImpl judgeService;

    private UserAccountEntity userAccount;
    private JudgeEntity existingJudge;

    @BeforeEach
    void setUp() {
        userAccount = new UserAccountEntity();
        existingJudge = new JudgeEntity();
        existingJudge.setName("JUDGE SMITH");

        judgeService = new JudgeCommonServiceImpl(judgeRepository);
    }

    @Test
    void retrieveOrCreateJudgeExistingJudge() {
        when(judgeRepository.findByNameIgnoreCase("Judge Smith")).thenReturn(Optional.of(existingJudge));

        JudgeEntity result = judgeService.retrieveOrCreateJudge("Judge Smith", userAccount);

        assertNotNull(result);
        assertEquals("JUDGE SMITH", result.getName());
        verify(judgeRepository).findByNameIgnoreCase("Judge Smith");
        verify(judgeRepository, never()).saveAndFlush(any(JudgeEntity.class));
    }

    @Test
    void retrieveOrCreateJudgeNewJudge() {
        when(judgeRepository.findByNameIgnoreCase("Judge Brown")).thenReturn(Optional.empty());
        when(judgeRepository.saveAndFlush(any(JudgeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JudgeEntity result = judgeService.retrieveOrCreateJudge("Judge Brown", userAccount);

        assertNotNull(result);
        assertEquals("JUDGE BROWN", result.getName());
        assertEquals(userAccount, result.getCreatedBy());
        assertEquals(userAccount, result.getLastModifiedBy());
        verify(judgeRepository).findByNameIgnoreCase("Judge Brown");
        verify(judgeRepository).saveAndFlush(any(JudgeEntity.class));
    }

    @Test
    void retrieveOrCreateJudgeCaseInsensitivity() {
        when(judgeRepository.findByNameIgnoreCase("judge smith")).thenReturn(Optional.of(existingJudge));

        JudgeEntity result = judgeService.retrieveOrCreateJudge("judge smith", userAccount);

        assertNotNull(result);
        assertEquals("JUDGE SMITH", result.getName());
        verify(judgeRepository).findByNameIgnoreCase("judge smith");
    }

    @Test
    void retrieveOrCreateJudgeNullJudgeName() {
        when(judgeRepository.findByNameIgnoreCase(null)).thenReturn(Optional.empty());
        when(judgeRepository.saveAndFlush(any(JudgeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JudgeEntity result = judgeService.retrieveOrCreateJudge(null, userAccount);

        assertNotNull(result);
        assertNull(result.getName());
        assertEquals(userAccount, result.getCreatedBy());
        assertEquals(userAccount, result.getLastModifiedBy());
        verify(judgeRepository).findByNameIgnoreCase(null);
        verify(judgeRepository).saveAndFlush(any(JudgeEntity.class));
    }

    @Test
    void retrieveOrCreateJudgeEmptyJudgeName() {
        when(judgeRepository.findByNameIgnoreCase("")).thenReturn(Optional.empty());
        when(judgeRepository.saveAndFlush(any(JudgeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JudgeEntity result = judgeService.retrieveOrCreateJudge("", userAccount);

        assertNotNull(result);
        assertEquals("", result.getName());
        assertEquals(userAccount, result.getCreatedBy());
        assertEquals(userAccount, result.getLastModifiedBy());
        verify(judgeRepository).findByNameIgnoreCase("");
        verify(judgeRepository).saveAndFlush(any(JudgeEntity.class));
    }
}