package com.digixmed.icu.cvprint.dto;

/**
 * 单元格编辑自动保存请求体
 */
public class SaveCellRequest {

    /** criticalValue._id，必传 */
    private String sourceId;

    /** 字段名，如 repeatResult / reporter / callTime ... */
    private String field;

    /** 新值 */
    private String value;

    /** 当前登录账号（postMessage account） */
    private String account;

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
}
