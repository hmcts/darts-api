package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RetentionConfidenceCategoryEnum {

    // Modernised values
    CASE_CLOSED(1), MANUAL_OVERRIDE(2), AGED_CASE(3),

    // Legacy values
    LEGACY_MANUAL_OVERRIDE_21(21),
    LEGACY_MANUAL_OVERRIDE_22(22),
    LEGACY_CASE_CLOSED_23(23),
    LEGACY_CASE_CLOSED_42(42),
    LEGACY_CASE_CLOSED_43(43),
    LEGACY_CASE_CLOSED_31(31),
    LEGACY_MAX_EVENT_CLOSED_32(32),
    LEGACY_MAX_MEDIA_CLOSED_33(33),
    LEGACY_CASE_CREATION_CLOSED_34(34),
    LEGACY_CASE_CLOSED_11(11),
    LEGACY_MAX_EVENT_CLOSED_12(12),
    LEGACY_MAX_MEDIA_CLOSED_13(13),
    LEGACY_CASE_CREATION_CLOSED_14(14),
    LEGACY_CASE_CLOSED_61(61),
    LEGACY_MAX_EVENT_CLOSED_62(62),
    LEGACY_MAX_MEDIA_CLOSED_63(63),
    LEGACY_CASE_CREATION_CLOSED_64(64);

    private final Integer id;
    }

