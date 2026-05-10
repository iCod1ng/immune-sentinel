package com.immunesentinel.domain.redflag.rules;

import com.immunesentinel.domain.redflag.RedFlagContext;
import com.immunesentinel.domain.redflag.RedFlagHit;
import com.immunesentinel.domain.redflag.RedFlagRule;
import org.springframework.stereotype.Component;

/**
 * 血压：收缩压 >=180 或 <=90；或舒张压 >=110 或 <=60 → WARN；结合头痛/乏力另行升级。
 */
@Component
public class BloodPressureRule implements RedFlagRule {
    public static final String CODE = "BP_OUT_OF_RANGE";

    @Override public String code() { return CODE; }

    @Override
    public RedFlagHit evaluate(RedFlagContext ctx) {
        Integer sys = ctx.intOf("sys");
        Integer dia = ctx.intOf("dia");
        if (sys == null || dia == null) return RedFlagHit.miss();

        if (sys >= 180 || dia >= 110) {
            return RedFlagHit.builder()
                .ruleCode(CODE).severity(RedFlagHit.Severity.H24)
                .title("血压偏高（" + sys + "/" + dia + " mmHg）")
                .detail("24h 内联系医生；若伴随剧烈头痛/视物重影/一侧无力请立即拨 120。")
                .build();
        }
        if (sys <= 90 || dia <= 60) {
            return RedFlagHit.builder()
                .ruleCode(CODE).severity(RedFlagHit.Severity.H24)
                .title("血压偏低（" + sys + "/" + dia + " mmHg）")
                .detail("联系医生；若伴随严重乏力 + 呕吐警惕肾上腺危象。")
                .build();
        }
        return RedFlagHit.miss();
    }
}
