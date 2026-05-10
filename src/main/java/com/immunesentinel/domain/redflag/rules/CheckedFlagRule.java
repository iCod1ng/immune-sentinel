package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

/**
 * 一类通用"勾选即风险预警"的规则基类，通过 code 区分。
 * 在 red_flag_rule 字段直接指定对应 code 即可复用。
 */
@Component
public class CheckedFlagRule implements RedFlagRule {
    public static final String[] CODES = {
        "RASH_SEVERE", "STOOL_BLOOD", "COUGH_DYSPNEA", "CHEST_PAIN",
        "HEADACHE_SEVERE", "UNUSUAL_FATIGUE", "LEG_TEMP_DIFF", "LEG_PITTING",
        "LEG_NEW_PAIN", "GRAFT_ABNORMAL", "PULSE_IRREGULAR", "URINE_DARK",
        "INFUSION_REACTION"
    };

    @Override public String code() { return "CHECKED_FLAG"; }

    public RedFlagHit evaluateAs(String ruleCode, RedFlagContext ctx) {
        if (!Boolean.TRUE.equals(ctx.checked())) return RedFlagHit.miss();

        return switch (ruleCode) {
            case "RASH_SEVERE" -> emer(ruleCode, "严重皮疹", "若伴黏膜糜烂警惕 SJS/TEN，立即急诊。否则 24h 联系医生。");
            case "STOOL_BLOOD" -> emer(ruleCode, "便血", "24h 联系肿瘤科；若量大 / 头晕立即急诊。");
            case "COUGH_DYSPNEA" -> h24(ruleCode, "咳嗽 / 气短", "进行性气短 + 干咳 + SpO₂ < 92% → 立即就医（免疫性肺炎）。");
            case "CHEST_PAIN" -> emer(ruleCode, "胸痛 / 心悸", "拨 120；警惕肺栓塞 / 免疫性心肌炎。");
            case "HEADACHE_SEVERE" -> h24(ruleCode, "严重头痛", "若伴视野缺损立即急诊（垂体炎）。");
            case "UNUSUAL_FATIGUE" -> h24(ruleCode, "异常乏力", "伴低血压 + 呕吐 → 肾上腺危象急诊。");
            case "LEG_TEMP_DIFF" -> h24(ruleCode, "左小腿皮温异常偏高", "警惕 DVT；立即就医做血管彩超，**不按摩不热敷**。");
            case "LEG_PITTING" -> h24(ruleCode, "凹陷性水肿", "警惕 DVT / 心功能问题；做血管彩超。");
            case "LEG_NEW_PAIN" -> h24(ruleCode, "左腿新发疼痛 / 紧绷", "警惕 DVT；做血管彩超。");
            case "GRAFT_ABNORMAL" -> h24(ruleCode, "植皮区异常", "联系整形外科；照片留档。");
            case "PULSE_IRREGULAR" -> h24(ruleCode, "脉搏不规整", "警惕新发房颤；联系心内科做心电图。");
            case "URINE_DARK" -> h24(ruleCode, "深色尿", "24h 查肝功 + 胆红素。");
            case "INFUSION_REACTION" -> emer(ruleCode, "输液反应", "立即按铃叫护士；记录症状。");
            default -> RedFlagHit.miss();
        };
    }

    private static RedFlagHit emer(String c, String t, String d) {
        return RedFlagHit.builder().ruleCode(c).severity(RedFlagHit.Severity.EMERGENCY).title(t).detail(d).build();
    }

    private static RedFlagHit h24(String c, String t, String d) {
        return RedFlagHit.builder().ruleCode(c).severity(RedFlagHit.Severity.H24).title(t).detail(d).build();
    }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        return RedFlagHit.miss();
    }
}
