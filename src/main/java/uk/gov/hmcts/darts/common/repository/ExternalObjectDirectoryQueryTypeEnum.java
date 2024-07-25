package uk.gov.hmcts.darts.common.repository;

import lombok.Getter;

@Getter
public enum ExternalObjectDirectoryQueryTypeEnum {

    MEDIA_QUERY(1), ANNOTATION_QUERY(2);
    private final int index;

    ExternalObjectDirectoryQueryTypeEnum(int index) {
        this.index = index;
    }
}