package uk.gov.hmcts.darts.retention.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyTest {
    @Test
    void retrievePolicyDetails() {
        Policy permanantPolicy = Policy.getForPolicyName("DARTS Permanent Retention v3");
        Policy standardPolicy = Policy.getForPolicyName("DARTS Standard Retention v3");

        assertThat(permanantPolicy.name()).isEqualTo(String.valueOf(Policy.PERMANENT));
        assertThat(standardPolicy.name()).isEqualTo(String.valueOf(Policy.STANDARD));

    }
}
