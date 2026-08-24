package com.giapha.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BrowserLauncher {

    @Value("${server.port:8080}")
    private String port;

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        String url = "http://localhost:" + port;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", url });
                log.info("Đã tự động mở trình duyệt tại: {}", url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[] { "open", url });
                log.info("Đã tự động mở trình duyệt tại: {}", url);
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[] { "xdg-open", url });
                log.info("Đã tự động mở trình duyệt tại: {}", url);
            }
        } catch (Exception e) {
            log.error("Không thể tự động mở trình duyệt. Vui lòng truy cập thủ công: {}", url, e);
        }
    }
}
