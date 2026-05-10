package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 左腿周长：
 *   aux.lastWeek -> BigDecimal 上周同部位 → 差 ≥ 2cm
 *   aux.days3    -> BigDecimal 3 天前同部位 → 差 ≥ 1cm
 * 任一命中即警惕 DVT。禁止按摩、禁止热敷；立即就医做下肢血管彩超。
 */
@Component
public class LegCircIncreaseRule implements RedFlagRule {
    public static final String CODE = "LEG_CIRC_INCREASE";
    private static final BigDecimal WEEK_DELTA = new BigDecimal("2.0");
    private static final BigDecimal D3_DELTA = new BigDecimal("1.0");

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        BigDecimal now = ctx.num();
        if (now == null || ctx.getAux() == null) return RedFlagHit.miss();

        BigDecimal lastWeek = toBd(ctx.getAux().get("lastWeek"));
        BigDecimal d3 = toBd(ctx.getAux().get("days3"));

        if (lastWeek != null && now.subtract(lastWeek).compareTo(WEEK_DELTA) >= 0) {
            return hit("较上周增 " + now.subtract(lastWeek) + " cm");
        }
        if (d3 != null && now.subtract(d3).compareTo(D3_DELTA) >= 0) {
            return hit("3 日内增 " + now.subtract(d3) + " cm");
        }
        return RedFlagHit.miss();
    }

    private RedFlagHit hit(String delta) {
        return RedFlagHit.builder()
            .ruleCode(CODE).severity(RedFlagHit.Severity.EMERGENCY)
            .title("左腿围度异常增大（" + delta + "）— 警惕 DVT")
            .detail("立即就医做下肢血管彩超。**不按摩、不热敷、不自驾**；叫 120 或家属送。")
            .build();
    }

    private BigDecimal toBd(Object o) {
        return o == null ? null : new BigDecimal(o.toString());
    }
}
