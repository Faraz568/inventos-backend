package com.inventos.controller;

import com.inventos.dto.response.ApiResponse;
import com.inventos.entity.ChatMessage;
import com.inventos.entity.User;
import com.inventos.exception.ResourceNotFoundException;
import com.inventos.repository.ChatMessageRepository;
import com.inventos.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatRepository;
    private final UserRepository        userRepository;

    // ── Group Chat ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getGroupMessages() {
        List<ChatMessage> msgs = chatRepository.findByRecipientUsernameIsNullOrderBySentAtAsc();
        return ResponseEntity.ok(ApiResponse.ok(msgs));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessage>> sendGroupMessage(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody SendRequest req) {
        User user = getUser(principal);
        ChatMessage msg = ChatMessage.builder()
            .senderUsername(user.getUsername())
            .senderRole(user.getRole().name())
            .recipientUsername(null)   // null = group
            .text(req.getText().trim())
            .sentAt(LocalDateTime.now())
            .build();
        return ResponseEntity.ok(ApiResponse.ok("Message sent", chatRepository.save(msg)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearGroupChat() {
        chatRepository.deleteAll();
        return ResponseEntity.ok(ApiResponse.ok("Chat cleared", null));
    }

    // ── Private / DM Chat ─────────────────────────────────────────

    /** GET /chat/dm/{username} — fetch DM conversation with a user */
    @GetMapping("/dm/{otherUsername}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getDmMessages(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String otherUsername) {
        String me = principal.getUsername();
        List<ChatMessage> msgs = chatRepository.findDmConversation(me, otherUsername);
        return ResponseEntity.ok(ApiResponse.ok(msgs));
    }

    /** POST /chat/dm — send a private message to another user */
    @PostMapping("/dm")
    public ResponseEntity<ApiResponse<ChatMessage>> sendDmMessage(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DmRequest req) {
        User sender = getUser(principal);
        // Validate recipient exists
        if (!userRepository.existsByUsername(req.getRecipientUsername()))
            throw new ResourceNotFoundException("Recipient user not found: " + req.getRecipientUsername());

        ChatMessage msg = ChatMessage.builder()
            .senderUsername(sender.getUsername())
            .senderRole(sender.getRole().name())
            .recipientUsername(req.getRecipientUsername())
            .text(req.getText().trim())
            .sentAt(LocalDateTime.now())
            .build();
        return ResponseEntity.ok(ApiResponse.ok("Message sent", chatRepository.save(msg)));
    }

    // ── Helpers ──────────────────────────────────────────────────

    private User getUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Data static class SendRequest {
        @NotBlank @Size(max = 2000) private String text;
    }

    @Data static class DmRequest {
        @NotBlank                    private String recipientUsername;
        @NotBlank @Size(max = 2000) private String text;
    }
}
