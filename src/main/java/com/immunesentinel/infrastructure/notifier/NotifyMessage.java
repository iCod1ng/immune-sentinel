package com.immunesentinel.infrastructure.notifier;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotifyMessage {
    private String category;
    private Severity severity;
    private String title;
    private String content;
    private List<Action> actions;

    public enum Severity {
        INFO, WARN, EMERGENCY
    }

    @Data
    @Builder
    public static class Action {
        private String label;
        private String url;
    }
}
