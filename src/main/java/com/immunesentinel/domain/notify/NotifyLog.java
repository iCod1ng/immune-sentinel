package com.immunesentinel.domain.notify;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify_log")
public class NotifyLog {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private Long channelId;
    private String channelType;
    private String category;
    private String title;
    private String content;
    private Integer success;
    private String errorMsg;
    private LocalDateTime sentAt;
}
