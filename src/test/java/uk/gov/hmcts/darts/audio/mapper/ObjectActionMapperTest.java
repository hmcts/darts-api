package uk.gov.hmcts.darts.audio.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObjectActionMapperTest {

    private ObjectActionMapper adminMediaMapper;

    @BeforeEach
    void setUp() {
        adminMediaMapper = new ObjectActionMapperImpl();
    }

    @Test
    void shouldReturnFirstAdminActionEntityOnly() {
        // Given
        ObjectAdminActionEntity entity1 = new ObjectAdminActionEntity();
        entity1.setId(1);

        ObjectAdminActionEntity entity2 = new ObjectAdminActionEntity();
        entity2.setId(2);

        List<ObjectAdminActionEntity> entities = Arrays.asList(entity1, entity2);

        // When
        var actionResponse = adminMediaMapper.toApiModel(entities);

        // Then
        assertEquals(1, actionResponse.getId());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void shouldReturnNull(List<ObjectAdminActionEntity> objectAdminActionEntities) {
        var adminActionResponse = adminMediaMapper.toApiModel(objectAdminActionEntities);

        assertNull(adminActionResponse);
    }

}