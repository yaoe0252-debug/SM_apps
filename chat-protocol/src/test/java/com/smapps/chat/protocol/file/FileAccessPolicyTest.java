package com.smapps.chat.protocol.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.FileStatus;
import com.smapps.chat.protocol.validation.ProtocolValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifies that every file request rechecks identity, state, and expiry. */
class FileAccessPolicyTest {

  private static final UserId ALICE = UserId.parse("20000000-0000-0000-0000-000000000001");
  private static final UserId BOB = UserId.parse("20000000-0000-0000-0000-000000000002");
  private static final UserId EVE = UserId.parse("20000000-0000-0000-0000-000000000003");
  private static final Instant NOW = Instant.parse("2026-10-05T08:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void shouldAllowUploadWhenOwnerStateAndExpiryAreValid() {
    assertDoesNotThrow(
        () ->
            FileAccessPolicy.authorizeUpload(
                ALICE, ALICE, FileStatus.READY, NOW.plusSeconds(60), CLOCK));
  }

  @Test
  void shouldRejectUploadWhenRequesterIsNotOwner() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () ->
                FileAccessPolicy.authorizeUpload(
                    BOB, ALICE, FileStatus.READY, NOW.plusSeconds(60), CLOCK));

    assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
  }

  @Test
  void shouldAllowDownloadWhenRecipientAndStateAreValid() {
    assertDoesNotThrow(
        () ->
            FileAccessPolicy.authorizeDownload(
                BOB, ALICE, BOB, FileStatus.AVAILABLE, NOW.plusSeconds(60), CLOCK));
  }

  @Test
  void shouldRejectDownloadWhenRequesterIsNotParticipant() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () ->
                FileAccessPolicy.authorizeDownload(
                    EVE, ALICE, BOB, FileStatus.AVAILABLE, NOW.plusSeconds(60), CLOCK));

    assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
  }

  @Test
  void shouldRejectRequestWhenAuthorizationIsExpired() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () ->
                FileAccessPolicy.authorizeDownload(
                    BOB, ALICE, BOB, FileStatus.AVAILABLE, NOW, CLOCK));

    assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
  }
}
