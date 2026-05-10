package com.immunesentinel.application.checklist;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.immunesentinel.domain.checklist.ChecklistInstance;
import com.immunesentinel.domain.checklist.ChecklistTemplate;
import com.immunesentinel.domain.medical.MedicalEvent;
import com.immunesentinel.domain.patient.Patient;
import com.immunesentinel.infrastructure.persistence.mapper.ChecklistInstanceMapper;
import com.immunesentinel.infrastructure.persistence.mapper.ChecklistTemplateMapper;
import com.immunesentinel.infrastructure.persistence.mapper.MedicalEventMapper;
import com.immunesentinel.infrastructure.persistence.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 按日期 & 触发类型，为指定患者幂等生成 checklist_instance。
 * 触发类型：
 *   daily     → 每天
 *   weekly    → 仅周一（MVP 固定，后续可放 cron）
 *   monthly   → 每月 1 日
 *   quarterly → 每月 1 日 & 月份 = 3,6,9,12
 *   relative_event → 按 medical_event 触发（单独处理）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistInstanceGenerator {

    private final PatientMapper patientMapper;
    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistInstanceMapper instanceMapper;
    private final MedicalEventMapper medicalEventMapper;

    public void generateFor(LocalDate date) {
        List<Patient> patients = patientMapper.selectList(
            new LambdaQueryWrapper<Patient>().eq(Patient::getDeleted, 0));

        for (Patient p : patients) {
            List<ChecklistTemplate> tpls = templateMapper.selectList(
                new LambdaQueryWrapper<ChecklistTemplate>()
                    .eq(ChecklistTemplate::getTenantId, p.getTenantId())
                    .eq(ChecklistTemplate::getEnabled, 1)
                    .eq(ChecklistTemplate::getDeleted, 0));

            for (ChecklistTemplate t : tpls) {
                if (!shouldTrigger(t, date)) continue;
                createIfAbsent(p, t, date);
            }

            // relative_event：针对接下来 7 天内的输液事件
            generateForUpcomingEvents(p, date);
        }
    }

    private boolean shouldTrigger(ChecklistTemplate t, LocalDate date) {
        return switch (t.getTriggerType()) {
            case "daily" -> true;
            case "weekly" -> date.getDayOfWeek().getValue() == 1;
            case "monthly" -> date.getDayOfMonth() == 1;
            case "quarterly" -> date.getDayOfMonth() == 1
                && (date.getMonthValue() == 3 || date.getMonthValue() == 6
                    || date.getMonthValue() == 9 || date.getMonthValue() == 12);
            default -> false;
        };
    }

    private void generateForUpcomingEvents(Patient p, LocalDate today) {
        List<ChecklistTemplate> relatives = templateMapper.selectList(
            new LambdaQueryWrapper<ChecklistTemplate>()
                .eq(ChecklistTemplate::getTenantId, p.getTenantId())
                .eq(ChecklistTemplate::getTriggerType, "relative_event")
                .eq(ChecklistTemplate::getEnabled, 1));
        if (relatives.isEmpty()) return;

        List<MedicalEvent> upcoming = medicalEventMapper.selectList(
            new LambdaQueryWrapper<MedicalEvent>()
                .eq(MedicalEvent::getPatientId, p.getId())
                .eq(MedicalEvent::getDeleted, 0)
                .eq(MedicalEvent::getStatus, "planned")
                .between(MedicalEvent::getScheduledAt,
                    today.atStartOfDay(), today.plusDays(7).atTime(23, 59, 59)));

        for (MedicalEvent ev : upcoming) {
            for (ChecklistTemplate t : relatives) {
                // 格式约定：trigger_cron = "infusion:-1d" / "infusion:0d"
                String expr = t.getTriggerCron();
                if (expr == null || !expr.contains(":")) continue;
                String[] parts = expr.split(":");
                if (!parts[0].equalsIgnoreCase(ev.getEventType())) continue;
                int offsetDays = parseOffsetDays(parts[1]);
                LocalDate targetDate = ev.getScheduledAt().toLocalDate().plusDays(offsetDays);
                if (!targetDate.equals(today)) continue;
                createIfAbsent(p, t, targetDate);
            }
        }
    }

    private int parseOffsetDays(String s) {
        // 支持 "-1d" / "0d" / "+1d"
        String n = s.replace("d", "").replace("D", "");
        return Integer.parseInt(n);
    }

    private void createIfAbsent(Patient p, ChecklistTemplate t, LocalDate date) {
        Long exists = instanceMapper.selectCount(
            new LambdaQueryWrapper<ChecklistInstance>()
                .eq(ChecklistInstance::getPatientId, p.getId())
                .eq(ChecklistInstance::getTemplateCode, t.getCode())
                .eq(ChecklistInstance::getDueDate, date));
        if (exists != null && exists > 0) return;

        LocalTime due = parseDue(t.getDefaultDueTime());
        ChecklistInstance inst = new ChecklistInstance();
        inst.setTenantId(p.getTenantId());
        inst.setPatientId(p.getId());
        inst.setTemplateId(t.getId());
        inst.setTemplateCode(t.getCode());
        inst.setDueDate(date);
        inst.setDueAt(LocalDateTime.of(date, due));
        inst.setStatus("pending");
        inst.setAbnormalCount(0);
        instanceMapper.insert(inst);
        log.info("generated checklist instance patient={} template={} date={}",
            p.getId(), t.getCode(), date);
    }

    private LocalTime parseDue(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) return LocalTime.of(22, 0);
        String[] parts = hhmm.split(":");
        return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
