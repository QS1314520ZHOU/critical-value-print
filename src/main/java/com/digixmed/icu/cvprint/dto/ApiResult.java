package com.digixmed.icu.cvprint.dto;

public class ApiResult<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 0;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> fail(String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
