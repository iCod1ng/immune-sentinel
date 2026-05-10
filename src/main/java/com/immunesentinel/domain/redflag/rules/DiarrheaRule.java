package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

/**
 * 每日大便次数 ≥ 7 或带血 → 24h 联系（免疫性结肠炎）。
 */
@Component
public class DiarrheaRule implements RedFlagRule {
    public static final String CODE = "DIARRHEA_HIGH";

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        Integer n = ctx.num() == null ? null : ctx.num().intValue();
        if (n == null) return RedFlagHit.miss();
        if (n >= 7) {
            return RedFlagHit.builder()
                .ruleCode(CODE).severity(RedFlagHit.Severity.H24)
                .title("大便次数 " + n + " 次 / 天")
                .detail("24h 内联系肿瘤科，警惕免疫性结肠炎；若带血 / 脱水 / 腹痛立即就医。")
                .build();
        }
        return RedFlagHit.miss();
    }
}
