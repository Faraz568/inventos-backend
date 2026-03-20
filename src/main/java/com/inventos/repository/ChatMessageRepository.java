package com.inventos.repository;

import com.inventos.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Group chat — messages with no recipient */
    List<ChatMessage> findByRecipientUsernameIsNullOrderBySentAtAsc();

    /** DM conversation between two users (both directions) */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.recipientUsername IS NOT NULL
          AND ((m.senderUsername = :a AND m.recipientUsername = :b)
            OR (m.senderUsername = :b AND m.recipientUsername = :a))
        ORDER BY m.sentAt ASC
    """)
    List<ChatMessage> findDmConversation(@Param("a") String userA, @Param("b") String userB);
}
