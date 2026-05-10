package com.immunesentinel.domain.vital;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("vital_sign_log")
public class VitalSignLog {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private String metricCode;
    private BigDecimal valueNum;
    private String valueText;
    private String unit;
    private LocalDateTime measuredAt;
    private String source;
    private Long sourceRef;
    private LocalDateTime createdAt;
}
