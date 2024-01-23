package demo.ss.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import demo.ss.exception.QrGenerationException;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ZxingPngQrGenerator {
    private static int imageSize = 350;

    public static String getImageMimeType() {
        return "image/png";
    }

    public static byte[] generate(String data) throws QrGenerationException {
        Writer writer = null;
        try {
            writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, imageSize, imageSize);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new QrGenerationException("Failed to generate QR code. See nested exception.", e);
        }
    }

    public static String generateBase64QRPng(String data) throws QrGenerationException {
        final byte[] bytes = generate(data);
        String base64Encoded = Base64.getEncoder().encodeToString(bytes);
        return "data:image/png;base64," + base64Encoded;
    }
}
