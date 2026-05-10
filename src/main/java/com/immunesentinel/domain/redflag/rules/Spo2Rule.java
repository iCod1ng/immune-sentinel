package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Spo2Rule implements RedFlagRule {
    public static final String CODE = "SPO2_LOW";
    private static final BigDecimal EMERGENCY = new BigDecimal("92");
    private static final BigDecimal WARN = new BigDecimal("94");

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        BigDecimal v = ctx.num();
        if (v == null) return RedFlagHit.miss();
        if (v.compareTo(EMERGENCY) < 0) {
            return RedFlagHit.builder()
                .ruleCode(CODE).severity(RedFlagHit.Severity.EMERGENCY)
                .title("血氧 " + v + "% 偏低")
                .detail("立即就医；若伴随干咳/气短警惕免疫性肺炎 / 肺栓塞。")
                .build();
        }
        if (v.compareTo(WARN) < 0) {
            return RedFlagHit.builder()
                .ruleCode(CODE).severity(RedFlagHit.Severity.H24)
                .title("血氧 " + v + "% 略低于 94%")
                .detail("24h 内复测；若持续偏低联系肿瘤科。")
                .build();
        }
        return RedFlagHit.miss();
    }
}
