package com.immunesentinel.domain.vital;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leg_measurement")
public class LegMeasurement {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private String site;
    private BigDecimal circumferenceCm;
    private String photoUrls;
    private BigDecimal deltaVsLastWeek;
    private BigDecimal deltaVs3days;
    private LocalDateTime measuredAt;
    private String note;
    private LocalDateTime createdAt;
}
