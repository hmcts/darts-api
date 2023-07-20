package uk.gov.hmcts.darts.cases.model;

import jakarta.persistence.Query;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SQLQueryModel {
    List<String> selectColumns = new ArrayList<>();
    List<String> fromTables = new ArrayList<>();
    List<String> whereCriteria = new ArrayList<>();
    Map<String, Integer> parameterNames = new HashMap<>();
    List<ParameterValue> parameters = new ArrayList<>();

    public void addTable(String tableName) {
        fromTables.add(tableName);
    }

    public void addSelectColumn(String selectColumn) {
        selectColumns.add(selectColumn);
    }

    public void addWhereCriteria(String criteria) {
        whereCriteria.add(criteria);
    }

    public void addParameter(String name, String value) {
        parameters.add(new ParameterValue(name, value));
    }

    public String toString() {
        return "SELECT " + String.join(", ", selectColumns) + "\n" +
            "FROM " + String.join(", ", fromTables.stream().distinct().toList()) + "\n" +
            "WHERE " + String.join("\nAND ", whereCriteria);
    }

    public void populateParameters(Query caseIdQuery) {
        for (ParameterValue parameter : parameters) {
            caseIdQuery.setParameter(parameter.getName(), parameter.getValue());
        }
    }
}
