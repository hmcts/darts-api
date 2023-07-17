package uk.gov.hmcts.darts.cases.mapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.SQLQueryModel;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

@Slf4j
public class AdvancedSearchRequestMapper {

    private boolean joinHearing = false;
    private boolean joinCourtroom = false;
    private boolean joinCourthouse = false;
    private boolean joinJudges = false;
    private boolean joinDefendants = false;
    private boolean joinEvents = false;

    SQLQueryModel sqlModel = new SQLQueryModel();

    public SQLQueryModel mapToSQL(GetCasesSearchRequest request) {
        sqlModel.addSelectColumn("distinct(court_case.cas_id)");
        sqlModel.addTable(CaseEntity.TABLE_NAME);
        addCaseCriteria(request);
        addCourtroomCriteria(request);
        addCourthouseCriteria(request);
        addHearingCriteria(request);
        addDefendantCriteria(request);
        addEventCriteria(request);


        if (joinJudges) {
            sqlModel.addTable("( select cas_id,unnest(judge_list) as judge from hearing) as judges");
            sqlModel.addWhereCriteria("judges.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (joinDefendants) {
            sqlModel.addTable("( select cas_id,unnest(defendant_list) as defendant from court_case) as defendants");
            sqlModel.addWhereCriteria("defendants.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (joinHearing) {
            sqlModel.addTable("hearing");
            sqlModel.addWhereCriteria("hearing.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (joinCourtroom) {
            sqlModel.addTable("courtroom");
            sqlModel.addWhereCriteria("hearing.ctr_id = " + CourtroomEntity.TABLE_NAME + ".ctr_id");
        }
        if (joinCourthouse) {
            sqlModel.addTable("courthouse");
            sqlModel.addWhereCriteria("courthouse.cth_id = " + CaseEntity.TABLE_NAME + ".cth_id");
        }
        if (joinEvents) {
            sqlModel.addTable("hearing_event_ae");
            sqlModel.addTable("event");
            sqlModel.addWhereCriteria("hearing_event_ae.hea_id = " + HearingEntity.TABLE_NAME + ".hea_id");
            sqlModel.addWhereCriteria("hearing_event_ae.eve_id = " + "event.eve_id");
        }
        return sqlModel;

    }

    private void addCaseCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            String parameterName = "caseNumber";
            sqlModel.addWhereCriteria("upper(court_case.case_number) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundValue(request.getCaseNumber(), "%"));
        }
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    private void addCourtroomCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getCourtroom())) {
            joinCourtroom = true;
            joinHearing = true;
            String parameterName = "courtroom";
            sqlModel.addWhereCriteria("upper(courtroom.courtroom_name) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundValue(request.getCourtroom(), "%"));
        }
    }

    private void addCourthouseCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getCourthouse())) {
            joinCourthouse = true;
            String parameterName = "courthouse";
            sqlModel.addWhereCriteria("upper(courthouse.courthouse_name) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundValue(request.getCourthouse(), "%"));
        }
    }

    private void addHearingCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getJudgeName())) {
            joinHearing = true;
            joinJudges = true;

            String parameterName = "judge";
            sqlModel.addWhereCriteria("upper(judges.judge) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundValue(request.getJudgeName(), "%"));
        }
        addHearingDateCriteria(request);

    }

    private void addDefendantCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getDefendantName())) {
            joinHearing = true;
            joinDefendants = true;

            String parameterName = "defendant";
            sqlModel.addWhereCriteria("upper(defendants.defendant) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundValue(request.getDefendantName(), "%"));
        }
    }

    private void addEventCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getKeywords())) {
            joinHearing = true;
            joinEvents = true;

            String parameterName = "keyword";
            sqlModel.addWhereCriteria("upper(defendants.defendant) like upper(:" + parameterName + ")");
            sqlModel.addParameter(parameterName, surroundValue(request.getKeywords(), "%"));
        }
    }

    private void addHearingDateCriteria(GetCasesSearchRequest request) {
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            joinHearing = true;
            if (request.getDateFrom() != null) {
                String parameterName = "dateFrom";
                sqlModel.addWhereCriteria("hearing.hearing_date >= date(:" + parameterName + ")");
                sqlModel.addParameter(parameterName, surroundValue(request.getDateFrom().toString(), "%"));
            }
            if (request.getDateTo() != null) {
                String parameterName = "dateTo";
                sqlModel.addWhereCriteria("hearing.hearing_date <= date(:" + parameterName + ")");
                sqlModel.addParameter(parameterName, surroundValue(request.getDateTo().toString(), "%"));
            }
        }
    }


}
