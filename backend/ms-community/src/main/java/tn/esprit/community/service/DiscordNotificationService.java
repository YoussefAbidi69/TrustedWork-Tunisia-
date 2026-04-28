package tn.esprit.community.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DiscordNotificationService {

    private final String webhookUrl;
    private final String webhookSecret;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(DiscordNotificationService.class);

    public DiscordNotificationService(
            @Value("${app.discord.webhook-url:http://localhost:3000/webhook}") String webhookUrl,
            @Value("${app.discord.webhook-secret:your_super_secret_webhook_token}") String webhookSecret) {
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
        this.restTemplate = new RestTemplate();
    }

    @Async
    public void notifyPostPublished(Object postResponse) {
        sendWebhook("post.published", postResponse);
    }

    @Async
    public void notifyCoursePublished(Object courseResponse) {
        sendWebhook("course.published", courseResponse);
    }

    private void sendWebhook(String event, Object payloadObj) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + webhookSecret);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("event", event);
            body.put("payload", payloadObj);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            logger.info("✅ Successfully notified Discord bot for event: {}", event);
        } catch (Exception e) {
            logger.error("❌ Failed to notify Discord bot: {}", e.getMessage(), e);
        }
    }
}
