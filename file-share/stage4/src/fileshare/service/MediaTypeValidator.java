package fileshare.service;

import org.springframework.http.MediaType;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class MediaTypeValidator {
    private MediaTypeValidator() { }

    public static boolean isValidFileContents(byte[] contents, String mimeType) {

        return switch (mimeType) {
            case MediaType.TEXT_PLAIN_VALUE -> isPlainText(contents);
            case MediaType.IMAGE_PNG_VALUE -> isPngImage(contents);
            case MediaType.IMAGE_JPEG_VALUE, "image/jpg" -> isJpegImage(contents);
            default -> false;
        };
    }

    private static boolean isPlainText(byte[] contents) {
        try {
            CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();

            utf8Decoder.reset();
            utf8Decoder.decode(ByteBuffer.wrap(contents));

            return true;
        } catch (CharacterCodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isPngImage(byte[] contents) {
        if (contents.length < 8) {
            return false;
        }

        return (contents[0] & 0xff) == 0x89 &&
                (contents[1] & 0xff) == 0x50 &&
                (contents[2] & 0xff) == 0x4e &&
                (contents[3] & 0xff) == 0x47 &&
                (contents[4] & 0xff) == 0x0d &&
                (contents[5] & 0xff) == 0x0a &&
                (contents[6] & 0xff) == 0x1a &&
                (contents[7] & 0xff) == 0x0a;
    }

    private static boolean isJpegImage(byte[] contents) {
        return (contents[0] & 0xff) == 0xff &&
                (contents[1] & 0xff) == 0xd8 &&
                (contents[contents.length - 2] & 0xff) == 0xff &&
                (contents[contents.length - 1] & 0xff) == 0xd9;
    }
}
