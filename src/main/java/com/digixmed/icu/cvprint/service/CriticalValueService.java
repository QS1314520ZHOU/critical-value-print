package com.digixmed.icu.cvprint.service;

import com.digixmed.icu.cvprint.dto.CriticalValueRow;
import com.digixmed.icu.cvprint.dto.SaveCellRequest;
import com.digixmed.icu.cvprint.entity.CriticalValue;
import com.digixmed.icu.cvprint.entity.CriticalValueReport;
import com.digixmed.icu.cvprint.entity.NurseRecords;
import com.digixmed.icu.cvprint.entity.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 危急值登记表业务处理。
 *
 * 数据链路：
 *   criticalValue.pid  ->  patient.hisPid  ->  patient._id / patient.mrn
 *   patient._id        ->  nurseRecords.pid（desc 模糊匹配危急值）-> nurseRecords.username
 *
 * 字段跟随规则：
 *   未被人工编辑过的字段，每次查询都按最新源数据重新计算（源数据变，表格跟着变）；
 *   只有写入 editedFields 的字段才用 criticalValueReport 中的人工值覆盖。
 */
@Service
public class CriticalValueService {

    private static final Logger log = LoggerFactory.getLogger(CriticalValueService.class);

    /** 允许前端编辑的字段白名单 */
    private static final Set<String> EDITABLE_FIELDS = new HashSet<>(Arrays.asList(
            "checkDate", "patientName", "deptText", "bedText", "inpatientNo",
            "lisItem", "value", "repeatResult", "reporter",
            "callTime", "callName", "reportDoctor", "handled"));

    private static final Pattern NUM_PATTERN = Pattern.compile("\\d+\\.\\d+|\\d{2,}");
    private static final long HOUR = 3600_000L;

    private final MongoTemplate mongoTemplate;

    @Value("${critical-value.source-collection:criticalValue}")
    private String sourceCollection;

    @Value("${critical-value.report-collection:criticalValueReport}")
    private String reportCollection;

    @Value("${critical-value.patient-collection:patient}")
    private String patientCollection;

    @Value("${critical-value.nurse-records-collection:nurseRecords}")
    private String nurseRecordsCollection;

    /** 报告人默认值 */
    @Value("${critical-value.default-reporter:\u91cd\u75c7\u7cfb\u7edf}")
    private String defaultReporter;

    /** 匹配护理记录时，允许的时间偏差（小时） */
    @Value("${critical-value.nurse-match-hours:72}")
    private int nurseMatchHours;

    /** 是否只展示已处理（status=true）的危急值 */
    @Value("${critical-value.only-handled:true}")
    private boolean onlyHandled;

    /** 科室编码 -> 科室名称映射，格式：4042:CCU,4041:ICU（不配则自动从 patient 表反查） */
    @Value("${critical-value.dept-code-map:}")
    private String deptCodeMapConfig;

    /** 科室编码换算结果缓存 */
    private final Map<String, String> deptCodeCache = new java.util.concurrent.ConcurrentHashMap<>();

    public CriticalValueService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // ==================================================================
    // 查询
    // ==================================================================

    /**
     * @param pid       criticalValue.pid（= patient.hisPid，postMessage 的 patient.hisPid）
     * @param bed       床号
     * @param deptName  科室
     * @param startDate yyyy-MM-dd
     * @param endDate   yyyy-MM-dd
     * @param byPatient true=仅当前患者
     */
    public List<CriticalValueRow> query(String pid, String bed, String deptName, String deptCode,
                                        String startDate, String endDate, boolean byPatient) {

        // 既没患者也没科室时直接返回空，避免把全院数据查出来
        if (!StringUtils.hasText(pid) && !StringUtils.hasText(deptName) && !StringUtils.hasText(deptCode)) {
            return new ArrayList<>();
        }

        List<Criteria> and = new ArrayList<>();
        // 只显示已处理的危急值（status = true），未处理的不进登记表
        if (onlyHandled) {
            and.add(Criteria.where("status").is(true));
        }
        // 科室：优先用 postMessage 传来的科室编码换算出科室名，并作为硬条件，避免串科室
        String dept = resolveDeptName(deptCode, deptName);
        if (StringUtils.hasText(dept)) {
            and.add(Criteria.where("deptName").is(dept));
        }
        if (byPatient && StringUtils.hasText(pid)) {
            and.add(Criteria.where("pid").is(pid.trim()));
        } else if (StringUtils.hasText(bed)) {
            and.add(Criteria.where("bed").is(bed.trim()));
        }

        Date start = parseDayStart(startDate);
        Date end = parseDayEnd(endDate);
        if (start != null && end != null) {
            and.add(Criteria.where("publishTime").gte(start).lte(end));
        } else if (start != null) {
            and.add(Criteria.where("publishTime").gte(start));
        } else if (end != null) {
            and.add(Criteria.where("publishTime").lte(end));
        }

        Query query = new Query();
        if (!and.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(and.toArray(new Criteria[0])));
        }
        query.with(Sort.by(Sort.Direction.ASC, "publishTime"));
        query.limit(500);

        List<CriticalValue> sources = mongoTemplate.find(query, CriticalValue.class, sourceCollection);
        if (sources.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. criticalValue.pid -> patient
        Set<String> hisPids = sources.stream()
                .map(CriticalValue::getPid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, Patient> patientByHisPid = loadPatients(hisPids);

        // 2. patient._id -> nurseRecords
        Set<String> patientIds = patientByHisPid.values().stream()
                .map(Patient::getId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, List<NurseRecords>> recordsByPatientId =
                loadNurseRecords(patientIds, start, end);

        // 3. 已人工编辑的内容
        List<String> ids = sources.stream().map(CriticalValue::getId).collect(Collectors.toList());
        List<CriticalValueReport> reports = mongoTemplate.find(
                new Query(Criteria.where("sourceId").in(ids)), CriticalValueReport.class, reportCollection);
        Map<String, CriticalValueReport> reportMap = new HashMap<>();
        for (CriticalValueReport r : reports) {
            reportMap.put(r.getSourceId(), r);
        }

        List<CriticalValueRow> rows = new ArrayList<>(sources.size());
        for (CriticalValue cv : sources) {
            Patient p = patientByHisPid.get(nvl(cv.getPid()));
            List<NurseRecords> records = p == null
                    ? Collections.emptyList()
                    : recordsByPatientId.getOrDefault(p.getId(), Collections.emptyList());
            rows.add(merge(cv, p, records, reportMap.get(cv.getId())));
        }
        return rows;
    }

    /**
     * 科室编码 -> 科室名称。
     * 1. 先查配置 critical-value.dept-code-map；
     * 2. 再从 patient 表用 deptCode / deptCode2 反查 dept；
     * 3. 都拿不到则回退到前端传来的科室名。
     */
    private String resolveDeptName(String deptCode, String deptName) {
        String fallback = StringUtils.hasText(deptName) ? deptName.trim() : null;
        if (!StringUtils.hasText(deptCode)) {
            return fallback;
        }
        String code = deptCode.trim();
        String cached = deptCodeCache.get(code);
        if (cached != null) {
            return cached;
        }

        // 1. 配置映射
        if (StringUtils.hasText(deptCodeMapConfig)) {
            for (String pair : deptCodeMapConfig.split(",")) {
                String[] kv = pair.split(":");
                if (kv.length == 2 && kv[0].trim().equals(code) && StringUtils.hasText(kv[1])) {
                    String name = kv[1].trim();
                    deptCodeCache.put(code, name);
                    return name;
                }
            }
        }

        // 2. patient 表反查
        try {
            Query q = new Query(new Criteria().orOperator(
                    Criteria.where("deptCode").is(code),
                    Criteria.where("deptCode2").is(code)));
            q.with(Sort.by(Sort.Direction.DESC, "createdTime"));
            q.limit(1);
            Patient p = mongoTemplate.findOne(q, Patient.class, patientCollection);
            if (p != null && StringUtils.hasText(p.getDept())) {
                deptCodeCache.put(code, p.getDept());
                return p.getDept();
            }
        } catch (Exception e) {
            log.warn("\u79d1\u5ba4\u7f16\u7801\u53cd\u67e5\u5931\u8d25 deptCode={}", code, e);
        }

        return fallback;
    }

    private Map<String, Patient> loadPatients(Set<String> hisPids) {
        Map<String, Patient> map = new HashMap<>();
        if (hisPids.isEmpty()) {
            return map;
        }
        List<Patient> patients = mongoTemplate.find(
                new Query(Criteria.where("hisPid").in(hisPids)), Patient.class, patientCollection);
        for (Patient p : patients) {
            if (!StringUtils.hasText(p.getHisPid())) {
                continue;
            }
            // 同一 hisPid 可能多次住院，保留 mrn 非空且最后一条
            Patient exist = map.get(p.getHisPid());
            if (exist == null || !StringUtils.hasText(exist.getMrn())) {
                map.put(p.getHisPid(), p);
            }
        }
        return map;
    }

    private Map<String, List<NurseRecords>> loadNurseRecords(Set<String> patientIds, Date start, Date end) {
        Map<String, List<NurseRecords>> map = new HashMap<>();
        if (patientIds.isEmpty()) {
            return map;
        }
        List<Criteria> and = new ArrayList<>();
        and.add(Criteria.where("pid").in(patientIds));
        // 只看含“危急值”的护理记录，减少无效数据
        and.add(Criteria.where("desc").regex("\u5371\u6025\u503c"));
        if (start != null) {
            and.add(Criteria.where("time").gte(new Date(start.getTime() - nurseMatchHours * HOUR)));
        }
        if (end != null) {
            and.add(Criteria.where("time").lte(new Date(end.getTime() + nurseMatchHours * HOUR)));
        }
        Query q = new Query(new Criteria().andOperator(and.toArray(new Criteria[0])));
        q.with(Sort.by(Sort.Direction.ASC, "time"));
        q.limit(2000);

        List<NurseRecords> list = mongoTemplate.find(q, NurseRecords.class, nurseRecordsCollection);
        for (NurseRecords r : list) {
            if (Boolean.FALSE.equals(r.getValid())) {
                continue;
            }
            map.computeIfAbsent(nvl(r.getPid()), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    // ==================================================================
    // 系统值计算 + 人工值覆盖
    // ==================================================================

    /** 根��最新源数据计算一行的系统值 */
    private Map<String, String> systemValues(CriticalValue cv, Patient p, List<NurseRecords> records) {
        Map<String, String> m = new LinkedHashMap<>();
        String result = StringUtils.hasText(cv.getValue()) ? cv.getValue() : nvl(cv.getComment());

        m.put("checkDate", fmtDate(cv.getPublishTime()));
        // 姓名：patient.name
        m.put("patientName", p != null ? nvl(p.getName()) : "");
        m.put("deptText", nvl(cv.getDeptName()));
        m.put("bedText", nvl(cv.getBed()));
        // 住院号：criticalValue.pid 是 hisPid，真正的住院号取 patient.mrn
        m.put("inpatientNo", p != null && StringUtils.hasText(p.getMrn()) ? p.getMrn() : "");
        m.put("lisItem", StringUtils.hasText(cv.getLisItem()) ? cv.getLisItem() : nvl(cv.getBigItemName()));
        m.put("value", result);
        // 复述结果默认与危急值结果一致
        m.put("repeatResult", result);
        // 报告人默认“重症系统”
        m.put("reporter", defaultReporter);
        // 接电话时间 = handleTime
        m.put("callTime", fmtTime(cv.getHandleTime()));
        // 接电话姓名 = 匹配到的护理记录 username
        m.put("callName", matchNurseName(records, cv));
        m.put("reportDoctor", nvl(cv.getDoctor()));
        m.put("handled", Boolean.TRUE.equals(cv.getStatus()) ? "\u221a" : "\u00d7");
        return m;
    }

    private CriticalValueRow merge(CriticalValue cv, Patient p,
                                   List<NurseRecords> records, CriticalValueReport r) {
        Map<String, String> values = systemValues(cv, p, records);

        // 只有人工编辑过的字段才覆盖，其余字段始终跟随源数据
        if (r != null && r.getEditedFields() != null) {
            Map<String, String> edited = reportValues(r);
            for (String f : r.getEditedFields()) {
                if (values.containsKey(f) && edited.get(f) != null) {
                    values.put(f, edited.get(f));
                }
            }
        }

        CriticalValueRow row = new CriticalValueRow();
        row.setSourceId(cv.getId());
        row.setCheckDate(values.get("checkDate"));
        row.setPatientName(values.get("patientName"));
        row.setDeptText(values.get("deptText"));
        row.setBedText(values.get("bedText"));
        row.setInpatientNo(values.get("inpatientNo"));
        row.setLisItem(values.get("lisItem"));
        row.setValue(values.get("value"));
        row.setRepeatResult(values.get("repeatResult"));
        row.setReporter(values.get("reporter"));
        row.setCallTime(values.get("callTime"));
        row.setCallName(values.get("callName"));
        row.setReportDoctor(values.get("reportDoctor"));
        row.setHandled(values.get("handled"));
        row.setEdited(r != null && r.getEditedFields() != null && !r.getEditedFields().isEmpty());
        return row;
    }

    private Map<String, String> reportValues(CriticalValueReport r) {
        Map<String, String> m = new HashMap<>();
        m.put("checkDate", r.getCheckDate());
        m.put("patientName", r.getPatientName());
        m.put("deptText", r.getDeptText());
        m.put("bedText", r.getBedText());
        m.put("inpatientNo", r.getInpatientNo());
        m.put("lisItem", r.getLisItem());
        m.put("value", r.getValue());
        m.put("repeatResult", r.getRepeatResult());
        m.put("reporter", r.getReporter());
        m.put("callTime", r.getCallTime());
        m.put("callName", r.getCallName());
        m.put("reportDoctor", r.getReportDoctor());
        m.put("handled", r.getHandled());
        return m;
    }

    // ==================================================================
    // 接电话姓名：护理记录模糊匹配
    // ==================================================================

    /**
     * 在该患者的护理记录中，根据 desc 模糊匹配本条危急值，命中后取 username。
     *
     * 评分规则：
     *   desc 包含危急值数值（如 2.161）      +4
     *   desc 包含项目名（如 肌钙蛋白I、乳酸） +3
     *   时间差 24 小时内 +2，72 小时内 +1
     * 得分 >= 3 才视为命中，同分取时间最接近的一条。
     */
    private String matchNurseName(List<NurseRecords> records, CriticalValue cv) {
        if (records == null || records.isEmpty()) {
            return "";
        }
        List<String> itemNames = extractItemNames(cv);
        List<String> numbers = extractNumbers(cv);
        Date ref = cv.getHandleTime() != null ? cv.getHandleTime() : cv.getPublishTime();

        NurseRecords best = null;
        int bestScore = 0;
        long bestDiff = Long.MAX_VALUE;

        for (NurseRecords r : records) {
            String desc = r.getDesc();
            if (!StringUtils.hasText(desc)) {
                continue;
            }
            int score = 0;
            for (String n : numbers) {
                if (desc.contains(n)) {
                    score += 4;
                    break;
                }
            }
            for (String name : itemNames) {
                if (desc.contains(name)) {
                    score += 3;
                    break;
                }
            }
            long diff = Long.MAX_VALUE;
            if (ref != null && r.getTime() != null) {
                diff = Math.abs(r.getTime().getTime() - ref.getTime());
                if (diff <= 24 * HOUR) {
                    score += 2;
                } else if (diff <= nurseMatchHours * HOUR) {
                    score += 1;
                } else {
                    continue;   // 时间相差太远，不参与匹配
                }
            }
            if (score > bestScore || (score == bestScore && diff < bestDiff)) {
                if (score >= 3) {
                    best = r;
                    bestScore = score;
                    bestDiff = diff;
                }
            }
        }
        return best == null ? "" : nvl(best.getUsername());
    }

    /** 从 lisItem / value 中拆出项目名关键词 */
    private static List<String> extractItemNames(CriticalValue cv) {
        Set<String> set = new HashSet<>();
        addItemName(set, cv.getLisItem());
        String text = StringUtils.hasText(cv.getValue()) ? cv.getValue() : cv.getComment();
        if (StringUtils.hasText(text)) {
            for (String seg : text.split("[,\uff0c;\uff1b]")) {
                seg = seg.trim();
                if (seg.isEmpty()) {
                    continue;
                }
                // 取第一个空格或数字之前的部分作为项目名
                Matcher m = Pattern.compile("^([^\\s\\d]+)").matcher(seg);
                if (m.find()) {
                    addItemName(set, m.group(1));
                }
            }
        }
        return new ArrayList<>(set);
    }

    private static void addItemName(Set<String> set, String raw) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String s = raw.replace("*", "").trim();
        if (s.length() >= 2) {
            set.add(s);
        }
        // 去掉括号内容，如 *纤维蛋白原(FIB) -> 纤维蛋白原
        String noBracket = s.replaceAll("[\\(\uff08][^\\)\uff09]*[\\)\uff09]", "").trim();
        if (noBracket.length() >= 2) {
            set.add(noBracket);
        }
        // 括号内的缩写也作为关键词，如 APTT、FIB
        Matcher m = Pattern.compile("[\\(\uff08]([^\\)\uff09]+)[\\)\uff09]").matcher(s);
        while (m.find()) {
            String in = m.group(1).trim();
            if (in.length() >= 2) {
                set.add(in);
            }
        }
    }

    /** 从 value 中拆出数值，如 2365.02、19.65 */
    private static List<String> extractNumbers(CriticalValue cv) {
        List<String> list = new ArrayList<>();
        String text = StringUtils.hasText(cv.getValue()) ? cv.getValue() : cv.getComment();
        if (!StringUtils.hasText(text)) {
            return list;
        }
        Matcher m = NUM_PATTERN.matcher(text);
        while (m.find()) {
            String n = m.group();
            if (n.length() >= 3 && !list.contains(n)) {
                list.add(n);
            }
        }
        return list;
    }

    // ==================================================================
    // 保存
    // ==================================================================

    /**
     * 单元格编辑自动保存（upsert）。
     * 同时把字段名记入 editedFields，表示该字段已被人工接管，不再跟随源数据。
     */
    public void saveCell(SaveCellRequest req) {
        if (!StringUtils.hasText(req.getSourceId())) {
            throw new IllegalArgumentException("sourceId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!EDITABLE_FIELDS.contains(req.getField())) {
            throw new IllegalArgumentException("\u4e0d\u652f\u6301\u7f16\u8f91\u7684\u5b57\u6bb5\uff1a" + req.getField());
        }

        CriticalValue cv = mongoTemplate.findById(req.getSourceId(), CriticalValue.class, sourceCollection);

        Update update = new Update()
                .set(req.getField(), req.getValue() == null ? "" : req.getValue())
                .addToSet("editedFields", req.getField())
                .set("updatedBy", nvl(req.getAccount()))
                .set("updateTime", new Date())
                .setOnInsert("sourceId", req.getSourceId())
                .setOnInsert("createTime", new Date());

        if (cv != null) {
            update.set("pid", nvl(cv.getPid()))
                    .set("bed", nvl(cv.getBed()))
                    .set("deptName", nvl(cv.getDeptName()))
                    .set("publishTime", cv.getPublishTime());
        }

        mongoTemplate.upsert(new Query(Criteria.where("sourceId").is(req.getSourceId())),
                update, CriticalValueReport.class, reportCollection);
        log.debug("saved cell {}.{}", req.getSourceId(), req.getField());
    }

    /** 撤销某个字段的人工修改，恢复为系统值并重新跟随源数据 */
    public void resetCell(String sourceId, String field) {
        if (!StringUtils.hasText(sourceId) || !EDITABLE_FIELDS.contains(field)) {
            throw new IllegalArgumentException("\u53c2\u6570\u4e0d\u5408\u6cd5");
        }
        Update update = new Update()
                .pull("editedFields", field)
                .unset(field)
                .set("updateTime", new Date());
        mongoTemplate.updateFirst(new Query(Criteria.where("sourceId").is(sourceId)),
                update, CriticalValueReport.class, reportCollection);
    }

    /** 整行保存 */
    public void saveRow(CriticalValueRow row, String account) {
        if (row == null || !StringUtils.hasText(row.getSourceId())) {
            throw new IllegalArgumentException("sourceId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        CriticalValue cv = mongoTemplate.findById(row.getSourceId(), CriticalValue.class, sourceCollection);

        Update update = new Update()
                .set("checkDate", nvl(row.getCheckDate()))
                .set("patientName", nvl(row.getPatientName()))
                .set("deptText", nvl(row.getDeptText()))
                .set("bedText", nvl(row.getBedText()))
                .set("inpatientNo", nvl(row.getInpatientNo()))
                .set("lisItem", nvl(row.getLisItem()))
                .set("value", nvl(row.getValue()))
                .set("repeatResult", nvl(row.getRepeatResult()))
                .set("reporter", nvl(row.getReporter()))
                .set("callTime", nvl(row.getCallTime()))
                .set("callName", nvl(row.getCallName()))
                .set("reportDoctor", nvl(row.getReportDoctor()))
                .set("handled", nvl(row.getHandled()))
                .set("editedFields", new ArrayList<>(EDITABLE_FIELDS))
                .set("updatedBy", nvl(account))
                .set("updateTime", new Date())
                .setOnInsert("sourceId", row.getSourceId())
                .setOnInsert("createTime", new Date());

        if (cv != null) {
            update.set("pid", nvl(cv.getPid()))
                    .set("bed", nvl(cv.getBed()))
                    .set("deptName", nvl(cv.getDeptName()))
                    .set("publishTime", cv.getPublishTime());
        }

        mongoTemplate.upsert(new Query(Criteria.where("sourceId").is(row.getSourceId())),
                update, CriticalValueReport.class, reportCollection);
    }

    // ==================================================================
    // 工具方法
    // ==================================================================

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String fmtDate(Date d) {
        return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    private static String fmtTime(Date d) {
        return d == null ? "" : new SimpleDateFormat("MM-dd HH:mm").format(d);
    }

    private static Date parseDayStart(String day) {
        if (!StringUtils.hasText(day)) return null;
        LocalDate ld = LocalDate.parse(day.trim());
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date parseDayEnd(String day) {
        if (!StringUtils.hasText(day)) return null;
        LocalDate ld = LocalDate.parse(day.trim());
        return Date.from(ld.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1_000_000).toInstant());
    }
}
