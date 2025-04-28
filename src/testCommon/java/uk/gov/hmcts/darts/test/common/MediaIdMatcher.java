package uk.gov.hmcts.darts.test.common;

import org.mockito.ArgumentMatcher;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

public class MediaIdMatcher implements ArgumentMatcher<MediaEntity> {

    private final Long id;

    public MediaIdMatcher(Long id) {
        this.id = id;

    }

    @Override
    public boolean matches(MediaEntity mediaEntity) {
        return mediaEntity != null && mediaEntity.getId().equals(id);
    }
}