package com.campusbuddies.messaging;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.security.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MessageController {
    public record TicketView(String ticket, int expiresInSeconds) {}
    public record SendInput(@NotBlank @Size(max = 64) String clientMessageId,
                            @NotNull MessageType messageType,
                            @Size(max = 1000) String content,
                            Long fileId) {}

    private final WsTicketStore tickets;
    private final MessageService messages;

    public MessageController(WsTicketStore tickets, MessageService messages) {
        this.tickets = tickets;
        this.messages = messages;
    }

    @PostMapping("/ws-ticket")
    public ApiResponse<TicketView> ticket() {
        var principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        return ApiResponse.ok(new TicketView(tickets.issue(principal.userId(), principal.tokenVersion()), 60));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<MessageService.ConversationView>> conversations() {
        return ApiResponse.ok(messages.conversations());
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageService.MessageView>> history(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") @Min(0) long afterId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return ApiResponse.ok(messages.history(id, afterId, limit));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<MessageService.MessageView> send(@PathVariable long id,
                                                         @Valid @RequestBody SendInput input) {
        return ApiResponse.ok(messages.sendCurrent(id, input.clientMessageId(), input.messageType(),
                input.content(), input.fileId()));
    }
}
