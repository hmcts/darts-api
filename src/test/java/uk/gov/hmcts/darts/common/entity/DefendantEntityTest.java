package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantEntityTest {

    @Test
    void positiveAnonymize() {
        DefendantEntity defendantEntity = new DefendantEntity();
        defendantEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        UUID uuid = UUID.randomUUID();

        defendantEntity.anonymize(userAccount, uuid);
        assertThat(defendantEntity.getName()).isEqualTo(uuid.toString());
    }
}
