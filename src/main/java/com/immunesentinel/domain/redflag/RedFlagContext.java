package com.immunesentinel.domain.redflag;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 评估上下文：本次 item 的结构化取值 + 可选的辅助数据（如历史 7 天体温、上周腿围）。
 */
@Data
@Builder
public class RedFlagContext {
    private Long tenantId;
    private Long patientId;
    private Long sourceItemRecordId;
    private String itemCode;
    /** 结构化值：{"num":36.7} / {"sys":130,"dia":80} / {"checked":true} */
    private Map<String, Object> value;
    /** 辅助数据，规则按需取；如 leg_last_week -> BigDecimal */
    private Map<String, Object> aux;

    public BigDecimal num() {
        Object n = value == null ? null : value.get("num");
        return n == null ? null : new BigDecimal(n.toString());
    }

    public Boolean checked() {
        Object c = value == null ? null : value.get("checked");
        return c == null ? null : Boolean.valueOf(c.toString());
    }

    public Integer intOf(String k) {
        Object v = value == null ? null : value.get(k);
        return v == null ? null : Integer.valueOf(v.toString());
    }

    public String text() {
        Object t = value == null ? null : value.get("text");
        return t == null ? null : t.toString();
    }
}
