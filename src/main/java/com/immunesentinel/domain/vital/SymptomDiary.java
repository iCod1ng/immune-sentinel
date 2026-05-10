package com.immunesentinel.domain.vital;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("symptom_diary")
public class SymptomDiary {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private LocalDate diaryDate;
    private String structured;
    private String freeText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
