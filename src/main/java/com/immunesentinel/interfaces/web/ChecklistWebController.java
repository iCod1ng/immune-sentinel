package com.immunesentinel.interfaces.web;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.immunesentinel.application.checklist.ChecklistSubmitService;
import com.immunesentinel.application.checklist.TokenSigner;
import com.immunesentinel.domain.checklist.ChecklistInstance;
import com.immunesentinel.domain.checklist.ChecklistItem;
import com.immunesentinel.domain.checklist.ChecklistItemRecord;
import com.immunesentinel.domain.checklist.ChecklistTemplate;
import com.immunesentinel.domain.patient.Patient;
import com.immunesentinel.infrastructure.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * H5 填报页：移动端优先；一次性 token 鉴权；post 后跳成功页。
 * 路径 /c/{token} 故意短，方便在飞书卡片里贴。
 */
@Controller
@RequiredArgsConstructor
public class ChecklistWebController {

    private final TokenSigner tokenSigner;
    private final ChecklistInstanceMapper instanceMapper;
    private final ChecklistTemplateMapper templateMapper;
    private final ChecklistItemMapper itemMapper;
    private final ChecklistItemRecordMapper recordMapper;
    private final PatientMapper patientMapper;
    private final ChecklistSubmitService submitService;

    @GetMapping("/c/{token}")
    public String form(@PathVariable String token, Model model) {
        TokenSigner.Parsed p = tokenSigner.verify(token);
        if (!p.valid) {
            model.addAttribute("error", p.error);
            return "expired";
        }
        ChecklistInstance inst = instanceMapper.selectById(p.instanceId);
        if (inst == null || !inst.getPatientId().equals(p.patientId)) {
            model.addAttribute("error", "链接对应的记录不存在");
            return "expired";
        }
        ChecklistTemplate tpl = templateMapper.selectById(inst.getTemplateId());
        Patient patient = patientMapper.selectById(inst.getPatientId());
        List<ChecklistItem> items = itemMapper.selectList(
            new LambdaQueryWrapper<ChecklistItem>()
                .eq(ChecklistItem::getTemplateId, inst.getTemplateId())
                .eq(ChecklistItem::getDeleted, 0)
                .orderByAsc(ChecklistItem::getOrderNo));
        List<ChecklistItemRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<ChecklistItemRecord>()
                .eq(ChecklistItemRecord::getInstanceId, inst.getId()));
        Map<Long, Map<String, Object>> existing = new HashMap<>();
        for (ChecklistItemRecord r : records) {
            if (r.getValueJson() != null) {
                existing.put(r.getItemId(), JSONUtil.toBean(r.getValueJson(), Map.class));
            }
        }

        Map<String, List<ItemView>> grouped = new LinkedHashMap<>();
        for (ChecklistItem it : items) {
            String section = it.getSection() == null ? "记录" : it.getSection();
            grouped.computeIfAbsent(section, k -> new ArrayList<>())
                .add(new ItemView(it, existing.get(it.getId())));
        }

        model.addAttribute("token", token);
        model.addAttribute("instance", inst);
        model.addAttribute("template", tpl);
        model.addAttribute("patient", patient);
        model.addAttribute("grouped", grouped);
        return "checklist";
    }

    @PostMapping("/c/{token}/submit")
    public String submit(@PathVariable String token,
                          @RequestParam Map<String, String> form,
                          Model model) {
        TokenSigner.Parsed p = tokenSigner.verify(token);
        if (!p.valid) {
            model.addAttribute("error", p.error);
            return "expired";
        }

        Map<String, Map<String, Object>> values = new HashMap<>();
        for (Map.Entry<String, String> e : form.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (val == null || val.isBlank()) continue;

            int idx = key.indexOf("__");
            if (idx < 0) continue;
            String itemCode = key.substring(0, idx);
            String field = key.substring(idx + 2);
            values.computeIfAbsent(itemCode, k -> new HashMap<>()).put(field, val);
        }

        String recordedBy = form.getOrDefault("_recordedBy", "family");
        ChecklistSubmitService.SubmitOutcome outcome =
            submitService.submit(p.instanceId, values, recordedBy);

        model.addAttribute("outcome", outcome);
        model.addAttribute("token", token);
        return "done";
    }
}
