package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.test.common.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class DefenceEntityTest {

    @Test
    void positiveAnonymize() {
        DefenceEntity defenceEntity = new DefenceEntity();
        defenceEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();

        defenceEntity.anonymize(userAccount);
        assertThat(defenceEntity.getName()).matches(TestUtils.UUID_REGEX);
    }
}
