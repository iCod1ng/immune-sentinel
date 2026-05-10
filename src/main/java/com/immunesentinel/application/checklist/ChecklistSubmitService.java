package com.immunesentinel.application.checklist;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.immunesentinel.domain.checklist.*;
import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagEngine;
import com.immunesentinel.domain.redflag.RedFlagEvent;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.vital.LegMeasurement;
import com.immunesentinel.domain.vital.VitalSignLog;
import com.immunesentinel.infrastructure.notifier.NotifyMessage;
import com.immunesentinel.infrastructure.notifier.NotifyService;
import com.immunesentinel.infrastructure.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 填报提交的核心编排：幂等写 item_record，同步冗余到 vital_sign_log / leg_measurement，
 * 命中风险预警规则则写 red_flag_event 并推送告警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistSubmitService {

    private final ChecklistInstanceMapper instanceMapper;
    private final ChecklistItemMapper itemMapper;
    private final ChecklistItemRecordMapper recordMapper;
    private final VitalSignLogMapper vitalLogMapper;
    private final LegMeasurementMapper legMapper;
    private final RedFlagEventMapper redFlagEventMapper;
    private final RedFlagEngine redFlagEngine;
    private final NotifyService notifyService;

    @Transactional
    public SubmitOutcome submit(Long instanceId, Map<String, Map<String, Object>> values, String recordedBy) {
        ChecklistInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) throw new IllegalArgumentException("实例不存在");

        List<ChecklistItem> items = itemMapper.selectList(
            new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, inst.getTemplateId())
                .eq(ChecklistItem::getDeleted, 0));

        List<RedFlagHit> hits = new ArrayList<>();
        int abnormalCount = 0;
        int filledCount = 0;

        for (ChecklistItem item : items) {
            Map<String, Object> val = values.get(item.getCode());
            if (val == null || val.isEmpty()) continue;
            filledCount++;

            RedFlagContext ctx = RedFlagContext.builder()
                .tenantId(inst.getTenantId())
                .patientId(inst.getPatientId())
                .itemCode(item.getCode())
                .value(val)
                .aux(buildAux(inst.getPatientId(), item, val))
                .build();
            RedFlagHit hit = redFlagEngine.evaluate(item.getRedFlagRule(), ctx);
            boolean abnormal = hit.isHit();
            if (abnormal) abnormalCount++;

            upsertRecord(inst, item, val, abnormal, recordedBy);
            ChecklistItemRecord fresh = recordMapper.selectOne(
                new LambdaQueryWrapper<ChecklistItemRecord>()
                    .eq(ChecklistItemRecord::getInstanceId, inst.getId())
                    .eq(ChecklistItemRecord::getItemId, item.getId()));

            mirrorToVitalOrLeg(inst, item, val, fresh);

            if (abnormal) {
                RedFlagEvent ev = persistRedFlag(inst, item, hit, fresh);
                hits.add(hit);
                notifyRedFlag(inst.getPatientId(), hit, ev);
            }
        }

        inst.setAbnormalCount(abnormalCount);
        inst.setStatus(filledCount >= items.size() ? "done" : "partial");
        if ("done".equals(inst.getStatus())) inst.setCompletedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        return new SubmitOutcome(inst.getStatus(), abnormalCount, hits);
    }

    private Map<String, Object> buildAux(Long patientId, ChecklistItem item, Map<String, Object> val) {
        if (!"LEG_CIRC_INCREASE".equals(item.getRedFlagRule())) return null;
        String site = legSiteFromCode(item.getCode());
        if (site == null) return null;

        LocalDateTime now = LocalDateTime.now();
        LegMeasurement lastWeek = legMapper.selectOne(
            new LambdaQueryWrapper<LegMeasurement>()
                .eq(LegMeasurement::getPatientId, patientId)
                .eq(LegMeasurement::getSite, site)
                .between(LegMeasurement::getMeasuredAt, now.minusDays(10), now.minusDays(4))
                .orderByDesc(LegMeasurement::getMeasuredAt)
                .last("limit 1"));
        LegMeasurement d3 = legMapper.selectOne(
            new LambdaQueryWrapper<LegMeasurement>()
                .eq(LegMeasurement::getPatientId, patientId)
                .eq(LegMeasurement::getSite, site)
                .between(LegMeasurement::getMeasuredAt, now.minusDays(4), now.minusHours(12))
                .orderByDesc(LegMeasurement::getMeasuredAt)
                .last("limit 1"));

        Map<String, Object> aux = new HashMap<>();
        if (lastWeek != null) aux.put("lastWeek", lastWeek.getCircumferenceCm());
        if (d3 != null) aux.put("days3", d3.getCircumferenceCm());
        return aux;
    }

    private void upsertRecord(ChecklistInstance inst, ChecklistItem item, Map<String, Object> val,
                              boolean abnormal, String recordedBy) {
        ChecklistItemRecord exist = recordMapper.selectOne(
            new LambdaQueryWrapper<ChecklistItemRecord>()
                .eq(ChecklistItemRecord::getInstanceId, inst.getId())
                .eq(ChecklistItemRecord::getItemId, item.getId()));
        if (exist == null) {
            ChecklistItemRecord r = new ChecklistItemRecord();
            r.setTenantId(inst.getTenantId());
            r.setPatientId(inst.getPatientId());
            r.setInstanceId(inst.getId());
            r.setItemId(item.getId());
            r.setItemCode(item.getCode());
            r.setValueJson(JSONUtil.toJsonStr(val));
            r.setIsAbnormal(abnormal ? 1 : 0);
            r.setRecordedAt(LocalDateTime.now());
            r.setRecordedBy(recordedBy);
            recordMapper.insert(r);
        } else {
            exist.setValueJson(JSONUtil.toJsonStr(val));
            exist.setIsAbnormal(abnormal ? 1 : 0);
            exist.setRecordedAt(LocalDateTime.now());
            exist.setRecordedBy(recordedBy);
            recordMapper.updateById(exist);
        }
    }

    private void mirrorToVitalOrLeg(ChecklistInstance inst, ChecklistItem item,
                                     Map<String, Object> val, ChecklistItemRecord r) {
        String metric = vitalMetricCode(item.getCode());
        if (metric != null && val.get("num") != null) {
            VitalSignLog v = new VitalSignLog();
            v.setTenantId(inst.getTenantId());
            v.setPatientId(inst.getPatientId());
            v.setMetricCode(metric);
            v.setValueNum(new BigDecimal(val.get("num").toString()));
            v.setUnit(item.getUnit());
            v.setMeasuredAt(LocalDateTime.now());
            v.setSource("checklist");
            v.setSourceRef(r.getId());
            vitalLogMapper.insert(v);
        }
        if ("bp".equals(item.getInputType()) && val.get("sys") != null && val.get("dia") != null) {
            LocalDateTime now = LocalDateTime.now();
            insertBp(inst, r, "bp_sys", val.get("sys"), now);
            insertBp(inst, r, "bp_dia", val.get("dia"), now);
        }
        String site = legSiteFromCode(item.getCode());
        if (site != null && val.get("num") != null) {
            LegMeasurement lm = new LegMeasurement();
            lm.setTenantId(inst.getTenantId());
            lm.setPatientId(inst.getPatientId());
            lm.setSite(site);
            lm.setCircumferenceCm(new BigDecimal(val.get("num").toString()));
            lm.setMeasuredAt(LocalDateTime.now());
            legMapper.insert(lm);
        }
    }

    private void insertBp(ChecklistInstance inst, ChecklistItemRecord r, String metric, Object v, LocalDateTime at) {
        VitalSignLog vs = new VitalSignLog();
        vs.setTenantId(inst.getTenantId());
        vs.setPatientId(inst.getPatientId());
        vs.setMetricCode(metric);
        vs.setValueNum(new BigDecimal(v.toString()));
        vs.setUnit("mmHg");
        vs.setMeasuredAt(at);
        vs.setSource("checklist");
        vs.setSourceRef(r.getId());
        vitalLogMapper.insert(vs);
    }

    private String vitalMetricCode(String itemCode) {
        return switch (itemCode) {
            case "body_temp" -> "temp";
            case "heart_rate" -> "hr";
            case "glucose_fasting" -> "glucose_fasting";
            case "glucose_pp" -> "glucose_pp";
            case "spo2" -> "spo2";
            case "weight" -> "weight";
            case "total_water" -> "water_total";
            case "itch_score" -> "itch_score";
            case "urine_volume" -> "urine_volume";
            case "stool_count" -> "stool_count";
            default -> null;
        };
    }

    private String legSiteFromCode(String code) {
        return switch (code) {
            case "leg_thigh_mid" -> "thigh_mid";
            case "leg_below_knee" -> "below_knee";
            case "leg_calf_max" -> "calf_max";
            case "leg_above_ankle" -> "above_ankle";
            default -> null;
        };
    }

    private RedFlagEvent persistRedFlag(ChecklistInstance inst, ChecklistItem item,
                                         RedFlagHit hit, ChecklistItemRecord r) {
        RedFlagEvent ev = new RedFlagEvent();
        ev.setTenantId(inst.getTenantId());
        ev.setPatientId(inst.getPatientId());
        ev.setRuleCode(hit.getRuleCode());
        ev.setSeverity(hit.getSeverity().name().toLowerCase());
        ev.setTitle(hit.getTitle());
        ev.setDetail(hit.getDetail());
        ev.setTriggeredBy("item_record:" + r.getId());
        ev.setSourceRef(r.getId());
        ev.setTriggeredAt(LocalDateTime.now());
        redFlagEventMapper.insert(ev);
        return ev;
    }

    private void notifyRedFlag(Long patientId, RedFlagHit hit, RedFlagEvent ev) {
        NotifyMessage.Severity sev = switch (hit.getSeverity()) {
            case EMERGENCY -> NotifyMessage.Severity.EMERGENCY;
            case H24 -> NotifyMessage.Severity.WARN;
            default -> NotifyMessage.Severity.INFO;
        };
        String content = hit.severityLabel() + "\n\n" + hit.getDetail();
        NotifyMessage msg = NotifyMessage.builder()
            .category("red_flag")
            .severity(sev)
            .title("[" + hit.severityLabel() + "] " + hit.getTitle())
            .content(content)
            .build();
        notifyService.dispatch(patientId, msg);
    }

    public record SubmitOutcome(String status, int abnormalCount, List<RedFlagHit> hits) {}
}
