package com.immunesentinel.domain.checklist;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checklist_item")
public class ChecklistItem {
    private Long id;
    private Long templateId;
    private String code;
    private String label;
    private String section;
    private String inputType;
    private String unit;
    private String normalRange;
    private String redFlagRule;
    private Integer required;
    private Integer orderNo;
    private String hint;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
