package com.immunesentinel.domain.redflag;

public interface RedFlagRule {
    String code();
    RedFlagHit evaluate(RedFlagContext ctx);
}
