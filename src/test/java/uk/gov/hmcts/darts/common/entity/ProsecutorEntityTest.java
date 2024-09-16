package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.test.common.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ProsecutorEntityTest {

    @Test
    void positiveAnonymize() {
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();

        prosecutorEntity.anonymize(userAccount);
        assertThat(prosecutorEntity.getName()).matches(TestUtils.UUID_REGEX);
    }
}
