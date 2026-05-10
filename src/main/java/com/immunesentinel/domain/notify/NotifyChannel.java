package com.immunesentinel.domain.notify;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify_channel")
public class NotifyChannel {
    private Long id;
    private Long tenantId;
    private Long patientId;
    private String channelType;
    private String displayName;
    private String webhookUrl;
    private String secret;
    private String extraJson;
    private Integer enabled;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
