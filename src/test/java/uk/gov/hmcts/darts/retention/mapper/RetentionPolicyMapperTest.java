package uk.gov.hmcts.darts.retention.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.OffsetDateTime.parse;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RetentionPolicyMapperTest {

    public static final OffsetDateTime POLICY_START_AT = parse("2024-03-01T10:00:00Z");
    public static final OffsetDateTime POLICY_END_AT = parse("2024-04-01T10:00:00Z");
    private RetentionPolicyMapper retentionPolicyMapper;

    @BeforeEach
    void setUp() {
        retentionPolicyMapper = new RetentionPolicyMapper();
    }

    @Test
    void mapsRetentionPolicyEntityToRetentionPolicyCorrectly() {

        RetentionPolicyTypeEntity retentionPolicyTypeEntity = getRetentionPolicyTypeEntity();

        assertThat(retentionPolicyMapper.mapToRetentionPolicyResponse(List.of(retentionPolicyTypeEntity)).get(0))
            .hasFieldOrPropertyWithValue("id", 1)
            .hasFieldOrPropertyWithValue("name", "DARTS Permanent Retention v3")
            .hasFieldOrPropertyWithValue("displayName", "Legacy Permanent")
            .hasFieldOrPropertyWithValue("description","DARTS Permanent retention policy")
            .hasFieldOrPropertyWithValue("fixedPolicyKey", "-1")
            .hasFieldOrPropertyWithValue("duration", "30Y0M0D")
            .hasFieldOrPropertyWithValue("policyStartAt", POLICY_START_AT)
            .hasFieldOrPropertyWithValue("policyEndAt", POLICY_END_AT);
    }

    private RetentionPolicyTypeEntity getRetentionPolicyTypeEntity() {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = new RetentionPolicyTypeEntity();


        retentionPolicyTypeEntity.setId(1);
        retentionPolicyTypeEntity.setDisplayName("Legacy Permanent");
        retentionPolicyTypeEntity.setPolicyName("DARTS Permanent Retention v3");
        retentionPolicyTypeEntity.setPolicyStart(OffsetDateTime.of(2024, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        retentionPolicyTypeEntity.setPolicyEnd(OffsetDateTime.of(2024, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        retentionPolicyTypeEntity.setDescription("DARTS Permanent retention policy");
        retentionPolicyTypeEntity.setFixedPolicyKey("-1");
        retentionPolicyTypeEntity.setDuration("30Y0M0D");
        retentionPolicyTypeEntity.setDescription("DARTS Permanent retention policy");
        return retentionPolicyTypeEntity;
    }

}
