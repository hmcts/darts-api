package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaEntityTest {

    @Test
    void getObjectAdminAction_shouldReturnEmptyOptional_whenNoAdminActionsAreSet() {
        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .build()
            .getEntity();

        Optional<ObjectAdminActionEntity> adminActionOptional = media.getObjectAdminAction();
        assertTrue(adminActionOptional.isEmpty());
    }

    @Test
    void getObjectAdminAction_shouldReturnAdminAction_whenSingularAdminActionIsSet() {
        ObjectAdminActionEntity adminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();
        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .objectAdminActions(Collections.singletonList(adminAction))
            .build()
            .getEntity();

        Optional<ObjectAdminActionEntity> adminActionOptional = media.getObjectAdminAction();
        assertTrue(adminActionOptional.isPresent());

        ObjectAdminActionEntity retrievedAdminAction = adminActionOptional.get();
        assertEquals(adminAction, retrievedAdminAction);
    }

    @Test
    void getObjectAdminAction_shouldReturnFirstAdminAction_whenMultipleAdminActionsAreSet() {
        ObjectAdminActionEntity firstAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();
        ObjectAdminActionEntity otherAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();

        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .objectAdminActions(List.of(firstAdminAction, otherAdminAction))
            .build()
            .getEntity();

        Optional<ObjectAdminActionEntity> adminActionOptional = media.getObjectAdminAction();
        assertTrue(adminActionOptional.isPresent());

        ObjectAdminActionEntity retrievedAdminAction = adminActionOptional.get();
        assertEquals(firstAdminAction, retrievedAdminAction);
    }

    @Test
    void setObjectAdminAction_shouldSetSingularAdminAction_whenSomeAlreadyExist() {
        ObjectAdminActionEntity existingAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();
        ObjectAdminActionEntity newAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();

        List<ObjectAdminActionEntity> adminActions = new ArrayList<>();
        adminActions.add(existingAdminAction);

        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .objectAdminActions(adminActions)
            .build()
            .getEntity();

        media.setObjectAdminAction(newAdminAction);

        assertEquals(1, media.getObjectAdminActions().size());
        assertEquals(newAdminAction, media.getObjectAdminActions().getFirst());
    }

}