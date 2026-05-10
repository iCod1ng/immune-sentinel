package com.immunesentinel.infrastructure.notifier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotifyResult {
    private boolean success;
    private String errorMsg;

    public static NotifyResult ok() {
        return NotifyResult.builder().success(true).build();
    }

    public static NotifyResult fail(String err) {
        return NotifyResult.builder().success(false).errorMsg(err).build();
    }
}
