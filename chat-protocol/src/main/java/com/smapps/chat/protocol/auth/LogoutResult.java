package com.smapps.chat.protocol.auth;

/** Reports whether logout observed an already inactive session. */
public record LogoutResult(boolean alreadyInactive) {}
