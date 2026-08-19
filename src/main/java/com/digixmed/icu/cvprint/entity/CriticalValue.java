package com.digixmed.icu.cvprint.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * 源数据：SmartCare 库 criticalValue 集合（只读）
 */
@Document(collection = "criticalValue")
public class CriticalValue {

    @Id
    private String id;

    /** 危急值项目 */
    private String lisItem;

    /** 危急值结果 */
    private String value;

    /** 住院号 / 患者标识 */
    private String pid;

    /** 床号 */
    private String bed;

    /** 科室 */
    private String deptName;

    /** 报告医生 */
    private String doctor;

    /** 备注 */
    private String comment;

    /** 发布（检查）时间 */
    private Date publishTime;

    /** 处理（接电话）时间 */
    private Date handleTime;

    /** true=已处置，false=未处置 */
    private Boolean status;

    private String reportId;

    private String testItemId;

    private String itemCode;

    /** 大项目名称 */
    private String bigItemName;

    @Field("fromView")
    private Boolean fromView;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLisItem() { return lisItem; }
    public void setLisItem(String lisItem) { this.lisItem = lisItem; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }
    public String getBed() { return bed; }
    public void setBed(String bed) { this.bed = bed; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Date getPublishTime() { return publishTime; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
    public Date getHandleTime() { return handleTime; }
    public void setHandleTime(Date handleTime) { this.handleTime = handleTime; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getTestItemId() { return testItemId; }
    public void setTestItemId(String testItemId) { this.testItemId = testItemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getBigItemName() { return bigItemName; }
    public void setBigItemName(String bigItemName) { this.bigItemName = bigItemName; }
    public Boolean getFromView() { return fromView; }
    public void setFromView(Boolean fromView) { this.fromView = fromView; }
}
