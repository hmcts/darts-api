package uk.gov.hmcts.darts.audio.repository;

public enum TransformedMediaQueryEnum {
    COURT_HOUSE("Court Name:",  "postfix courthouse"),
    OWNER( "Owner", "post owner"),
    REQUESTED_BY("Requested By", "post requested by");

    private final String prefix;

    private final String postfix;

    TransformedMediaQueryEnum (String prefix, String postfix) {
        this.prefix = prefix;
        this.postfix = postfix;
    }

    public String getQueryString(String content) {
        return prefix + content + postfix;
    }

    public String getQueryStringPrefix(String content) {
        return prefix + content;
    }

    public String getQueryStringPostfix(String content) {
        return postfix + content;
    }

    public String getQueryStringPrefix() {
        return prefix;
    }

    public String getQueryStringPostfix() {
        return postfix;
    }
}