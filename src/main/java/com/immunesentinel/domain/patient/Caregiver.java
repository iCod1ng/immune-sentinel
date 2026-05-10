package com.immunesentinel.domain.patient;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("caregiver")
public class Caregiver {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private String name;
    private String relation;
    private String phone;
    private Integer isPrimary;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
