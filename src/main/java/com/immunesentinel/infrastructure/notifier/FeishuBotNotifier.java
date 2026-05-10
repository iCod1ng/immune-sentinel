package com.immunesentinel.infrastructure.notifier;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.immunesentinel.domain.notify.NotifyChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 飞书自定义机器人 webhook 实现。
 * 文档：https://open.feishu.cn/document/client-docs/bot-v1/add-custom-bot
 * 签名校验（可选）：若群机器人开启了"签名校验"，channel.secret 即为密钥。
 */
@Slf4j
@Component
public class FeishuBotNotifier implements Notifier {

    public static final String TYPE = "feishu_bot";

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
            JSONObject body = buildInteractiveCard(msg);
            if (StrUtil.isNotBlank(channel.getSecret())) {
                long ts = System.currentTimeMillis() / 1000;
                body.set("timestamp", String.valueOf(ts));
                body.set("sign", sign(ts, channel.getSecret()));
            }

            HttpResponse resp = HttpRequest.post(channel.getWebhookUrl())
                .body(body.toString())
                .timeout(5000)
                .execute();

            if (!resp.isOk()) {
                return NotifyResult.fail("http " + resp.getStatus() + " " + resp.body());
            }
            JSONObject r = JSONUtil.parseObj(resp.body());
            int code = r.getInt("code", r.getInt("StatusCode", 0));
            if (code != 0) {
                return NotifyResult.fail("feishu err code=" + code + " msg=" + r.getStr("msg"));
            }
            return NotifyResult.ok();
        } catch (Exception e) {
            log.warn("feishu notify failed", e);
            return NotifyResult.fail(e.getMessage());
        }
    }

    private JSONObject buildInteractiveCard(NotifyMessage msg) {
        String color = switch (msg.getSeverity() == null ? NotifyMessage.Severity.INFO : msg.getSeverity()) {
            case EMERGENCY -> "red";
            case WARN -> "orange";
            default -> "blue";
        };

        JSONObject header = new JSONObject()
            .set("template", color)
            .set("title", new JSONObject()
                .set("tag", "plain_text")
                .set("content", StrUtil.nullToDefault(msg.getTitle(), "immune-sentinel 提醒")));

        List<JSONObject> elements = new ArrayList<>();
        if (StrUtil.isNotBlank(msg.getContent())) {
            elements.add(new JSONObject()
                .set("tag", "div")
                .set("text", new JSONObject()
                    .set("tag", "lark_md")
                    .set("content", msg.getContent())));
        }

        if (msg.getActions() != null && !msg.getActions().isEmpty()) {
            List<JSONObject> actions = new ArrayList<>();
            for (NotifyMessage.Action a : msg.getActions()) {
                actions.add(new JSONObject()
                    .set("tag", "button")
                    .set("text", new JSONObject()
                        .set("tag", "plain_text")
                        .set("content", a.getLabel()))
                    .set("type", "primary")
                    .set("url", a.getUrl()));
            }
            elements.add(new JSONObject()
                .set("tag", "action")
                .set("actions", actions));
        }

        return new JSONObject()
            .set("msg_type", "interactive")
            .set("card", new JSONObject()
                .set("config", new JSONObject().set("wide_screen_mode", true))
                .set("header", header)
                .set("elements", elements));
    }

    private String sign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[]{});
        return Base64.encode(signData);
    }
}
