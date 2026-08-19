package com.digixmed.icu.cvprint.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * SmartCare.nurseRecords 护理记录（只读）
 *
 * pid = patient._id；desc 中记录“接检验科报危急值…”，username 即接电话护士姓名。
 */
@Document(collection = "nurseRecords")
public class NurseRecords {

    @Id
    private String id;

    /** 护士姓名（接电话姓名取该字段） */
    private String username;

    /** 工号 */
    private String trueName;

    private String userId;

    /** 对应 patient._id */
    private String pid;

    /** 患者姓名 */
    private String name;

    /** 护理记录内容，用于模糊匹配危急值 */
    private String desc;

    /** 记录时间 */
    private Date time;

    private Date createTime;

    private String professions;

    private Boolean valid;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTrueName() { return trueName; }
    public void setTrueName(String trueName) { this.trueName = trueName; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public Date getTime() { return time; }
    public void setTime(Date time) { this.time = time; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getProfessions() { return professions; }
    public void setProfessions(String professions) { this.professions = professions; }
    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }
}
