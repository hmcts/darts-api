package uk.gov.hmcts.darts.testutils.stubs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

@Component
@Deprecated
public class ObjectHiddenReasonStub {

    @Autowired
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    public ObjectHiddenReasonEntity getAnyWithMarkedForDeletion(boolean markedForDeletion) {
        return objectHiddenReasonRepository.findAll().stream()
            .filter(objectHiddenReasonEntity -> markedForDeletion == objectHiddenReasonEntity.isMarkedForDeletion())
            .findFirst()
            .orElseThrow();
    }

}