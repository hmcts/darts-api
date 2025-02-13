package uk.gov.hmcts.darts.common.util.paginated;


public interface IsPageable {

    SortMethod getSortMethod();

    SortMethod.HasComparableExpression getSortField();

    long getPageLimit();

    long getPageNumber();
}
