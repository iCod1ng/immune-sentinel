package com.immunesentinel.domain.checklist;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("checklist_instance")
public class ChecklistInstance {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private Long templateId;
    private String templateCode;
    private LocalDate dueDate;
    private LocalDateTime dueAt;
    private String status;
    private LocalDateTime completedAt;
    private Integer abnormalCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
