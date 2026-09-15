package co.id.skyworx.slikideb.service;

import co.id.skyworx.slikideb.dto.response.WsNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for real-time WebSocket notifications via STOMP broker.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendProgress(String requestId, String step) {
        WsNotification notification = WsNotification.builder()
                .status("PROCESSING")
                .requestId(requestId)
                .step(step)
                .message("Processing step: " + step)
                .build();
        sendToRequestTopic(requestId, notification);
        log.debug("Sent progress update: requestId={}, step={}", requestId, step);
    }

    public void sendSuccess(String requestId, Long reportId) {
        WsNotification notification = WsNotification.builder()
                .status("SUCCESS")
                .requestId(requestId)
                .message("SLIK PDF successfully generated")
                .url("http://localhost:8080/api/ideb/report/" + reportId + "/download")
                .build();
        sendToRequestTopic(requestId, notification);
        log.info("Sent success notification: requestId={}, reportId={}", requestId, reportId);
    }

    public void sendFailure(String requestId, String errorCode, String message) {
        WsNotification notification = WsNotification.builder()
                .status("FAILED")
                .requestId(requestId)
                .errorCode(errorCode)
                .message(message)
                .build();
        sendToRequestTopic(requestId, notification);
        log.warn("Sent failure notification: requestId={}, errorCode={}", requestId, errorCode);
    }

    private void sendToRequestTopic(String requestId, WsNotification notification) {
        String topic = "/topic/ideb/" + requestId;
        messagingTemplate.convertAndSend(topic, notification);
    }

    private void sendBroadcast(WsNotification notification) {
        messagingTemplate.convertAndSend("/topic/ideb/notifications", notification);
    }
}
