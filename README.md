# 危急值报告记录登记（A4 横向打印）

后端：Java 8 + Spring Boot 2.7 + Spring Data MongoDB
前端：单页 `index.html`（内置 postMessage 握手、单元格编辑自动保存、A4 横向打印）

## 一、目录结构

```
critical-value-print
├─ pom.xml
└─ src/main
   ├─ java/com/digixmed/icu/cvprint
   │  ├─ CriticalValuePrintApplication.java   启动类
   │  ├─ config/MongoConfig.java              MongoDB 连接（连接池、去 _class）
   │  ├─ config/MongoIndexInitializer.java    启动时自动建集合 + 索引
   │  ├─ config/WebConfig.java                跨域配置
   │  ├─ entity/CriticalValue.java            源集合 criticalValue（只读）
   │  ├─ entity/CriticalValueReport.java      登记表集合 criticalValueReport（写入）
   │  ├─ dto/…                                行数据、单元格保存请求、统一返回
   │  ├─ repository/…                         MongoRepository
   │  ├─ service/CriticalValueService.java    查询合并 + upsert 自动保存
   │  └─ controller/CriticalValueController.java
   └─ resources
      ├─ application.yml                      数据库连接方式在这里配置
      └─ static/index.html                    表格页面
```

## 二、数据库连接方式

`src/main/resources/application.yml`：

```yaml
spring:
  data:
    mongodb:
      # 无认证
      uri: mongodb://192.168.5.154:27017/SmartCare
      # 有认证
      # uri: mongodb://用户名:密码@192.168.5.154:27017/SmartCare?authSource=admin
      # 副本集
      # uri: mongodb://ip1:27017,ip2:27017/SmartCare?replicaSet=rs0&authSource=admin
```

也可用分项配置（host/port/database/username/password/authentication-database），与 uri 二选一。
连接池、超时参数在 `MongoConfig.java` 中：maxSize=50、minSize=5、连接超时 10s、读超时 30s。

启动参数覆盖示例：

```bash
java -jar critical-value-print-1.0.0.jar \
  --spring.data.mongodb.uri="mongodb://user:pwd@10.0.0.9:27017/SmartCare?authSource=admin" \
  --server.port=18088
```

## 三、数据来源与字段映射

涉及三个只读集合：`criticalValue`、`patient`、`nurseRecords`（程序不会修改它们）。

关联链路：

```
criticalValue.pid  ==  patient.hisPid   ->  patient._id、patient.mrn、patient.name
patient._id        ==  nurseRecords.pid ->  desc 模糊匹配危急值 -> nurseRecords.username
```

| 表格列 | 来源 | 说明 |
| --- | --- | --- |
| 检查日期 | `criticalValue.publishTime` | yyyy-MM-dd |
| 姓名 | `patient.name` | 通过 hisPid 关联 |
| 科室 | `criticalValue.deptName` | |
| 床号 | `criticalValue.bed` | |
| **住院号** | **`patient.mrn`** | criticalValue.pid 是 HIS 标识，不是住院号 |
| 危急值项目 | `lisItem`，为空取 `bigItemName` | |
| 危急值结果 | `value`，为空取 `comment` | |
| **复述结果** | **默认与危急值结果一致** | 可人工修改 |
| **报告人** | **默认“重症系统”** | `critical-value.default-reporter` 可配 |
| 接电话时间 | `criticalValue.handleTime` | MM-dd HH:mm |
| **接电话姓名** | **`nurseRecords.username`** | 按下方规则匹配 |
| 报告医生 | `criticalValue.doctor` | |
| 是否处置 | `criticalValue.status` | true → √，false → × |

### 接电话姓名匹配规则

1. 用 `criticalValue.pid` 到 `patient` 表按 `hisPid` 查到 `_id`；
2. 用该 `_id` 到 `nurseRecords` 表按 `pid` 查出该患者含“危急值”的护理记录；
3. 对 `desc` 打分：包含危急值数值（如 `2.161`）+4；包含项目名（如 `肌钙蛋白I`、`APTT`）+3；时间差 24h 内 +2，72h 内 +1；
4. 得分 ≥ 3 视为命中，同分取时间最接近的一条，取其 `username`；未命中则留空供人工填写。

时间窗口由 `critical-value.nurse-match-hours`（默认 72 小时）控制。

### 科室隔离

前端从 postMessage 取 `patient.deptCode`（没有则取 `deptCode2`）一并传给后端，后端按下列顺序换算成科室名，并作为**硬条件**加到查询里（即使已按患者过滤也一样加）：

1. 配置 `critical-value.dept-code-map`（如 `4042:CCU,4041:ICU`）；
2. 从 `patient` 表用 `deptCode` / `deptCode2` 反查 `dept`，结果会缓存；
3. 以上都没命中时，回退使用 `patient.dept` 文本。

另外，勾选“仅当前患者”但没拿到 `pid` 时，前后端都直接返回空结果，不会退化成查全库。

### 只显示已处理的危急值

登记表默认只查 `criticalValue.status = true`（已处理）的记录，未处理的不显示，所以「是否处置」列正常都是 √。
如需连未处理的一起显示，把 `critical-value.only-handled` 改成 `false` 后重启。

### 源数据���化后表格自动跟随

登记表记录中多了一个 `editedFields` 数组：

- **没被人工改过的列**：每次查询都按 `criticalValue` / `patient` / `nurseRecords` 的最新数据重新计算，源数据（如 `status` 从 false 变 true、`handleTime` 回写）一变，表格跟着变；
- **被人工改过的列**：以人工值为准，不再被源数据覆盖；
- 想把某个单元格恢复成系统值，调 `POST /api/critical-value/cell/reset`（`{sourceId, field}`）。

## 四、编辑自动保存

- 表格所有单元格 `contenteditable`，失焦（或回车）即自动保存。
- 保存写入集合 `SmartCare.criticalValueReport`；**该集合不存在时会自动创建**（启动时 `MongoIndexInitializer` 检查创建，或首次 upsert 时由 MongoDB 创建）。
- 以 `sourceId = criticalValue._id` 作为唯一键 upsert，重复编辑覆盖同一条记录。
- 记录 `updatedBy`（postMessage 的 account）、`createTime`、`updateTime`。
- 只允许白名单字段写入，防止任意字段注入。

`criticalValueReport` 文档示例：

```json
{
  "_id": ObjectId("..."),
  "sourceId": "6a6427ff857d0c71d9e30166",
  "pid": "2514595", "bed": "107", "deptName": "CCU",
  "publishTime": ISODate("2026-07-25T03:01:21.000Z"),
  "checkDate": "2026-07-25", "patientName": "卢旭莉",
  "deptText": "CCU", "bedText": "107", "inpatientNo": "2514595",
  "lisItem": "DB/TB", "value": "*肌酸激酶 2365.02U/L (↑↑)",
  "repeatResult": "肌酸激酶 2365.02", "reporter": "李护士",
  "callTime": "07-25 11:11", "callName": "王护士",
  "reportDoctor": "黄星", "handled": "√",
  "updatedBy": "zhangsan", "createTime": ISODate("..."), "updateTime": ISODate("...")
}
```

## 五、接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/critical-value/list?pid=&bed=&deptName=&startDate=&endDate=&byPatient=` | 查询登记表数据（源数据 + 已编辑内容合并） |
| POST | `/api/critical-value/cell` | 单元格自动保存 `{sourceId, field, value, account}` |
| POST | `/api/critical-value/cell/reset` | 撤销人工修改，恢复系统值 `{sourceId, field}` |
| POST | `/api/critical-value/row` | 整行保存 |

## 六、postMessage 握手（按对接文档 V2）

1. 页面加载后向宿主发送 `{ type: 'SmartCareReady' }`。
2. 宿主回传 `{ type: 'SmartCare', account, patient, token }`。
3. 页面取用：
   - `pid = patient.hisPid || patient.patientId`（用于匹配 `criticalValue.pid`）
   - `mrn = patient.mrn`（表格里的住院号）
   - `bed = patient.showBed || patient.hisBed`
   - `dept = patient.dept`
   - `name = patient.name`，性别按 `genderStr` 优先。
4. 收到新的 patient 后自动重新查询，避免打印上一位患者数据。

正式部署请修改 `index.html` 顶部：

```js
var HOST_ORIGIN = 'http://宿主系统地址';   // 不要长期使用 '*'
```

并放开来源校验：`if (e.origin !== 'http://宿主系统地址') return;`

## 七、打印

- `@page { size: A4 landscape; margin: 8mm; }`，点击「打印」调用浏览器打印。
- 打印时自动隐藏工具栏、去掉底色和编辑高亮，表头跨页重复，行不跨页断开。
- 空表也会铺满 11 行，可直接打印出来手写。

## 八、构建运行

```bash
mvn clean package -DskipTests
java -jar target/critical-value-print-1.0.0.jar
# 访问 http://ip:18088/index.html ，或由宿主系统以 iframe 加载该地址
```
