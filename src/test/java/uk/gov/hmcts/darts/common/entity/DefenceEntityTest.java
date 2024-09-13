package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefenceEntityTest {

    @Test
    void positiveAnonymize() {
        DefenceEntity defenceEntity = new DefenceEntity();
        defenceEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        UUID uuid = UUID.randomUUID();

        defenceEntity.anonymize(userAccount, uuid);
        assertThat(defenceEntity.getName()).isEqualTo(uuid.toString());
    }
}
