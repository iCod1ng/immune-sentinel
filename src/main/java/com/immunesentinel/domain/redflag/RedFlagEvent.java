package com.immunesentinel.domain.redflag;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("red_flag_event")
public class RedFlagEvent {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private String ruleCode;
    private String severity;
    private String title;
    private String detail;
    private String triggeredBy;
    private Long sourceRef;
    private LocalDateTime triggeredAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
