package com.immunesentinel.domain.checklist;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checklist_item_record")
public class ChecklistItemRecord {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private Long instanceId;
    private Long itemId;
    private String itemCode;
    private String valueJson;
    private Integer isAbnormal;
    private String note;
    private LocalDateTime recordedAt;
    private String recordedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
