package com.immunesentinel.application.checklist;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import com.immunesentinel.config.SentinelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * 一次性签名 token，嵌在 H5 链接里。
 * 格式：base64url(patientId.instanceId.expireEpochSec).hmacHex
 * 不追求加密，只防篡改；48h 过期。
 */
@Component
@RequiredArgsConstructor
public class TokenSigner {

    private final SentinelProperties props;

    public String sign(long patientId, long instanceId) {
        long expire = Instant.now().getEpochSecond() + props.getTokenTtlHours() * 3600L;
        String payload = patientId + "." + instanceId + "." + expire;
        String payloadB64 = Base64.encodeUrlSafe(payload.getBytes(StandardCharsets.UTF_8));
        String sig = hmac(payloadB64);
        return payloadB64 + "." + sig;
    }

    public Parsed verify(String token) {
        if (token == null || !token.contains(".")) return Parsed.invalid("token 格式错误");
        int idx = token.lastIndexOf('.');
        String payloadB64 = token.substring(0, idx);
        String sig = token.substring(idx + 1);
        String expected = hmac(payloadB64);
        if (!expected.equals(sig)) return Parsed.invalid("签名校验失败");
        String payload = new String(Base64.decode(payloadB64), StandardCharsets.UTF_8);
        String[] parts = payload.split("\\.");
        if (parts.length != 3) return Parsed.invalid("payload 格式错误");
        long expire = Long.parseLong(parts[2]);
        if (Instant.now().getEpochSecond() > expire) return Parsed.invalid("链接已过期，请联系家属重新获取");
        Parsed p = new Parsed();
        p.valid = true;
        p.patientId = Long.parseLong(parts[0]);
        p.instanceId = Long.parseLong(parts[1]);
        return p;
    }

    private String hmac(String payloadB64) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexUtil.encodeHexStr(mac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Parsed {
        public boolean valid;
        public long patientId;
        public long instanceId;
        public String error;

        public static Parsed invalid(String err) {
            Parsed p = new Parsed();
            p.valid = false;
            p.error = err;
            return p;
        }
    }
}
