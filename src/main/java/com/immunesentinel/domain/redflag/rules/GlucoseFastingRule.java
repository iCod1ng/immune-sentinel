package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GlucoseFastingRule implements RedFlagRule {
    public static final String CODE = "GLUCOSE_FASTING_HIGH";
    private static final BigDecimal DKA_THRESHOLD = new BigDecimal("13.9");
    private static final BigDecimal LOW_THRESHOLD = new BigDecimal("3.9");

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        BigDecimal g = ctx.num();
        if (g == null) return RedFlagHit.miss();
        if (g.compareTo(DKA_THRESHOLD) > 0) {
            return RedFlagHit.builder()
                .ruleCode(CODE).severity(RedFlagHit.Severity.EMERGENCY)
                .title("空腹血糖 " + g + " mmol/L，警惕 DKA")
                .detail("立即测尿酮；尿酮 ≥ ++ 直接急诊。PD-1 可诱发自身免疫性糖尿病，发病急。")
                .build();
        }
        if (g.compareTo(LOW_THRESHOLD) < 0) {
            return RedFlagHit.builder()
                .ruleCode(CODE).severity(RedFlagHit.Severity.WARN)
                .title("空腹血糖偏低 " + g + " mmol/L")
                .detail("立即进食含糖食物；调整降糖方案请联系医生。")
                .build();
        }
        return RedFlagHit.miss();
    }
}
