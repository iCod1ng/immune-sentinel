-- =========================================================
-- V1 基础表结构
-- 设计原则：
--   1. 所有业务表带 tenant_id、patient_id，MVP 阶段值恒为 1
--   2. 时间字段统一 DATETIME，时区由应用层设置 Asia/Shanghai
--   3. 预留 deleted 做逻辑删除；created_at/updated_at 做审计
-- =========================================================

-- 租户（MVP: 1）
CREATE TABLE tenant (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    code          VARCHAR(64)  NOT NULL UNIQUE,
    name          VARCHAR(128) NOT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

-- 患者
CREATE TABLE patient (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    name          VARCHAR(64)  NOT NULL,
    gender        VARCHAR(8),
    birth_date    DATE,
    tags          VARCHAR(512) COMMENT 'JSON 数组：PD-1,DM,post-surgery,wheelchair...',
    therapy_start DATE         COMMENT '治疗起始日期',
    notes         TEXT,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant (tenant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者';

-- 照护人（家属 / 患者本人）
CREATE TABLE caregiver (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    patient_id    BIGINT       NOT NULL,
    name          VARCHAR(64)  NOT NULL,
    relation      VARCHAR(32)  COMMENT 'self/spouse/child/sibling/other',
    phone         VARCHAR(32),
    is_primary    TINYINT      NOT NULL DEFAULT 0,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_patient (patient_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照护人';

-- 推送通道
CREATE TABLE notify_channel (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    patient_id    BIGINT       NOT NULL,
    channel_type  VARCHAR(32)  NOT NULL COMMENT 'feishu_bot/wecom_bot/email',
    display_name  VARCHAR(128) NOT NULL,
    webhook_url   VARCHAR(512),
    secret        VARCHAR(256),
    extra_json    TEXT,
    enabled       TINYINT      NOT NULL DEFAULT 1,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_patient (patient_id, deleted, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送通道配置';

-- 照护人 × 通道
CREATE TABLE caregiver_channel (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    caregiver_id  BIGINT       NOT NULL,
    channel_id    BIGINT       NOT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cg_ch (caregiver_id, channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照护人通道关联';

-- checklist 模板
CREATE TABLE checklist_template (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id          BIGINT       NOT NULL,
    code               VARCHAR(64)  NOT NULL COMMENT 'daily_morning/daily_evening/weekly_leg/...',
    name               VARCHAR(128) NOT NULL,
    trigger_type       VARCHAR(32)  NOT NULL COMMENT 'daily/weekly/monthly/quarterly/relative_event',
    trigger_cron       VARCHAR(64)  COMMENT 'cron 表达式或相对事件描述',
    default_due_time   VARCHAR(8)   COMMENT 'HH:mm，实例默认截止时段',
    applicable_tags    VARCHAR(256) COMMENT 'JSON 数组，命中任意标签即适用',
    reminder_cron      VARCHAR(64)  COMMENT '提醒推送的 cron（独立于生成）',
    description        TEXT,
    enabled            TINYINT      NOT NULL DEFAULT 1,
    deleted            TINYINT      NOT NULL DEFAULT 0,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='checklist 模板';

-- checklist 模板条目
CREATE TABLE checklist_item (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id   BIGINT       NOT NULL,
    code          VARCHAR(64)  NOT NULL COMMENT 'body_temp/bp/glucose/fast_test/leg_circ...',
    label         VARCHAR(256) NOT NULL,
    section       VARCHAR(64)  COMMENT '早晨/白天/上午/晚上/应急...',
    input_type    VARCHAR(32)  NOT NULL COMMENT 'checkbox/number/bp/text/photo/composite',
    unit          VARCHAR(32),
    normal_range  VARCHAR(128) COMMENT '显示给用户看的正常范围',
    red_flag_rule VARCHAR(64)  COMMENT '风险预警规则 code（可选）',
    required      TINYINT      NOT NULL DEFAULT 0,
    order_no      INT          NOT NULL DEFAULT 0,
    hint          VARCHAR(512),
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_template (template_id, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='checklist 条目';

-- checklist 实例（按日期生成，幂等）
CREATE TABLE checklist_instance (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL,
    patient_id     BIGINT       NOT NULL,
    template_id    BIGINT       NOT NULL,
    template_code  VARCHAR(64)  NOT NULL,
    due_date       DATE         NOT NULL COMMENT '应完成日期',
    due_at         DATETIME     NOT NULL COMMENT '应完成时点',
    status         VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/partial/done/missed',
    completed_at   DATETIME,
    abnormal_count INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_patient_tpl_date (patient_id, template_code, due_date),
    KEY idx_status (patient_id, status, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='checklist 实例';

-- checklist 条目填写记录
CREATE TABLE checklist_item_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    patient_id    BIGINT       NOT NULL,
    instance_id   BIGINT       NOT NULL,
    item_id       BIGINT       NOT NULL,
    item_code     VARCHAR(64)  NOT NULL,
    value_json    TEXT         COMMENT '结构化值：{"num":36.7} / {"sys":130,"dia":80} / {"checked":true} / {"photos":["url1"]}',
    is_abnormal   TINYINT      NOT NULL DEFAULT 0,
    note          VARCHAR(512),
    recorded_at   DATETIME,
    recorded_by   VARCHAR(64)  COMMENT 'caregiver name or self',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_instance_item (instance_id, item_id),
    KEY idx_patient_item (patient_id, item_code, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='checklist 条目记录';

-- 体征时序表（从 item_record 冗余出来，查询快）
CREATE TABLE vital_sign_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    patient_id    BIGINT       NOT NULL,
    metric_code   VARCHAR(32)  NOT NULL COMMENT 'temp/bp_sys/bp_dia/hr/glucose_fasting/glucose_pp/spo2/weight',
    value_num     DECIMAL(10,2),
    value_text    VARCHAR(128),
    unit          VARCHAR(16),
    measured_at   DATETIME     NOT NULL,
    source        VARCHAR(32)  COMMENT 'checklist/manual',
    source_ref    BIGINT       COMMENT '来源记录 id',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_patient_metric (patient_id, metric_code, measured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体征时序';

-- 左腿围度专表（太重要，独立）
CREATE TABLE leg_measurement (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id         BIGINT       NOT NULL,
    patient_id        BIGINT       NOT NULL,
    site              VARCHAR(32)  NOT NULL COMMENT 'thigh_mid/below_knee/calf_max/above_ankle',
    circumference_cm  DECIMAL(5,1) NOT NULL,
    photo_urls        TEXT         COMMENT 'JSON 数组',
    delta_vs_last_week DECIMAL(5,1) COMMENT '与上周同部位差值，应用层算好',
    delta_vs_3days    DECIMAL(5,1) COMMENT '与 3 天前同部位差值',
    measured_at       DATETIME     NOT NULL,
    note              VARCHAR(512),
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_patient_site (patient_id, site, measured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='左腿围度测量';

-- 症状日记（晚间）
CREATE TABLE symptom_diary (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    patient_id    BIGINT       NOT NULL,
    diary_date    DATE         NOT NULL,
    structured    TEXT         COMMENT 'JSON 勾选结果',
    free_text     TEXT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_patient_date (patient_id, diary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='症状日记';

-- 风险预警事件
CREATE TABLE red_flag_event (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL,
    patient_id     BIGINT       NOT NULL,
    rule_code      VARCHAR(64)  NOT NULL,
    severity       VARCHAR(16)  NOT NULL COMMENT 'emergency/24h/7d/info',
    title          VARCHAR(256) NOT NULL,
    detail         TEXT,
    triggered_by   VARCHAR(64)  COMMENT 'item_record:<id> / manual / scheduled',
    source_ref     BIGINT,
    triggered_at   DATETIME     NOT NULL,
    acknowledged_at DATETIME,
    acknowledged_by VARCHAR(64),
    resolved_at    DATETIME,
    resolution_note TEXT,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_patient_time (patient_id, triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险预警事件';

-- 关键节点事件（输液、复诊、检查）
CREATE TABLE medical_event (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL,
    patient_id     BIGINT       NOT NULL,
    event_type     VARCHAR(32)  NOT NULL COMMENT 'infusion/checkup/lab_test/imaging',
    title          VARCHAR(256) NOT NULL,
    scheduled_at   DATETIME     NOT NULL,
    executed_at    DATETIME,
    status         VARCHAR(16)  NOT NULL DEFAULT 'planned' COMMENT 'planned/done/cancelled',
    note           TEXT,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_patient_time (patient_id, scheduled_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关键节点事件';

-- 推送历史
CREATE TABLE notify_log (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL,
    patient_id     BIGINT       NOT NULL,
    channel_id     BIGINT,
    channel_type   VARCHAR(32),
    category       VARCHAR(32)  COMMENT 'reminder/red_flag/summary/heartbeat',
    title          VARCHAR(256),
    content        TEXT,
    success        TINYINT      NOT NULL DEFAULT 0,
    error_msg      TEXT,
    sent_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_patient_time (patient_id, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送日志';

-- ShedLock 锁表
CREATE TABLE shedlock (
    name          VARCHAR(64)  PRIMARY KEY,
    lock_until    TIMESTAMP(3) NOT NULL,
    locked_at     TIMESTAMP(3) NOT NULL,
    locked_by     VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度分布式锁';
