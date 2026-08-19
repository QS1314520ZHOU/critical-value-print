package com.digixmed.icu.cvprint.controller;

import com.digixmed.icu.cvprint.dto.ApiResult;
import com.digixmed.icu.cvprint.dto.CriticalValueRow;
import com.digixmed.icu.cvprint.dto.SaveCellRequest;
import com.digixmed.icu.cvprint.service.CriticalValueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 危急值报告记录登记 接口
 *
 * GET  /api/critical-value/list   查询登记表数据
 * POST /api/critical-value/cell   单元格编辑自动保存
 * POST /api/critical-value/row    整行保存
 */
@RestController
@RequestMapping("/api/critical-value")
public class CriticalValueController {

    private final CriticalValueService service;

    public CriticalValueController(CriticalValueService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public ApiResult<List<CriticalValueRow>> list(
            @RequestParam(required = false) String pid,
            @RequestParam(required = false) String bed,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "false") boolean byPatient) {
        try {
            return ApiResult.ok(service.query(pid, bed, deptName, deptCode, startDate, endDate, byPatient));
        } catch (Exception e) {
            return ApiResult.fail("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/cell")
    public ApiResult<String> saveCell(@RequestBody SaveCellRequest req) {
        try {
            service.saveCell(req);
            return ApiResult.ok("saved");
        } catch (Exception e) {
            return ApiResult.fail("保存失败：" + e.getMessage());
        }
    }

    /** 撤销某单元格的人工修改，恢复为系统值 */
    @PostMapping("/cell/reset")
    public ApiResult<String> resetCell(@RequestBody SaveCellRequest req) {
        try {
            service.resetCell(req.getSourceId(), req.getField());
            return ApiResult.ok("reset");
        } catch (Exception e) {
            return ApiResult.fail("重置失败：" + e.getMessage());
        }
    }

    @PostMapping("/row")
    public ApiResult<String> saveRow(@RequestBody Map<String, Object> body) {
        try {
            CriticalValueRow row = new CriticalValueRow();
            row.setSourceId((String) body.get("sourceId"));
            row.setCheckDate((String) body.get("checkDate"));
            row.setPatientName((String) body.get("patientName"));
            row.setDeptText((String) body.get("deptText"));
            row.setBedText((String) body.get("bedText"));
            row.setInpatientNo((String) body.get("inpatientNo"));
            row.setLisItem((String) body.get("lisItem"));
            row.setValue((String) body.get("value"));
            row.setRepeatResult((String) body.get("repeatResult"));
            row.setReporter((String) body.get("reporter"));
            row.setCallTime((String) body.get("callTime"));
            row.setCallName((String) body.get("callName"));
            row.setReportDoctor((String) body.get("reportDoctor"));
            row.setHandled((String) body.get("handled"));
            service.saveRow(row, (String) body.get("account"));
            return ApiResult.ok("saved");
        } catch (Exception e) {
            return ApiResult.fail("保存失败：" + e.getMessage());
        }
    }
}
