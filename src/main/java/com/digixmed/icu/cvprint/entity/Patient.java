package com.digixmed.icu.cvprint.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * SmartCare.patient 集合（只读，只映射本程序需要的字段）
 *
 * 关联关系：
 *   criticalValue.pid == patient.hisPid
 *   patient._id       == nurseRecords.pid
 */
@Document(collection = "patient")
public class Patient {

    @Id
    private String id;

    /** HIS 患者标识，对应 criticalValue.pid */
    private String hisPid;

    /** 住院号 */
    private String mrn;

    /** 姓名 */
    private String name;

    private String gender;

    private String dept;

    private String deptCode;

    private String hisBed;

    private String hospitalTime;

    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHisPid() { return hisPid; }
    public void setHisPid(String hisPid) { this.hisPid = hisPid; }
    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }
    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
    public String getHisBed() { return hisBed; }
    public void setHisBed(String hisBed) { this.hisBed = hisBed; }
    public String getHospitalTime() { return hospitalTime; }
    public void setHospitalTime(String hospitalTime) { this.hospitalTime = hospitalTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
