package uk.gov.hmcts.darts.retention.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum.UNKNOWN;

@Converter(autoApply = true)
public class RetentionConfidenceCategoryEnumConverter implements AttributeConverter<RetentionConfidenceCategoryEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RetentionConfidenceCategoryEnum retentionConfidenceCategoryEnum) {
        if (retentionConfidenceCategoryEnum == null) {
            return UNKNOWN.getId();
        }
        return retentionConfidenceCategoryEnum.getId();
    }

    @Override
    public RetentionConfidenceCategoryEnum convertToEntityAttribute(Integer id) {
        if (id == null) {
            return UNKNOWN;
        }

        return Stream.of(RetentionConfidenceCategoryEnum.values())
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(UNKNOWN);
    }
    
}