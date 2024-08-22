package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.DefenceRepository;
import uk.gov.hmcts.darts.common.repository.DefendantRepository;
import uk.gov.hmcts.darts.common.repository.ProsecutorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCoreObjectServiceImplTest {

    @Mock
    DefenceRepository defenceRepository;
    @Mock
    DefendantRepository defendantRepository;
    @Mock
    ProsecutorRepository prosecutorRepository;

    CreateCoreObjectServiceImpl createCoreObjectService;

    UserAccountEntity userAccount;
    CourtCaseEntity courtCase;

    @BeforeEach
    void setUp() {
        createCoreObjectService = new CreateCoreObjectServiceImpl(defenceRepository, defendantRepository, prosecutorRepository);
        userAccount = new UserAccountEntity();
        userAccount.setId(1);

        courtCase = new CourtCaseEntity();
        courtCase.setId(1);
    }

    @Test
    void testCreateDefence() {
        // Given
        String defenceName = "Test Defence";
        DefenceEntity defenceEntity = new DefenceEntity();
        defenceEntity.setName(defenceName);
        defenceEntity.setCourtCase(courtCase);
        defenceEntity.setCreatedBy(userAccount);
        defenceEntity.setLastModifiedBy(userAccount);

        when(defenceRepository.saveAndFlush(any(DefenceEntity.class))).thenReturn(defenceEntity);

        // When
        DefenceEntity createdDefence = createCoreObjectService.createDefence(defenceName, courtCase, userAccount);

        // Then
        assertEquals(defenceName, createdDefence.getName());
        assertEquals(courtCase, createdDefence.getCourtCase());
        assertEquals(userAccount, createdDefence.getCreatedBy());
        assertEquals(userAccount, createdDefence.getLastModifiedBy());

        verify(defenceRepository, times(1)).saveAndFlush(any(DefenceEntity.class));
    }

    @Test
    void testCreateDefendant() {
        // Given
        String defendantName = "Test Defendant";
        DefendantEntity defendantEntity = new DefendantEntity();
        defendantEntity.setName(defendantName);
        defendantEntity.setCourtCase(courtCase);
        defendantEntity.setCreatedBy(userAccount);
        defendantEntity.setLastModifiedBy(userAccount);

        when(defendantRepository.saveAndFlush(any(DefendantEntity.class))).thenReturn(defendantEntity);

        // When
        DefendantEntity createdDefendant = createCoreObjectService.createDefendant(defendantName, courtCase, userAccount);

        // Then
        assertEquals(defendantName, createdDefendant.getName());
        assertEquals(courtCase, createdDefendant.getCourtCase());
        assertEquals(userAccount, createdDefendant.getCreatedBy());
        assertEquals(userAccount, createdDefendant.getLastModifiedBy());

        verify(defendantRepository, times(1)).saveAndFlush(any(DefendantEntity.class));
    }

    @Test
    void testCreateProsecutor() {
        // Given
        String prosecutorName = "Test Prosecutor";
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setName(prosecutorName);
        prosecutorEntity.setCourtCase(courtCase);
        prosecutorEntity.setCreatedBy(userAccount);
        prosecutorEntity.setLastModifiedBy(userAccount);

        when(prosecutorRepository.saveAndFlush(any(ProsecutorEntity.class))).thenReturn(prosecutorEntity);

        // When
        ProsecutorEntity createdProsecutor = createCoreObjectService.createProsecutor(prosecutorName, courtCase, userAccount);

        // Then
        assertEquals(prosecutorName, createdProsecutor.getName());
        assertEquals(courtCase, createdProsecutor.getCourtCase());
        assertEquals(userAccount, createdProsecutor.getCreatedBy());
        assertEquals(userAccount, createdProsecutor.getLastModifiedBy());

        verify(prosecutorRepository, times(1)).saveAndFlush(any(ProsecutorEntity.class));
    }
}
