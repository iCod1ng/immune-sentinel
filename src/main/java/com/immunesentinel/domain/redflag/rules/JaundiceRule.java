package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

/**
 * 黄疸 / 眼白发黄 / 尿色深茶 → 24h 查肝功（免疫性肝炎）。
 */
@Component
public class JaundiceRule implements RedFlagRule {
    public static final String CODE = "JAUNDICE";

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        Boolean checked = ctx.checked();
        String text = ctx.text();
        boolean hit = Boolean.TRUE.equals(checked)
            || (text != null && (text.contains("黄") || text.contains("深茶")));
        if (!hit) return RedFlagHit.miss();
        return RedFlagHit.builder()
            .ruleCode(CODE).severity(RedFlagHit.Severity.H24)
            .title("黄疸 / 深茶色尿")
            .detail("24h 内查肝功 + 胆红素；免疫性肝炎是 PD-1 常见严重 irAE。避免所有中药 / 保健品。")
            .build();
    }
}
