package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;

@ExtendWith(MockitoExtension.class)
class RemoveAdminActionComponentTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private ObjectAdminActionRepository adminActionRepository;

    @Mock
    private AuditApi auditApi;

    private RemoveAdminActionComponent removeAdminActionComponent;

    @BeforeEach
    void setUp() {
        removeAdminActionComponent = new RemoveAdminActionComponent(mediaRepository,
                                                                    adminActionRepository,
                                                                    auditApi);
    }

    @Nested
    class RemoveAdminActionFromAllVersionsTests {

        @BeforeEach
        void setUp() {

        }

        @Test
        void test() {
            // TODO: Implement tests
        }

    }

}