package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProsecutorEntityTest {

    @Test
    void positiveAnonymize() {
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        UUID uuid = UUID.randomUUID();

        prosecutorEntity.anonymize(userAccount, uuid);
        assertThat(prosecutorEntity.getName()).isEqualTo(uuid.toString());
    }
}
