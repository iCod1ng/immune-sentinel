package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BodyTempRule implements RedFlagRule {
    public static final String CODE = "BODY_TEMP_OVER_38";
    private static final BigDecimal THRESHOLD = new BigDecimal("38.0");

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        BigDecimal t = ctx.num();
        if (t == null) return RedFlagHit.miss();
        if (t.compareTo(THRESHOLD) >= 0) {
            return RedFlagHit.builder()
                .ruleCode(CODE)
                .severity(RedFlagHit.Severity.H24)
                .title("发热 ≥ 38℃")
                .detail("PD-1 治疗中发热需 24h 联系肿瘤科，排除免疫性炎症 / 感染。记录起始时间、是否伴随咳嗽气短、腹泻、皮疹。")
                .build();
        }
        return RedFlagHit.miss();
    }
}
