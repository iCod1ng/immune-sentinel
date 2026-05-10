package com.immunesentinel.domain.patient;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("patient")
public class Patient {
    private Long id;
    private Long tenantId;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String tags;
    private LocalDate therapyStart;
    private String notes;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
