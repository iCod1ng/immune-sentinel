package com.immunesentinel.domain.redflag;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 规则命中结果。severity: EMERGENCY(立即就医/拨120) / H24(24h 联系医生) / WARN(一般警示) / NONE.
 */
@Data
@Builder
public class RedFlagHit {
    public enum Severity { NONE, WARN, H24, EMERGENCY }

    private String ruleCode;
    private Severity severity;
    private String title;
    private String detail;
    private Map<String, Object> context;

    public boolean isHit() {
        return severity != null && severity != Severity.NONE;
    }

    public static RedFlagHit miss() {
        return RedFlagHit.builder().severity(Severity.NONE).build();
    }

    public String severityLabel() {
        return switch (severity) {
            case EMERGENCY -> "立即就医 / 拨 120";
            case H24 -> "24 小时内联系医生";
            case WARN -> "注意观察";
            default -> "";
        };
    }
}
