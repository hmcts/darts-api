package uk.gov.hmcts.darts.arm.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ArmRpoUtil {

    private static final String CREATE_EXPORT_CSV_EXTENSION = "_CSV";

    public String generateUniqueProductionName(String productionName) {
        return productionName + "_" + UUID.randomUUID().toString() + CREATE_EXPORT_CSV_EXTENSION;
    }
}
