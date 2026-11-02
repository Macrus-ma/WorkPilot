# WorkPilot

企业客户服务工单与知识库协同平台。目前完成阶段 0 和阶段 1：客户注册、登录、退出、查看分类、创建工单及查看自己的工单。工单流转、知识库、Redis、Spring Cloud、RabbitMQ 和 AI 尚未实现。

## 代码分层

`com.workpilot.controller` 接收 HTTP 请求，`dto` 存放以 `DTO` 结尾的请求数据，`vo` 存放以 `VO` 结尾的响应数据，`entity` 映射数据库表，`mapper` 负责数据库访问。`service` 只声明业务接口，主要业务逻辑放在 `service.impl`。认证主体和过滤器放在 `security`，不作为数据库实体或接口响应直接暴露。

工单创建逻辑集中在 `TicketServiceImpl`：`TransactionTemplate` 确保请求记录、工单和操作日志一起提交或回滚；重复键异常在事务结束后处理，再查询并返回原工单。

## 本阶段业务规则

- 注册接口只创建 `CUSTOMER`。不能通过请求指定员工或管理员身份；员工账号和权限管理留到工单流转阶段。
- 创建工单时，分类决定负责部门。新工单状态为 `NEW`；P1、P2、P3 的演示截止时间分别为创建后的 4、24、72 个自然小时。数据库时间按 UTC 保存，接口返回的日期时间也按 UTC 解读。
- 客户查询工单时，SQL 同时检查工单 ID 和客户 ID。无权查看时返回 404。
- 每次创建必须发送 `Idempotency-Key`。同一客户用相同键和相同内容重试，会返回原工单；相同键但内容不同返回 409。`operation_request` 的数据库唯一约束是正确性兜底。当前阶段没有 Redis。
- 登录返回随机 Bearer 令牌。数据库仅保存令牌的 SHA-256 摘要；退出后令牌立即失效。令牌默认 12 小时有效。公开网络部署前必须启用 HTTPS，并增加注册与登录限流。

## 运行环境

已在 Java 17、Maven 3.9.15 上编译。项目固定 Spring Boot 3.5.16、MyBatis-Plus 3.5.18.1，不包含 Spring Cloud 依赖。

1. 安装并启动 MySQL 8，使用有建库权限的账号登录 MySQL 命令行，执行：

   ```sql
   SOURCE E:/code/WorkPilot/sql/001_initial.sql;
   CREATE USER 'workpilot'@'127.0.0.1' IDENTIFIED BY '自行设置的强密码';
   GRANT SELECT, INSERT, UPDATE, DELETE ON workpilot.* TO 'workpilot'@'127.0.0.1';
   ```

   如果该账号已经存在，只需确认密码和权限。脚本会创建 `workpilot` 数据库、阶段 1 的表，并准备“账号问题”“使用问题”两个分类。SQL 脚本应只在新数据库上执行；后续阶段使用新的增量脚本。

2. 在当前 PowerShell 窗口设置数据库账号，然后启动应用：

   ```powershell
   $env:WORKPILOT_DB_USER = 'workpilot'
   $env:WORKPILOT_DB_PASSWORD = '上一步设置的密码'
   mvn -B -ntp spring-boot:run
   ```

   数据库不是默认端口时，另设 `WORKPILOT_DB_URL`，例如 `jdbc:mysql://127.0.0.1:3307/workpilot?useUnicode=true&characterEncoding=utf8&connectionTimeZone=%2B00:00&forceConnectionTimeZoneToSession=true`。连接会话固定为 UTC，使 MySQL 默认生成的时间与 Java 计算的截止时间一致。密码只放本机环境变量，不提交进仓库。

3. 检查 `http://localhost:8080/actuator/health` 返回 `UP`。如果返回 `DOWN`，先核对数据库地址、账号、密码与脚本是否执行成功。

## HTTP 接口

| 方法与路径 | 用途 |
|---|---|
| `POST /api/auth/register` | 客户注册：`loginName`, `password`, `displayName` |
| `POST /api/auth/login` | 登录，取得 `accessToken` |
| `POST /api/auth/logout` | 退出，需 `Authorization: Bearer <token>` |
| `GET /api/auth/me` | 查看当前账号 |
| `GET /api/tickets/categories` | 查看可用分类 |
| `POST /api/tickets` | 创建工单，需 `Idempotency-Key`；正文含 `categoryId`, `title`, `description`, `priority` |
| `GET /api/tickets/mine?page=1&size=20` | 查看自己的工单，`size` 最大 50 |
| `GET /api/tickets/{id}` | 查看自己的一张工单 |

除注册、登录及健康检查外，接口均需 Bearer 令牌。创建工单示例：

```powershell
$headers = @{ Authorization = "Bearer $token"; 'Idempotency-Key' = [guid]::NewGuid().ToString() }
$body = @{ categoryId = 1; title = '无法登录'; description = '账号提示错误'; priority = 'P2' } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:8080/api/tickets' -Method Post -Headers $headers -ContentType 'application/json' -Body $body
```

## 验证边界

`mvn verify` 的 HTTP 集成测试使用 H2 验证第一条业务链路、重复请求、数据隔离和退出失效。它不能代替真实 MySQL 验收；本机 MySQL 有密码保护，尚需按上述步骤配置应用账号后做实际启动检查。
