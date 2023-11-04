/*
 * Copyright (c) 2023 Jean Schmitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.talkforgeai.backend.chat.repository;

import com.talkforgeai.backend.chat.domain.ChatMessageEntity;
import com.talkforgeai.backend.chat.domain.ChatMessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findAllByChatSessionIdAndType(UUID sessionId, ChatMessageType type);

    @Query("select cm from ChatMessageEntity cm where cm.chatSession.id = ?1 " +
            "and cm.type = com.talkforgeai.backend.chat.domain.ChatMessageType.PROCESSED " +
            "order by cm.id asc")
    ChatMessageEntity findLastProcessedMessage(UUID sessionId);
}
