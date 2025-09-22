package com.sankim.chat_server.chat.chat.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReadUpToRequest(@NotNull Long lastReadMessageId) { }

