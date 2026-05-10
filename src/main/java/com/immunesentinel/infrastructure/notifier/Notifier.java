package com.immunesentinel.infrastructure.notifier;

import com.immunesentinel.domain.notify.NotifyChannel;

public interface Notifier {
    String channelType();

    NotifyResult send(NotifyChannel channel, NotifyMessage msg);
}
