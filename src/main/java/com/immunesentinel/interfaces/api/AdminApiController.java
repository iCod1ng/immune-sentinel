package com.immunesentinel.interfaces.api;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.immunesentinel.application.checklist.ChecklistInstanceGenerator;
import com.immunesentinel.application.checklist.TokenSigner;
import com.immunesentinel.application.reminder.ReminderService;
import com.immunesentinel.config.SentinelProperties;
import com.immunesentinel.domain.checklist.ChecklistInstance;
import com.immunesentinel.domain.medical.MedicalEvent;
import com.immunesentinel.domain.notify.NotifyChannel;
import com.immunesentinel.infrastructure.notifier.NotifyMessage;
import com.immunesentinel.infrastructure.notifier.NotifyService;
import com.immunesentinel.infrastructure.persistence.mapper.ChecklistInstanceMapper;
import com.immunesentinel.infrastructure.persistence.mapper.MedicalEventMapper;
import com.immunesentinel.infrastructure.persistence.mapper.NotifyChannelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 轻量管理端 API：首次上线配 webhook、手动触发提醒、登记输液事件、查看今日 checklist。
 * 用 sentinel.admin-token 做鉴权（Header: X-Admin-Token），不做登录页；够 MVP 用。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final SentinelProperties props;
    private final NotifyChannelMapper channelMapper;
    private final NotifyService notifyService;
    private final ChecklistInstanceGenerator generator;
    private final ReminderService reminderService;
    private final ChecklistInstanceMapper instanceMapper;
    private final MedicalEventMapper medicalEventMapper;
    private final TokenSigner tokenSigner;

    private void auth(String header) {
        if (StrUtil.isBlank(props.getAdminToken()) || !props.getAdminToken().equals(header)) {
            throw new RuntimeException("admin token invalid");
        }
    }

    @PostMapping("/channels/{id}")
    public Map<String, Object> updateChannel(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        auth(token);
        NotifyChannel ch = channelMapper.selectById(id);
        if (ch == null) throw new RuntimeException("channel not found");
        if (body.containsKey("webhookUrl")) ch.setWebhookUrl(body.get("webhookUrl"));
        if (body.containsKey("secret")) ch.setSecret(body.get("secret"));
        if (body.containsKey("channelType")) ch.setChannelType(body.get("channelType"));
        if (body.containsKey("enabled")) ch.setEnabled(Integer.parseInt(body.get("enabled")));
        channelMapper.updateById(ch);
        return Map.of("ok", true, "id", ch.getId());
    }

    @PostMapping("/channels/{id}/test")
    public Map<String, Object> testChannel(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable Long id) {
        auth(token);
        NotifyChannel ch = channelMapper.selectById(id);
        if (ch == null) throw new RuntimeException("channel not found");
        notifyService.dispatch(ch.getPatientId(), NotifyMessage.builder()
            .category("test")
            .severity(NotifyMessage.Severity.INFO)
            .title("immune-sentinel 连通性测试")
            .content("如果你看到这条消息，说明推送通道已接通。")
            .build());
        return Map.of("ok", true);
    }

    @PostMapping("/generate")
    public Map<String, Object> generateToday(@RequestHeader("X-Admin-Token") String token) {
        auth(token);
        generator.generateFor(LocalDate.now());
        return Map.of("ok", true);
    }

    @PostMapping("/reminder/{templateCode}")
    public Map<String, Object> triggerReminder(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable String templateCode) {
        auth(token);
        reminderService.pushReminderForTemplate(templateCode);
        return Map.of("ok", true, "template", templateCode);
    }

    @GetMapping("/today")
    public Map<String, Object> today(@RequestHeader("X-Admin-Token") String token) {
        auth(token);
        List<ChecklistInstance> list = instanceMapper.selectList(
            new LambdaQueryWrapper<ChecklistInstance>()
                .eq(ChecklistInstance::getDueDate, LocalDate.now()));
        return Map.of(
            "date", LocalDate.now().toString(),
            "instances", list.stream().map(i -> Map.of(
                "id", i.getId(),
                "templateCode", i.getTemplateCode(),
                "status", i.getStatus(),
                "abnormalCount", i.getAbnormalCount(),
                "url", props.getBaseUrl() + "/c/" + tokenSigner.sign(i.getPatientId(), i.getId())
            )).toList()
        );
    }

    @PostMapping("/medical-events")
    public Map<String, Object> createEvent(
            @RequestHeader("X-Admin-Token") String token,
            @RequestBody Map<String, Object> body) {
        auth(token);
        MedicalEvent ev = new MedicalEvent();
        ev.setTenantId(Long.valueOf(body.getOrDefault("tenantId", 1).toString()));
        ev.setPatientId(Long.valueOf(body.getOrDefault("patientId", 1).toString()));
        ev.setEventType((String) body.getOrDefault("eventType", "infusion"));
        ev.setTitle((String) body.getOrDefault("title", "PD-1 输液"));
        ev.setScheduledAt(LocalDateTime.parse((String) body.get("scheduledAt")));
        ev.setStatus("planned");
        medicalEventMapper.insert(ev);
        return Map.of("ok", true, "id", ev.getId());
    }

    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> err(RuntimeException e) {
        return Map.of("ok", false, "error", e.getMessage());
    }
}
