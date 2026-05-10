-- =========================================================
-- V2 种子数据
--   1. 默认租户 + 1 位患者 + 1 位照护人
--   2. 基于 checklist 原文生成的 4 个核心模板（晨间/晚间/每周/每月）
--      + 输液前 24h / 输液当天 2 个相对事件模板
--   3. 模板内置条目对应原 checklist 的每一项
-- =========================================================

-- 1. 基础数据
INSERT INTO tenant (id, code, name) VALUES (1, 'default', '默认租户');
INSERT INTO patient (id, tenant_id, name, gender, birth_date, tags, therapy_start, notes)
VALUES (1, 1, '父亲', 'male', '1968-01-01',
        '["PD-1","DM","post-surgery","wheelchair"]', '2026-05-18',
        '57 岁，2 型糖尿病，左足底植皮 + 小腿取皮，长期轮椅 6 个月，PD-1 治疗中');
INSERT INTO caregiver (id, tenant_id, patient_id, name, relation, is_primary)
VALUES (1, 1, 1, '家属', 'child', 1);

-- 推送通道：默认创建占位，应用启动后由管理员填 webhook
INSERT INTO notify_channel (id, tenant_id, patient_id, channel_type, display_name, webhook_url, enabled)
VALUES (1, 1, 1, 'feishu_bot', '家属飞书群', 'REPLACE_WITH_FEISHU_WEBHOOK', 0);
INSERT INTO caregiver_channel (caregiver_id, channel_id) VALUES (1, 1);

-- =========================================================
-- 2. 模板：晨间 checklist（每天 07:30）
-- =========================================================
INSERT INTO checklist_template (id, tenant_id, code, name, trigger_type, trigger_cron, default_due_time, applicable_tags, reminder_cron, description)
VALUES (1, 1, 'daily_morning', '早晨 checklist', 'daily', '0 0 0 * * *', '10:00',
        '["PD-1"]', '0 30 7 * * *', '晨起监测：FAST、体温、血压、血糖、服药、尿色眼白');

INSERT INTO checklist_item (template_id, code, label, section, input_type, unit, normal_range, red_flag_rule, required, order_no, hint) VALUES
(1, 'fast_test',        '30 秒 FAST 晨间互动（嘴角对称 / 双手平举 / 说话清晰）', '早晨', 'composite', NULL, '全部正常', 'FAST_ANY_ABNORMAL', 1, 10, '任一异常 → 立即拨 120，并记下症状起始时间'),
(1, 'body_temp',        '体温',             '早晨', 'number', '℃',       '36.0–37.2', 'BODY_TEMP_OVER_38', 1, 20, '≥ 38℃ 联系医生'),
(1, 'bp',               '血压（收缩/舒张）', '早晨', 'bp',     'mmHg',     '< 130/80',  'BP_OUT_OF_RANGE',    1, 30, '目标 < 130/80'),
(1, 'heart_rate',       '心率',             '早晨', 'number', '次/分',    '60–100',    'HR_OUT_OF_RANGE',    1, 40, NULL),
(1, 'glucose_fasting',  '空腹血糖',          '早晨', 'number', 'mmol/L',   '4.4–7.0',   'GLUCOSE_FASTING_HIGH', 1, 50, '> 13.9 + 尿酮 ≥ ++ → DKA 急诊'),
(1, 'morning_water',    '喝水 1 杯（500 mL）', '早晨', 'checkbox', NULL, NULL, NULL, 0, 60, NULL),
(1, 'take_metformin',   '服二甲双胍',        '早晨', 'checkbox', NULL, NULL, NULL, 0, 70, NULL),
(1, 'take_bp_med',      '服降压药（如有）',  '早晨', 'checkbox', NULL, NULL, NULL, 0, 71, NULL),
(1, 'take_other_med',   '服其他药（备注）',  '早晨', 'text',     NULL, NULL, NULL, 0, 72, NULL),
(1, 'urine_color',      '尿色（清/淡黄/深茶色）', '早晨', 'text', NULL, '清 / 淡黄', 'URINE_DARK', 1, 80, '深茶色 → 立即查肝功'),
(1, 'sclera_yellow',    '眼白发黄？',        '早晨', 'checkbox', NULL, '否', 'JAUNDICE', 1, 90, '发黄 → 立即查肝功');

-- =========================================================
-- 3. 模板：晚间 checklist（每天 21:00）
-- =========================================================
INSERT INTO checklist_template (id, tenant_id, code, name, trigger_type, trigger_cron, default_due_time, applicable_tags, reminder_cron, description)
VALUES (2, 1, 'daily_evening', '晚间 checklist + 症状日记', 'daily', '0 0 0 * * *', '23:00',
        '["PD-1"]', '0 0 21 * * *', '晚间复测与症状日记');

INSERT INTO checklist_item (template_id, code, label, section, input_type, unit, normal_range, red_flag_rule, required, order_no, hint) VALUES
(2, 'bp_evening',        '血压（收缩/舒张）', '晚上', 'bp',     'mmHg',     '< 130/80', 'BP_OUT_OF_RANGE',    1, 10, NULL),
(2, 'glucose_pp',        '餐后 2h 血糖',      '晚上', 'number', 'mmol/L',   '< 10.0',   'GLUCOSE_PP_HIGH',    1, 20, NULL),
(2, 'take_statin',       '睡前服他汀 1 片',   '晚上', 'checkbox', NULL, NULL, NULL, 1, 30, NULL),
(2, 'total_water',       '今日总饮水量',      '晚上', 'number', 'L',         '≥ 2.0',   'WATER_LOW',          1, 40, '< 2L 需警惕'),
(2, 'urine_color_pm',    '尿色',              '晚上', 'text',   NULL,        '清 / 淡黄', 'URINE_DARK',        0, 50, NULL),
(2, 'urine_volume',      '尿量（约）',        '晚上', 'number', 'mL',        NULL,       NULL,                  0, 60, NULL),
(2, 'stool_count',       '大便次数',          '晚上', 'number', '次',        '1–2',      'DIARRHEA_HIGH',      1, 70, '≥ 7 次 / 天 或带血 → 24h 联系医生'),
(2, 'stool_blood',       '大便带血？',        '晚上', 'checkbox', NULL, '否', 'STOOL_BLOOD',                     1, 71, NULL),
(2, 'rash',              '皮疹？',            '晚上', 'checkbox', NULL, '否', 'RASH_SEVERE',                     1, 80, '广泛皮疹 + 黏膜糜烂 → SJS/TEN 急诊'),
(2, 'itch_score',        '瘙痒 0–10 分',      '晚上', 'number', '分',        '< 4',      NULL,                  0, 81, NULL),
(2, 'cough_dyspnea',     '咳嗽 / 气短？',     '晚上', 'checkbox', NULL, '否', 'COUGH_DYSPNEA',                   1, 90, '进行性气短 + 干咳 + SpO₂ < 92% → 24h 急诊'),
(2, 'chest_pain',        '胸痛 / 心悸？',     '晚上', 'checkbox', NULL, '否', 'CHEST_PAIN',                      1, 100, '突发胸痛 + 气短 + 心悸 → 拨 120'),
(2, 'headache',          '头痛？',            '晚上', 'checkbox', NULL, '否', 'HEADACHE_SEVERE',                 1, 110, '持续剧烈头痛 + 视野缺损 → 24h 急诊（垂体炎）'),
(2, 'unusual_fatigue',   '异常乏力？',        '晚上', 'checkbox', NULL, '否', 'UNUSUAL_FATIGUE',                 1, 120, '乏力 + 低血压 + 呕吐 → 肾上腺危象急诊'),
(2, 'diary_note',        '其他备注',          '晚上', 'text',   NULL,        NULL,       NULL,                   0, 130, NULL);

-- =========================================================
-- 4. 模板：每周左腿 + 周度监测（周一 08:00）
-- =========================================================
INSERT INTO checklist_template (id, tenant_id, code, name, trigger_type, trigger_cron, default_due_time, applicable_tags, reminder_cron, description)
VALUES (3, 1, 'weekly_leg', '每周监测（左腿围度 + 全身）', 'weekly', '0 0 0 * * MON', '20:00',
        '["PD-1","post-surgery","wheelchair"]', '0 0 8 * * MON', '左腿 4 部位周长 + 3 个非围度信号 + 全身');

INSERT INTO checklist_item (template_id, code, label, section, input_type, unit, normal_range, red_flag_rule, required, order_no, hint) VALUES
(3, 'leg_thigh_mid',     '左腿大腿中段（髌骨上 15 cm）', '左腿', 'number', 'cm', NULL, 'LEG_CIRC_INCREASE', 1, 10, '与上周同部位对比'),
(3, 'leg_below_knee',    '左腿膝下 5 cm',                '左腿', 'number', 'cm', NULL, 'LEG_CIRC_INCREASE', 1, 20, NULL),
(3, 'leg_calf_max',      '左腿小腿最粗处',               '左腿', 'number', 'cm', NULL, 'LEG_CIRC_INCREASE', 1, 30, NULL),
(3, 'leg_above_ankle',   '左腿踝上 5 cm',                '左腿', 'number', 'cm', NULL, 'LEG_CIRC_INCREASE', 1, 40, '任一部位 1 周内增 ≥ 2cm 或 3 天内增 ≥ 1cm → 警惕 DVT'),
(3, 'leg_photos',        '左腿 4 方位拍照（正/内/外/后）', '左腿', 'photo', NULL, NULL, NULL, 1, 50, '存入"左腿监测"专辑'),
(3, 'leg_temp_diff',     '左小腿比右侧明显更热？',        '左腿', 'checkbox', NULL, '否', 'LEG_TEMP_DIFF', 1, 60, NULL),
(3, 'leg_pitting_edema', '胫骨内侧按压 5 秒凹陷不回弹？', '左腿', 'checkbox', NULL, '否', 'LEG_PITTING',   1, 70, NULL),
(3, 'leg_new_pain',      '左腿新出现疼痛/紧绷/沉重感？',  '左腿', 'checkbox', NULL, '否', 'LEG_NEW_PAIN',  1, 80, NULL),
(3, 'weight',            '体重',                           '全身', 'number', 'kg', NULL, 'WEIGHT_DELTA_3KG', 1, 90, '月变化 ≥ 3kg 联系医生'),
(3, 'spo2',              '指尖血氧',                       '全身', 'number', '%',  '≥ 94', 'SPO2_LOW',      1, 100, '< 94% 立即就医'),
(3, 'skin_check',        '全身皮肤检查已完成（含面、手、生殖区，已拍照对比）', '全身', 'checkbox', NULL, NULL, NULL, 1, 110, NULL),
(3, 'graft_site_check',  '植皮区（足底/小腿取皮）检查：红肿/渗液/破溃/缝线异常？', '全身', 'checkbox', NULL, '无', 'GRAFT_ABNORMAL', 1, 120, '有异常 → 联系整形外科'),
(3, 'pulse_irregular',   '1 分钟脉搏不规整？',             '全身', 'checkbox', NULL, '否', 'PULSE_IRREGULAR', 1, 130, '防新发房颤');

-- =========================================================
-- 5. 模板：每月输液前抽血 + 复盘
-- =========================================================
INSERT INTO checklist_template (id, tenant_id, code, name, trigger_type, trigger_cron, default_due_time, applicable_tags, reminder_cron, description)
VALUES (4, 1, 'monthly_infusion_lab', '每月输液前抽血项', 'monthly', '0 0 0 1 * *', '18:00',
        '["PD-1"]', '0 0 9 1 * *', '每月输液前抽血与准备');

INSERT INTO checklist_item (template_id, code, label, section, input_type, unit, normal_range, red_flag_rule, required, order_no, hint) VALUES
(4, 'lab_cbc',              '血常规',                    '抽血', 'checkbox', NULL, NULL, NULL, 1, 10, NULL),
(4, 'lab_liver_kidney',     '肝肾功能、电解质',           '抽血', 'checkbox', NULL, NULL, NULL, 1, 20, NULL),
(4, 'lab_glucose_fasting',  '空腹血糖',                  '抽血', 'checkbox', NULL, NULL, NULL, 1, 30, NULL),
(4, 'lab_urine_uacr',       '尿常规 + UACR',             '抽血', 'checkbox', NULL, NULL, NULL, 1, 40, NULL),
(4, 'lab_d_dimer',          'D-二聚体 + 凝血（PT/APTT/FIB）', '抽血', 'checkbox', NULL, NULL, NULL, 1, 50, 'DVT 监测'),
(4, 'lab_thyroid_6w',       '（每 6 周加查）甲功 5 项',  '抽血', 'checkbox', NULL, NULL, NULL, 0, 60, NULL),
(4, 'monthly_review',       '复盘本月症状 + 准备复诊问题', '复盘', 'text',   NULL, NULL, NULL, 1, 70, NULL);

-- =========================================================
-- 6. 模板：输液前 24h（相对事件触发）
-- =========================================================
INSERT INTO checklist_template (id, tenant_id, code, name, trigger_type, trigger_cron, default_due_time, applicable_tags, reminder_cron, description)
VALUES (5, 1, 'infusion_t_minus_1d', '输液前 24 小时准备', 'relative_event', 'infusion:-1d', '22:00',
        '["PD-1"]', NULL, '每次输液前一天推送；挂钩 medical_event.event_type=infusion');

INSERT INTO checklist_item (template_id, code, label, section, input_type, unit, normal_range, red_flag_rule, required, order_no, hint) VALUES
(5, 'hydration',      '饮水 ≥ 2.5–3 L',                '准备', 'checkbox', NULL, NULL, NULL, 1, 10, NULL),
(5, 'light_diet',     '清淡饮食、不饮酒',              '准备', 'checkbox', NULL, NULL, NULL, 1, 20, NULL),
(5, 'good_sleep',     '充足睡眠',                      '准备', 'checkbox', NULL, NULL, NULL, 1, 30, NULL),
(5, 'no_new_herbs',   '不服新启用的中药 / 保健品',    '准备', 'checkbox', NULL, NULL, NULL, 1, 40, NULL),
(5, 'pack_diary',     '整理症状日记带去医院',          '准备', 'checkbox', NULL, NULL, NULL, 1, 50, NULL),
(5, 'new_symptoms',   '列出任何新症状（即使轻微）',    '准备', 'text',     NULL, NULL, NULL, 0, 60, NULL);

-- =========================================================
-- 7. 模板：输液当天
-- =========================================================
INSERT INTO checklist_template (id, tenant_id, code, name, trigger_type, trigger_cron, default_due_time, applicable_tags, reminder_cron, description)
VALUES (6, 1, 'infusion_day', '输液当天监测', 'relative_event', 'infusion:0d', '20:00',
        '["PD-1"]', NULL, '输液当天检查 + 化验 + 输液中观察');

INSERT INTO checklist_item (template_id, code, label, section, input_type, unit, normal_range, red_flag_rule, required, order_no, hint) VALUES
(6, 'pre_vitals',        '输液前：血压、心率、体温、SpO₂',  '输液', 'text',     NULL, NULL, NULL, 1, 10, NULL),
(6, 'pre_labs',          '化验：血常规/肝肾功/电解质/血糖/TSH', '输液', 'checkbox', NULL, NULL, NULL, 1, 20, NULL),
(6, 'monthly_extra',     '当月该加查项',                    '输液', 'text',     NULL, NULL, NULL, 0, 30, NULL),
(6, 'during_watch',      '输液中观察：寒战/皮肤潮红/胸闷',  '输液', 'checkbox', NULL, '无', 'INFUSION_REACTION', 1, 40, '异常按铃叫护士'),
(6, 'post_observe_1h',   '输液后观察 ≥ 1 小时（首次 ≥ 2 小时）', '输液', 'checkbox', NULL, NULL, NULL, 1, 50, NULL),
(6, 'next_infusion_date','下次输液时间',                   '离院', 'text',     NULL, NULL, NULL, 1, 60, NULL),
(6, 'emergency_contact', '24h 联系电话',                    '离院', 'text',     NULL, NULL, NULL, 1, 70, NULL);
