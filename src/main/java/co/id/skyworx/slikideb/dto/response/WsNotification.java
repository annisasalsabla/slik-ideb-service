package co.id.skyworx.slikideb.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket notification payload broadcast via STOMP topics.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsNotification {

    private String status;
    private String requestId;
    private String message;
    private String url;
    private String step;
    private String errorCode;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
