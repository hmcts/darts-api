package uk.gov.hmcts.darts.arm.util;

import org.springframework.stereotype.Component;

@Component
public class ArmRpoUtil {

    private static final String CREATE_EXPORT_CSV_EXTENSION = "_CSV";

    public String generateUniqueProductionName(String productionName) {
        return productionName + "_" + CREATE_EXPORT_CSV_EXTENSION;
    }
}
