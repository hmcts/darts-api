package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.cases.model.AdvSearchReqMapperTables;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.SQLQueryModel;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

@Slf4j
@UtilityClass
public class AdvancedSearchRequestMapper {


    public SQLQueryModel mapToSQLQueryModel(GetCasesSearchRequest request) {
        SQLQueryModel sqlModel = new SQLQueryModel();
        AdvSearchReqMapperTables advSearchReqMapperTables = new AdvSearchReqMapperTables();
        sqlModel.addSelectColumn("distinct(court_case.cas_id)");
        sqlModel.addTable(CaseEntity.TABLE_NAME);
        addCaseCriteria(request, advSearchReqMapperTables, sqlModel);
        addCourtroomCriteria(request, advSearchReqMapperTables, sqlModel);
        addCourthouseCriteria(request, advSearchReqMapperTables, sqlModel);
        addHearingCriteria(request, advSearchReqMapperTables, sqlModel);
        addDefendantCriteria(request, advSearchReqMapperTables, sqlModel);
        addEventCriteria(request, advSearchReqMapperTables, sqlModel);


        if (advSearchReqMapperTables.isJoinJudges()) {
            sqlModel.addTable("( select cas_id,unnest(judge_list) as judge from hearing) as judges");
            sqlModel.addWhereCriteria("judges.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (advSearchReqMapperTables.isJoinDefendants()) {
            sqlModel.addTable("( select cas_id,unnest(defendant_list) as defendant from court_case) as defendants");
            sqlModel.addWhereCriteria("defendants.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (advSearchReqMapperTables.isJoinHearing()) {
            sqlModel.addTable("hearing");
            sqlModel.addWhereCriteria("hearing.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (advSearchReqMapperTables.isJoinCourtroom()) {
            sqlModel.addTable("courtroom");
            sqlModel.addWhereCriteria("hearing.ctr_id = " + CourtroomEntity.TABLE_NAME + ".ctr_id");
        }
        if (advSearchReqMapperTables.isJoinCourthouse()) {
            sqlModel.addTable("courthouse");
            sqlModel.addWhereCriteria("courthouse.cth_id = " + CaseEntity.TABLE_NAME + ".cth_id");
        }
        if (advSearchReqMapperTables.isJoinEvents()) {
            sqlModel.addTable("hearing_event_ae");
            sqlModel.addTable("event");
            sqlModel.addWhereCriteria("hearing_event_ae.hea_id = " + HearingEntity.TABLE_NAME + ".hea_id");
            sqlModel.addWhereCriteria("hearing_event_ae.eve_id = " + "event.eve_id");
        }
        return sqlModel;

    }

    private void addCaseCriteria(GetCasesSearchRequest request, AdvSearchReqMapperTables advSearchReqMapperTables, SQLQueryModel sqlModel) {
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            String parameterName = "caseNumber";
            sqlModel.addWhereCriteria("upper(court_case.case_number) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundWithPercent(request.getCaseNumber()));
        }
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    private void addCourtroomCriteria(GetCasesSearchRequest request, AdvSearchReqMapperTables advSearchReqMapperTables, SQLQueryModel sqlModel) {
        if (StringUtils.isNotBlank(request.getCourtroom())) {
            advSearchReqMapperTables.setJoinCourtroom(true);
            advSearchReqMapperTables.setJoinHearing(true);
            String parameterName = "courtroom";
            sqlModel.addWhereCriteria("upper(courtroom.courtroom_name) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundWithPercent(request.getCourtroom()));
        }
    }

    private void addCourthouseCriteria(GetCasesSearchRequest request, AdvSearchReqMapperTables advSearchReqMapperTables, SQLQueryModel sqlModel) {
        if (StringUtils.isNotBlank(request.getCourthouse())) {
            advSearchReqMapperTables.setJoinCourthouse(true);
            String parameterName = "courthouse";
            sqlModel.addWhereCriteria("upper(courthouse.courthouse_name) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundWithPercent(request.getCourthouse()));
        }
    }

    private String surroundWithPercent(String value) {
        return surroundValue(value, "%");
    }

    private void addHearingCriteria(GetCasesSearchRequest request, AdvSearchReqMapperTables advSearchReqMapperTables, SQLQueryModel sqlModel) {
        if (StringUtils.isNotBlank(request.getJudgeName())) {
            advSearchReqMapperTables.setJoinJudges(true);

            String parameterName = "judge";
            sqlModel.addWhereCriteria("upper(judges.judge) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundWithPercent(request.getJudgeName()));
        }
        addHearingDateCriteria(request, advSearchReqMapperTables, sqlModel);

    }

    private void addDefendantCriteria(GetCasesSearchRequest request, AdvSearchReqMapperTables advSearchReqMapperTables, SQLQueryModel sqlModel) {
        if (StringUtils.isNotBlank(request.getDefendantName())) {
            advSearchReqMapperTables.setJoinDefendants(true);

            String parameterName = "defendant";
            sqlModel.addWhereCriteria("upper(defendants.defendant) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundWithPercent(request.getDefendantName()));
        }
    }

    private void addEventCriteria(GetCasesSearchRequest request, AdvSearchReqMapperTables advSearchReqMapperTables, SQLQueryModel sqlModel) {
        if (StringUtils.isNotBlank(request.getEventTextContains())) {
            advSearchReqMapperTables.setJoinHearing(true);
            advSearchReqMapperTables.setJoinEvents(true);

            String parameterName = "keyword";
            sqlModel.addWhereCriteria("upper(event.event_text) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundWithPercent(request.getEventTextContains()));
        }
    }

    private void addHearingDateCriteria(GetCasesSearchRequest request, AdvSearchReqMapperTables advSearchReqMapperTables, SQLQueryModel sqlModel) {
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            advSearchReqMapperTables.setJoinHearing(true);
            if (request.getDateFrom() != null) {
                String parameterName = "dateFrom";
                sqlModel.addWhereCriteria("hearing.hearing_date >= date(:" + parameterName + ")");
                sqlModel.addParameter(parameterName, surroundWithPercent(request.getDateFrom().toString()));
            }
            if (request.getDateTo() != null) {
                String parameterName = "dateTo";
                sqlModel.addWhereCriteria("hearing.hearing_date <= date(:" + parameterName + ")");
                sqlModel.addParameter(parameterName, surroundWithPercent(request.getDateTo().toString()));
            }
        }
    }


}
