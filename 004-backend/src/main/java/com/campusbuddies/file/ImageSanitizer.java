package com.campusbuddies.file;

import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class ImageSanitizer {
    private static final long MAX_PIXELS = 40_000_000L;
    private static final int MAX_DIMENSION = 8_000;

    public record Sanitized(byte[] bytes, String contentType, String extension,
                            int width, int height, String sha256) {}

    public Sanitized sanitize(byte[] source) {
        if (source == null || source.length < 12 || !hasSupportedMagic(source)) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "仅支持真实的 JPEG、PNG 或 WebP 图片");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException(ErrorCode.FILE_REJECTED, "无法识别图片内容");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new BusinessException(ErrorCode.FILE_REJECTED, "图片尺寸或像素数量超过限制");
                }
                try {
                    if (reader.getNumImages(true) > 1) {
                        throw new BusinessException(ErrorCode.FILE_REJECTED, "不支持动图");
                    }
                } catch (UnsupportedOperationException ignored) {
                    // Some readers cannot count frames; only the decoded first frame is accepted below.
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new BusinessException(ErrorCode.FILE_REJECTED, "图片解码失败");
                boolean alpha = decoded.getColorModel().hasAlpha();
                String format = alpha ? "png" : "jpg";
                String contentType = alpha ? "image/png" : "image/jpeg";
                BufferedImage safe = alpha ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                        : new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = safe.createGraphics();
                try {
                    graphics.drawImage(decoded, 0, 0, null);
                } finally {
                    graphics.dispose();
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(safe, format, output)) {
                    throw new BusinessException(ErrorCode.FILE_REJECTED, "图片安全重编码失败");
                }
                byte[] bytes = output.toByteArray();
                return new Sanitized(bytes, contentType, format, width, height, sha256(bytes));
            } finally {
                reader.dispose();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "图片内容损坏");
        }
    }

    private boolean hasSupportedMagic(byte[] bytes) {
        boolean jpeg = (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        boolean png = (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
        boolean webp = bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        return jpeg || png || webp;
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
