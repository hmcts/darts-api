package uk.gov.hmcts.darts.cases.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SQLQueryModel {
    List<String> selectColumns = new ArrayList<>();
    List<String> fromTables = new ArrayList<>();
    List<String> whereCriteria = new ArrayList<>();

    public void addTable(String tableName) {
        fromTables.add(tableName);
    }

    public void addSelectColumn(String selectColumn) {
        selectColumns.add(selectColumn);
    }

    public void addWhereCriteria(String criteria) {
        whereCriteria.add(criteria);
    }

    public String toString() {
        return "SELECT " + String.join(", ", selectColumns) + "\n" +
            "FROM " + String.join(", ", fromTables.stream().distinct().toList()) + "\n" +
            "WHERE " + String.join("\r\nAND ", whereCriteria);
    }
}
