package com.digixmed.icu.cvprint.dto;

/**
 * 前端登记表一行数据（源数据 + 编辑后数据 合并后的结果）
 */
public class CriticalValueRow {

    /** criticalValue._id */
    private String sourceId;

    private String checkDate;      // 检查日期
    private String patientName;    // 姓名
    private String deptText;       // 科室
    private String bedText;        // 床号
    private String inpatientNo;    // 住院号
    private String lisItem;        // 危急值项目
    private String value;          // 危急值结果
    private String repeatResult;   // 复述结果
    private String reporter;       // 报告人
    private String callTime;       // 接电话时间
    private String callName;       // 接电话姓名
    private String reportDoctor;   // 报告医生
    private String handled;        // 是否处置 √ / ×

    /** 是否已有编辑记录 */
    private boolean edited;

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getCheckDate() { return checkDate; }
    public void setCheckDate(String checkDate) { this.checkDate = checkDate; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDeptText() { return deptText; }
    public void setDeptText(String deptText) { this.deptText = deptText; }
    public String getBedText() { return bedText; }
    public void setBedText(String bedText) { this.bedText = bedText; }
    public String getInpatientNo() { return inpatientNo; }
    public void setInpatientNo(String inpatientNo) { this.inpatientNo = inpatientNo; }
    public String getLisItem() { return lisItem; }
    public void setLisItem(String lisItem) { this.lisItem = lisItem; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getRepeatResult() { return repeatResult; }
    public void setRepeatResult(String repeatResult) { this.repeatResult = repeatResult; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getCallTime() { return callTime; }
    public void setCallTime(String callTime) { this.callTime = callTime; }
    public String getCallName() { return callName; }
    public void setCallName(String callName) { this.callName = callName; }
    public String getReportDoctor() { return reportDoctor; }
    public void setReportDoctor(String reportDoctor) { this.reportDoctor = reportDoctor; }
    public String getHandled() { return handled; }
    public void setHandled(String handled) { this.handled = handled; }
    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }
}
