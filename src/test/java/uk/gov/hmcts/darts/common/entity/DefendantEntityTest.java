package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.test.common.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantEntityTest {

    @Test
    void positiveAnonymize() {
        DefendantEntity defendantEntity = new DefendantEntity();
        defendantEntity.setName("name");

        UserAccountEntity userAccount = new UserAccountEntity();
        defendantEntity.anonymize(userAccount);
        assertThat(defendantEntity.getName()).matches(TestUtils.UUID_REGEX);
    }
}
