# immune-sentinel

PD-1 免疫治疗患者副作用全流程监测系统。

> **定位**：单患者起步，多租户 / 模板市场 / 医生端留扩展口；2C4G 小服务器即可部署。
>
> **技术栈**：Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8 + Thymeleaf + ShedLock + 飞书 / 企微机器人推送。

## 功能概览

- 每日 / 每周 / 每月 / 每 3 个月 / 输液前后 checklist 模板化驱动
- 定时生成当天 checklist 实例 + 定点推送填报链接（飞书 / 企微群机器人）
- H5 移动端填报页（签名 token 链接，无需登录）
- 体征时序与左腿围度持久化，异常自动触发风险预警推送
- 风险预警规则引擎：FAST、血压、血糖、SpO₂、腿围、黄疸、腹泻、皮疹、胸痛、头痛等 10+ 内置规则
- 全部数据带 `tenant_id / patient_id`，便于后续多患者、多租户扩展

## 快速开始（本地开发）

```bash
# 1. 准备 MySQL（端口 3306，root/root，库名 immune_sentinel，utf8mb4）
mysql -uroot -proot -e "CREATE DATABASE immune_sentinel DEFAULT CHARSET utf8mb4;"

# 2. 构建运行
mvn -DskipTests package
SPRING_PROFILES_ACTIVE=dev java -jar target/immune-sentinel.jar
```

启动后：

- 应用地址：http://localhost:8080
- 健康检查：http://localhost:8080/actuator/health

Flyway 会自动建表 + 灌入种子数据（默认租户、一名患者、一套 checklist 模板）。

## 配置飞书机器人（或企微）

### 方式 A：直接改数据库

```sql
UPDATE notify_channel
   SET webhook_url = '<你的飞书 webhook>',
       secret      = '<签名密钥，可选>',
       enabled     = 1
 WHERE id = 1;
```

### 方式 B：Admin API（推荐）

所有管理接口用 `X-Admin-Token` 头鉴权，值为环境变量 `SENTINEL_ADMIN_TOKEN`。

```bash
ADMIN=$SENTINEL_ADMIN_TOKEN
BASE=http://localhost:8080

# 1. 配通道（type 也可改为 wecom_bot）
curl -s -X POST $BASE/admin/channels/1 \
  -H "X-Admin-Token: $ADMIN" -H "Content-Type: application/json" \
  -d '{"channelType":"feishu_bot","webhookUrl":"<飞书 webhook>","secret":"<密钥>","enabled":"1"}'

# 2. 测试推送
curl -s -X POST $BASE/admin/channels/1/test -H "X-Admin-Token: $ADMIN"

# 3. 手动生成当天 checklist（首次上线时用）
curl -s -X POST $BASE/admin/generate -H "X-Admin-Token: $ADMIN"

# 4. 手动触发一次晨间提醒
curl -s -X POST $BASE/admin/reminder/daily_morning -H "X-Admin-Token: $ADMIN"

# 5. 查今天所有 checklist + 填报链接
curl -s $BASE/admin/today -H "X-Admin-Token: $ADMIN"

# 6. 登记下一次输液（触发 infusion_t_minus_1d / infusion_day 模板）
curl -s -X POST $BASE/admin/medical-events \
  -H "X-Admin-Token: $ADMIN" -H "Content-Type: application/json" \
  -d '{"patientId":1,"eventType":"infusion","title":"PD-1 输液","scheduledAt":"2026-05-18T09:00:00"}'
```

## 服务器部署（Docker Compose）

```bash
# 1. 在你的 2C4G 服务器克隆仓库
git clone <repo> /opt/immune-sentinel
cd /opt/immune-sentinel

# 2. 生成环境变量文件
cp .env.example .env
# 编辑 .env，至少修改 DB_PASSWORD、SENTINEL_TOKEN_SECRET、SENTINEL_BASE_URL

# 3. 本地打包（或在服务器用 maven 镜像打包）
mvn -DskipTests package

# 4. 起容器
docker compose up -d --build

# 5. 看日志
docker compose logs -f app
```

目录约定：

- `./data/mysql` MySQL 数据
- `./data/uploads` 照片 / 附件（Nginx `/files/` 对外只读）
- `./logs`  应用日志

## 排查

- 推送没到：查 `notify_log` 表 `success` 与 `error_msg`
- 实例没生成：确认 `daily_generate` 调度触发 + `shedlock` 表里没有陈旧锁
- 链接失效：token 默认 48h 过期，可修改 `sentinel.token-ttl-hours`

## 路线图

- **阶段 1（MVP）**：完成。
- **阶段 2**：Vue3/Vant 或小程序前端；七牛云 / MinIO 图片上传；复诊问题 PDF 自动生成。
- **阶段 3**：真·多租户、模板市场、医生端。
