package uk.gov.hmcts.darts.common.util;

public interface TreeNode {
    Integer getId();

    String getAntecedent();

    default  boolean doesHaveAntecedent() {
        return getAntecedent() != null;
    }
}