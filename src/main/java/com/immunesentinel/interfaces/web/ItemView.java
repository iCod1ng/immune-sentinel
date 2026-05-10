package com.immunesentinel.interfaces.web;

import com.immunesentinel.domain.checklist.ChecklistItem;
import lombok.Getter;

import java.util.Map;

@Getter
public class ItemView {
    private final ChecklistItem item;
    private final Map<String, Object> existing;

    public ItemView(ChecklistItem item, Map<String, Object> existing) {
        this.item = item;
        this.existing = existing;
    }

    public Object v(String key) {
        return existing == null ? null : existing.get(key);
    }
}
