package com.smapps.chat.protocol.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smapps.chat.common.id.FileId;
import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.FileStatus;
import com.smapps.chat.protocol.validation.ProtocolValidationException;
import org.junit.jupiter.api.Test;

/** Verifies bounded upload headers and safe download metadata. */
class FileTransferHttpContractTest {

  @Test
  void shouldBuildContentPathWhenFileIdIsValid() {
    FileId fileId = FileId.parse("50000000-0000-0000-0000-000000000001");

    assertEquals(
        "/api/files/50000000-0000-0000-0000-000000000001/content",
        FileTransferHttpContract.contentPath(fileId));
  }

  @Test
  void shouldAcceptContentLengthWhenItMatchesDeclaration() {
    FileTransferHttpContract.validateContentLength(1024, 1024);
  }

  @Test
  void shouldRejectContentLengthWhenItWouldAppendBytes() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () -> FileTransferHttpContract.validateContentLength(1025, 1024));

    assertEquals(ErrorCode.LIMIT_EXCEEDED, exception.errorCode());
  }

  @Test
  void shouldRemovePathAndHeaderCharactersWhenDispositionIsBuilt() {
    String disposition = FileTransferHttpContract.contentDisposition("../报告\";\r\n.txt");

    assertFalse(disposition.contains("../"));
    assertFalse(disposition.contains("\r"));
    assertFalse(disposition.contains("\n"));
  }

  @Test
  void shouldExtractTokenWhenBearerHeaderIsValid() {
    String token = "example-session-token-with-at-least-32-characters";

    assertEquals(token, FileTransferHttpContract.extractBearerToken("Bearer " + token));
  }

  @Test
  void shouldReplaceTemporaryFileWhenUploadIsRetried() {
    FileId fileId = FileId.parse("50000000-0000-0000-0000-000000000001");

    assertEquals(
        UploadRetryAction.REPLACE_TEMPORARY_FILE,
        FileTransferHttpContract.uploadRetryAction(fileId, FileStatus.UPLOADING));
  }

  @Test
  void shouldKeepOnlySanitizedMetadataWhenFileIsDeclared() {
    FileDeclare declaration =
        new FileDeclare(
            UserId.parse("20000000-0000-0000-0000-000000000002"),
            "../folder/report.txt",
            12,
            "a948904f2f0f479b8f8197694b30184b0d2ed1c1cd2a1ec0fb85d299a192a447");

    assertEquals("report.txt", declaration.displayName());
  }
}
