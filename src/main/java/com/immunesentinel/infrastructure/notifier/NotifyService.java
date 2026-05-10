package com.immunesentinel.infrastructure.notifier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.immunesentinel.domain.notify.NotifyChannel;
import com.immunesentinel.domain.notify.NotifyLog;
import com.immunesentinel.infrastructure.persistence.mapper.NotifyChannelMapper;
import com.immunesentinel.infrastructure.persistence.mapper.NotifyLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扇出到患者配置的所有启用通道，落推送日志。
 * 故意不做重试队列，单机 MVP 够用；失败就记录，下次提醒仍会触发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotifyChannelMapper channelMapper;
    private final NotifyLogMapper logMapper;
    private final List<Notifier> notifiers;

    private Map<String, Notifier> registry;

    public void dispatch(Long patientId, NotifyMessage msg) {
        Map<String, Notifier> reg = registry();
        List<NotifyChannel> channels = channelMapper.selectList(
            new LambdaQueryWrapper<NotifyChannel>()
                .eq(NotifyChannel::getPatientId, patientId)
                .eq(NotifyChannel::getEnabled, 1)
                .eq(NotifyChannel::getDeleted, 0));

        if (channels.isEmpty()) {
            log.warn("patient {} has no enabled channel, msg={}", patientId, msg.getTitle());
            return;
        }

        for (NotifyChannel ch : channels) {
            Notifier n = reg.get(ch.getChannelType());
            NotifyResult r;
            if (n == null) {
                r = NotifyResult.fail("no notifier for " + ch.getChannelType());
            } else {
                r = n.send(ch, msg);
            }
            saveLog(ch, msg, r);
        }
    }

    private synchronized Map<String, Notifier> registry() {
        if (registry == null) {
            Map<String, Notifier> m = new HashMap<>();
            for (Notifier n : notifiers) {
                m.put(n.channelType(), n);
            }
            registry = m;
        }
        return registry;
    }

    private void saveLog(NotifyChannel ch, NotifyMessage msg, NotifyResult r) {
        NotifyLog log = new NotifyLog();
        log.setTenantId(ch.getTenantId());
        log.setPatientId(ch.getPatientId());
        log.setChannelId(ch.getId());
        log.setChannelType(ch.getChannelType());
        log.setCategory(msg.getCategory());
        log.setTitle(msg.getTitle());
        log.setContent(msg.getContent());
        log.setSuccess(r.isSuccess() ? 1 : 0);
        log.setErrorMsg(r.getErrorMsg());
        log.setSentAt(LocalDateTime.now());
        logMapper.insert(log);
    }
}
