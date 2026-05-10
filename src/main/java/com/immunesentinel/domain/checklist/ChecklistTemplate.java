package com.immunesentinel.domain.checklist;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checklist_template")
public class ChecklistTemplate {
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private String triggerType;
    private String triggerCron;
    private String defaultDueTime;
    private String applicableTags;
    private String reminderCron;
    private String description;
    private Integer enabled;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
