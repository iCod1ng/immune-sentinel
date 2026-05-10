package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * FAST 任一异常 → 拨 120。
 * 期望 value 结构：{"face":"abnormal","arm":"ok","speech":"ok"} 任一值为 abnormal 即命中。
 */
@Component
public class FastAnyAbnormalRule implements RedFlagRule {
    public static final String CODE = "FAST_ANY_ABNORMAL";

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        if (ctx.getValue() == null) return RedFlagHit.miss();
        for (Object v : ctx.getValue().values()) {
            if (v != null && "abnormal".equalsIgnoreCase(v.toString())) {
                return RedFlagHit.builder()
                    .ruleCode(CODE)
                    .severity(RedFlagHit.Severity.EMERGENCY)
                    .title("疑似脑梗 FAST 阳性")
                    .detail("嘴角/单手/言语任一异常 → 立即拨 120，记录症状起始时间。家属取应急包；患者平躺头偏一侧；不喂水不喂药。")
                    .build();
            }
        }
        return RedFlagHit.miss();
    }
}
