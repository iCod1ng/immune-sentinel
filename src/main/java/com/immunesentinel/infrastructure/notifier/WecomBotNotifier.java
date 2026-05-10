package com.immunesentinel.infrastructure.notifier;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.immunesentinel.domain.notify.NotifyChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企业微信群机器人 webhook 实现。
 * 文档：https://developer.work.weixin.qq.com/document/path/91770
 */
@Slf4j
@Component
public class WecomBotNotifier implements Notifier {

    public static final String TYPE = "wecom_bot";

    @Override
    public String channelType() {
        return TYPE;
    }

    @Override
    public NotifyResult send(NotifyChannel channel, NotifyMessage msg) {
        if (channel.getWebhookUrl() == null || channel.getWebhookUrl().startsWith("REPLACE_")) {
            return NotifyResult.fail("webhook url 未配置");
        }
        try {
            StringBuilder md = new StringBuilder();
            String title = StrUtil.nullToDefault(msg.getTitle(), "immune-sentinel 提醒");
            String prefix = switch (msg.getSeverity() == null ? NotifyMessage.Severity.INFO : msg.getSeverity()) {
                case EMERGENCY -> "<font color=\"warning\">【紧急】</font>";
                case WARN -> "<font color=\"comment\">【注意】</font>";
                default -> "";
            };
            md.append("### ").append(prefix).append(title).append("\n");
            if (StrUtil.isNotBlank(msg.getContent())) {
                md.append(msg.getContent()).append("\n");
            }
            if (msg.getActions() != null) {
                for (NotifyMessage.Action a : msg.getActions()) {
                    md.append("\n[").append(a.getLabel()).append("](").append(a.getUrl()).append(")");
                }
            }

            JSONObject body = new JSONObject()
                .set("msgtype", "markdown")
                .set("markdown", new JSONObject().set("content", md.toString()));

            HttpResponse resp = HttpRequest.post(channel.getWebhookUrl())
                .body(body.toString())
                .timeout(5000)
                .execute();

            if (!resp.isOk()) {
                return NotifyResult.fail("http " + resp.getStatus() + " " + resp.body());
            }
            JSONObject r = JSONUtil.parseObj(resp.body());
            int errcode = r.getInt("errcode", 0);
            if (errcode != 0) {
                return NotifyResult.fail("wecom err " + errcode + " " + r.getStr("errmsg"));
            }
            return NotifyResult.ok();
        } catch (Exception e) {
            log.warn("wecom notify failed", e);
            return NotifyResult.fail(e.getMessage());
        }
    }
}
