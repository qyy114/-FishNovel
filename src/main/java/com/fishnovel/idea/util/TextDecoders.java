package com.fishnovel.idea.util;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class TextDecoders {
    private static final List<Charset> CANDIDATES = List.of(
        StandardCharsets.UTF_8,
        Charset.forName("GB18030"),
        StandardCharsets.UTF_16LE,
        StandardCharsets.UTF_16BE
    );

    private TextDecoders() {
    }

    public static String decode(byte[] bytes) {
        if (bytes.length >= 3
            && (bytes[0] & 0xFF) == 0xEF
            && (bytes[1] & 0xFF) == 0xBB
            && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        for (Charset charset : CANDIDATES) {
            try {
                return charset
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            } catch (CharacterCodingException ignored) {
                // Try the next charset candidate.
            }
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
