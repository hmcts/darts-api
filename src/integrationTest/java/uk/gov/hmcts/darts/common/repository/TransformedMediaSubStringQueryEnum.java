package uk.gov.hmcts.darts.common.repository;

public enum TransformedMediaSubStringQueryEnum {
    COURT_HOUSE("Court Name:", "post courthouse"),
    OWNER("Owner", "post owner"),
    REQUESTED_BY("Requested By", "post requested by");

    private final String prefix;

    private final String postfix;

    TransformedMediaSubStringQueryEnum(String prefix, String postfix) {
        this.prefix = prefix;
        this.postfix = postfix;
    }

    public String getQueryString(String content) {
        return prefix + content + postfix;
    }

    public String getQueryStringPrefix(String content) {
        return prefix + content;
    }

    public String getQueryStringPrefix() {
        return prefix;
    }

    public String getQueryStringPostfix() {
        return postfix;
    }

    public String getQueryStringPostfix(String content) {
        return content + postfix;
    }
}