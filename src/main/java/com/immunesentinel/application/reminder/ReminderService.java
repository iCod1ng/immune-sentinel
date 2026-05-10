package com.immunesentinel.application.reminder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.immunesentinel.application.checklist.TokenSigner;
import com.immunesentinel.config.SentinelProperties;
import com.immunesentinel.domain.checklist.ChecklistInstance;
import com.immunesentinel.domain.checklist.ChecklistTemplate;
import com.immunesentinel.infrastructure.notifier.NotifyMessage;
import com.immunesentinel.infrastructure.notifier.NotifyService;
import com.immunesentinel.infrastructure.persistence.mapper.ChecklistInstanceMapper;
import com.immunesentinel.infrastructure.persistence.mapper.ChecklistTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 推送当天指定模板的 checklist 提醒。
 * 幂等策略：不特别做记录（notify_log 已有痕迹），重复推送影响可接受；真要做可用 reminder_rule 表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ChecklistInstanceMapper instanceMapper;
    private final ChecklistTemplateMapper templateMapper;
    private final NotifyService notifyService;
    private final TokenSigner tokenSigner;
    private final SentinelProperties props;

    public void pushReminderForTemplate(String templateCode) {
        LocalDate today = LocalDate.now();

        List<ChecklistInstance> todayInstances = instanceMapper.selectList(
            new LambdaQueryWrapper<ChecklistInstance>()
                .eq(ChecklistInstance::getTemplateCode, templateCode)
                .eq(ChecklistInstance::getDueDate, today)
                .in(ChecklistInstance::getStatus, List.of("pending", "partial")));

        for (ChecklistInstance inst : todayInstances) {
            ChecklistTemplate tpl = templateMapper.selectById(inst.getTemplateId());
            String token = tokenSigner.sign(inst.getPatientId(), inst.getId());
            String url = props.getBaseUrl() + "/c/" + token;
            String title = icon(templateCode) + " " + tpl.getName();
            String content = tpl.getDescription() == null ? "请点按钮填写今日清单。" : tpl.getDescription();

            NotifyMessage msg = NotifyMessage.builder()
                .category("reminder")
                .severity(NotifyMessage.Severity.INFO)
                .title(title)
                .content(content)
                .actions(List.of(NotifyMessage.Action.builder().label("立即填写").url(url).build()))
                .build();
            notifyService.dispatch(inst.getPatientId(), msg);
        }
    }

    private String icon(String code) {
        return switch (code) {
            case "daily_morning" -> "🌅";
            case "daily_evening" -> "🌙";
            case "weekly_leg" -> "📏";
            case "monthly_infusion_lab" -> "💉";
            case "infusion_t_minus_1d" -> "📋";
            case "infusion_day" -> "🏥";
            default -> "📝";
        };
    }
}
