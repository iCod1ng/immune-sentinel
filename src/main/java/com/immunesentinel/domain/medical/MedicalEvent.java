package com.immunesentinel.domain.medical;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_event")
public class MedicalEvent {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private String eventType;
    private String title;
    private LocalDateTime scheduledAt;
    private LocalDateTime executedAt;
    private String status;
    private String note;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
