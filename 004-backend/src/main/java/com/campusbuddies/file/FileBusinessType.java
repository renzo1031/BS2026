package com.campusbuddies.file;

public enum FileBusinessType {
    AVATAR(2 * 1024 * 1024L),
    ACTIVITY_IMAGE(5 * 1024 * 1024L),
    CHAT_IMAGE(5 * 1024 * 1024L),
    IDENTITY_PROOF(8 * 1024 * 1024L),
    REPORT_EVIDENCE(8 * 1024 * 1024L);

    private final long maxBytes;

    FileBusinessType(long maxBytes) { this.maxBytes = maxBytes; }

    public long maxBytes() { return maxBytes; }
}
