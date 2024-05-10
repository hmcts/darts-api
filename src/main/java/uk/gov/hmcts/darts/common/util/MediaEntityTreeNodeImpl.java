package uk.gov.hmcts.darts.common.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

@EqualsAndHashCode
@Getter
public class MediaEntityTreeNodeImpl implements TreeNode {

    @Getter
    @EqualsAndHashCode.Exclude
    private final MediaEntity entity;

    private final Integer id;

    public MediaEntityTreeNodeImpl(MediaEntity entity) {
        this.entity = entity;
        id = this.entity.getId();

    }

    @Override
    public Integer getId() {
        return entity.getId();
    }

    @Override
    public String getAntecedent() {
        return entity.getAntecedentId();
    }

    @Override
    public boolean doesHaveAntecedent() {
        return entity.getAntecedentId() != null;
    }

}