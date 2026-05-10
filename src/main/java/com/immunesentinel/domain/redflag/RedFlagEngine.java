package com.immunesentinel.domain.redflag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.immunesentinel.domain.redflag.rules.CheckedFlagRule;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险预警规则注册表 + 评估入口。
 * 优先按 ruleCode 精确匹配；未找到则落到 CheckedFlagRule 通用逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedFlagEngine {

    private final List<RedFlagRule> rules;
    private final CheckedFlagRule checkedFallback;

    private Map<String, RedFlagRule> registry;

    public RedFlagHit evaluate(String ruleCode, RedFlagContext ctx) {
        if (ruleCode == null) return RedFlagHit.miss();
        Map<String, RedFlagRule> reg = registry();
        RedFlagRule r = reg.get(ruleCode);
        if (r != null) {
            try {
                RedFlagHit h = r.evaluate(ctx);
                return h == null ? RedFlagHit.miss() : h;
            } catch (Exception e) {
                log.warn("rule {} failed", ruleCode, e);
                return RedFlagHit.miss();
            }
        }
        return checkedFallback.evaluateAs(ruleCode, ctx);
    }

    private synchronized Map<String, RedFlagRule> registry() {
        if (registry == null) {
            Map<String, RedFlagRule> m = new HashMap<>();
            for (RedFlagRule r : rules) {
                m.put(r.code(), r);
            }
            registry = m;
        }
        return registry;
    }
}
