package com.digixmed.icu.cvprint.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 登记表编辑结果集合：criticalValueReport
 *
 * 该集合不需要提前创建，第一次保存时 MongoDB 会自动创建；
 * 启动时 MongoIndexInitializer 会保证集合与索引存在。
 *
 * 与源数据的关系：sourceId = criticalValue._id（一行一条，上测覆盖）。
 * 源集合 criticalValue 不做任何修改。
 */
@Document(collection = "criticalValueReport")
@CompoundIndex(name = "idx_pid_publishTime", def = "{'pid': 1, 'publishTime': -1}")
public class CriticalValueReport {

    @Id
    private String id;

    /** 源记录 criticalValue._id，唯一 */
    @Indexed(unique = true)
    private String sourceId;

    // ---- 快照字段（方便单独查询登记表）----
    private String pid;
    private String bed;
    private String deptName;
    private Date publishTime;

    // ---- 登记表 13 列的可编辑内容 ----
    /** 检查日期（文本，默认取 publishTime） */
    private String checkDate;
    /** 姓名 */
    private String patientName;
    /** 科室 */
    private String deptText;
    /** 床号 */
    private String bedText;
    /** 住院号 */
    private String inpatientNo;
    /** 危急值项目 */
    private String lisItem;
    /** 危急值结果 */
    private String value;
    /** 复述结果 */
    private String repeatResult;
    /** 报告人 */
    private String reporter;
    /** 接电话时间 */
    private String callTime;
    /** 接电话姓名 */
    private String callName;
    /** 报告医生 */
    private String reportDoctor;
    /** 是否处置：√ / × */
    private String handled;

    /**
     * 已被人工编辑过的字段名列表。
     * 只有在该列表里的字段才会覆盖系统值；
     * 其余字段始终跟随 criticalValue / patient / nurseRecords 的最新数据。
     */
    private java.util.List<String> editedFields;

    // ---- 审计字段 ----
    /** 最后编辑人（postMessage 中的 account） */
    private String updatedBy;
    private Date createTime;
    private Date updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }
    public String getBed() { return bed; }
    public void setBed(String bed) { this.bed = bed; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Date getPublishTime() { return publishTime; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
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
    public java.util.List<String> getEditedFields() { return editedFields; }
    public void setEditedFields(java.util.List<String> editedFields) { this.editedFields = editedFields; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
