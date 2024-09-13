package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CourtCaseEntityTest {

    @Test
    void positiveAnonymize() {
        CourtCaseEntity courtCase = new CourtCaseEntity();

        DefendantEntity defendantEntity1 = mock(DefendantEntity.class);
        DefendantEntity defendantEntity2 = mock(DefendantEntity.class);
        courtCase.setDefendantList(List.of(defendantEntity1, defendantEntity2));

        DefenceEntity defenceEntity1 = mock(DefenceEntity.class);
        DefenceEntity defenceEntity2 = mock(DefenceEntity.class);
        courtCase.setDefenceList(List.of(defenceEntity1, defenceEntity2));

        ProsecutorEntity prosecutorEntity1 = mock(ProsecutorEntity.class);
        ProsecutorEntity prosecutorEntity2 = mock(ProsecutorEntity.class);
        courtCase.setProsecutorList(List.of(prosecutorEntity1, prosecutorEntity2));

        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        courtCase.setHearings(List.of(hearingEntity1, hearingEntity2));

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(123);
        UUID uuid = UUID.randomUUID();

        courtCase.anonymize(userAccount, uuid);
        assertThat(courtCase.isDataAnonymised()).isTrue();
        assertThat(courtCase.getDataAnonymisedBy()).isEqualTo(123);
        assertThat(courtCase.getDataAnonymisedTs()).isCloseToUtcNow(within(5, SECONDS));

        verify(defendantEntity1, times(1)).anonymize(userAccount, uuid);
        verify(defendantEntity2, times(1)).anonymize(userAccount, uuid);

        verify(defenceEntity1, times(1)).anonymize(userAccount, uuid);
        verify(defenceEntity2, times(1)).anonymize(userAccount, uuid);
        verify(prosecutorEntity1, times(1)).anonymize(userAccount, uuid);
        verify(prosecutorEntity2, times(1)).anonymize(userAccount, uuid);
        verify(hearingEntity1, times(1)).anonymize(userAccount, uuid);
        verify(hearingEntity2, times(1)).anonymize(userAccount, uuid);
    }
}
