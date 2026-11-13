package com.pvp_utils.client.communication;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class EncryptedChatCodec {
    public static final String TEXT_PREFIX = "~C";
    public static final String MARKER_PREFIX = "~P";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] SALT = "PVPUtils encrypted chat v1".getBytes(StandardCharsets.UTF_8);
    private static final String[] DIMENSIONS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
    private static final int MAX_PACKET_BYTES = 512;

    private EncryptedChatCodec() {
    }

    public static SecretKeySpec deriveKey(String password) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, 210_000, 256);
        byte[] bytes = null;
        try {
            bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return new SecretKeySpec(bytes, "AES");
        } finally {
            spec.clearPassword();
            if (bytes != null) Arrays.fill(bytes, (byte) 0);
        }
    }

    public static String encryptText(SecretKeySpec key, String text) throws GeneralSecurityException {
        byte[] raw = text.getBytes(StandardCharsets.UTF_8);
        if (raw.length > 4096) throw new IllegalArgumentException("Message too long");
        byte[] compressed = compress(raw);
        boolean useCompression = compressed.length < raw.length;
        byte[] data = useCompression ? compressed : raw;
        ByteBuffer body = ByteBuffer.allocate(data.length + 1);
        body.put((byte) (useCompression ? 1 : 0)).put(data);
        return encrypt(key, TEXT_PREFIX, body.array());
    }

    public static String encryptMarker(SecretKeySpec key, BlockPos position, String dimension, boolean danger)
            throws GeneralSecurityException {
        return encryptMarker(key, position, Vec3.upFromBottomCenterOf(position, 1.0), Direction.UP, dimension, danger);
    }

    public static String encryptMarker(SecretKeySpec key, BlockPos position, Vec3 location, Direction face,
                                       String dimension, boolean danger) throws GeneralSecurityException {
        return encryptMarkerBody(key, position, location, face, dimension, danger, null, 0);
    }

    public static String encryptMarker(SecretKeySpec key, BlockPos position, Vec3 location, Direction face,
                                       String dimension, boolean danger, Vec3 senderPosition, int colorCode)
            throws GeneralSecurityException {
        if (senderPosition == null) throw new IllegalArgumentException("Invalid sender position");
        return encryptMarkerBody(key, position, location, face, dimension, danger, senderPosition, colorCode);
    }

    private static String encryptMarkerBody(SecretKeySpec key, BlockPos position, Vec3 location, Direction face,
                                            String dimension, boolean danger, Vec3 senderPosition, int colorCode)
            throws GeneralSecurityException {
        if (position == null || !BlockPos.of(position.asLong()).equals(position)) {
            throw new IllegalArgumentException("Position out of range");
        }
        if (location == null || face == null) throw new IllegalArgumentException("Invalid marker surface");
        if (colorCode < 0 || colorCode > 7) throw new IllegalArgumentException("Invalid color code");
        byte x = encodeFraction(location.x - position.getX());
        byte y = encodeFraction(location.y - position.getY());
        byte z = encodeFraction(location.z - position.getZ());
        if (!validDimension(dimension)) throw new IllegalArgumentException("Invalid dimension");
        int dimensionId = 3;
        for (int i = 0; i < DIMENSIONS.length; i++) {
            if (DIMENSIONS[i].equals(dimension)) dimensionId = i;
        }
        byte[] name = dimensionId == 3 ? dimension.getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (name.length > 128) throw new IllegalArgumentException("Invalid dimension");
        BlockPos senderBlock = null;
        int senderData = 0;
        boolean compactSender = false;
        if (senderPosition != null) {
            if (!Double.isFinite(senderPosition.x) || !Double.isFinite(senderPosition.y)
                    || !Double.isFinite(senderPosition.z)) throw new IllegalArgumentException("Invalid sender position");
            senderBlock = BlockPos.containing(senderPosition);
            if (!BlockPos.of(senderBlock.asLong()).equals(senderBlock)) {
                throw new IllegalArgumentException("Sender position out of range");
            }
            long dx = (long) senderBlock.getX() - position.getX();
            long dy = (long) senderBlock.getY() - position.getY();
            long dz = (long) senderBlock.getZ() - position.getZ();
            compactSender = dx >= -256 && dx <= 255 && dy >= -256 && dy <= 255 && dz >= -256 && dz <= 255;
            if (compactSender) {
                senderData = ((int) dx & 511) | (((int) dy & 511) << 9) | (((int) dz & 511) << 18) | (colorCode << 27);
            }
        }
        int senderBytes = senderBlock == null ? 0 : compactSender ? 4 : 9;
        ByteBuffer body = ByteBuffer.allocate(12 + senderBytes + name.length);
        body.put((byte) (dimensionId | (danger ? 4 : 0) | (face.get3DDataValue() << 3) | 64 | (senderBlock == null ? 0 : 128)))
                .putLong(position.asLong()).put(x).put(y).put(z);
        if (senderBlock != null) {
            if (compactSender) body.putInt(senderData);
            else body.put((byte) (128 | colorCode)).putLong(senderBlock.asLong());
        }
        body.put(name);
        return encrypt(key, MARKER_PREFIX, body.array());
    }

    public static Decoded decrypt(SecretKeySpec key, String token) {
        if (token == null || token.length() < 16 || token.length() > 256) return null;
        String prefix = null;
        String symbols = token;
        if (token.startsWith(TEXT_PREFIX) || token.startsWith(MARKER_PREFIX)) {
            if (!token.endsWith("~")) return null;
            prefix = token.substring(0, 2);
            symbols = token.substring(2, token.length() - 1);
        }
        byte[] packet = unpack(symbols);
        if (packet == null || packet.length < 29 || packet.length > MAX_PACKET_BYTES) return null;
        String[] prefixes = prefix == null ? new String[]{TEXT_PREFIX, MARKER_PREFIX} : new String[]{prefix};
        for (int trim = 0; trim <= 1; trim++) {
            if (trim == 1 && packet[packet.length - 1] != 0) continue;
            byte[] candidate = trim == 0 ? packet : Arrays.copyOf(packet, packet.length - 1);
            if (!pack(candidate).equals(symbols)) continue;
            for (String type : prefixes) {
                try {
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, candidate, 0, 12));
                    cipher.updateAAD(aad(type));
                    byte[] plain = cipher.doFinal(candidate, 12, candidate.length - 12);
                    return parse(type, plain);
                } catch (GeneralSecurityException | IllegalArgumentException exception) {
                    continue;
                }
            }
        }
        return null;
    }

    private static String encrypt(SecretKeySpec key, String prefix, byte[] plain) throws GeneralSecurityException {
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(aad(prefix));
        byte[] encrypted = cipher.doFinal(plain);
        byte[] packet = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, packet, 0, nonce.length);
        System.arraycopy(encrypted, 0, packet, nonce.length, encrypted.length);
        return pack(packet);
    }

    private static byte[] aad(String prefix) {
        return ("PVPUtils/chat/v1/" + prefix).getBytes(StandardCharsets.US_ASCII);
    }

    private static Decoded parse(String prefix, byte[] plain) {
        if (TEXT_PREFIX.equals(prefix)) {
            if (plain.length < 1 || (plain[0] & 254) != 0) return null;
            byte[] text = Arrays.copyOfRange(plain, 1, plain.length);
            if ((plain[0] & 1) != 0) text = inflate(text);
            String decoded = decodeUtf8(text);
            return decoded == null ? null : new Decoded(decoded, null, null, false, null, null, null, 0);
        }
        if (plain.length < 9) return null;
        int metadata = plain[0] & 255;
        boolean extended = (metadata & 64) != 0;
        boolean hasSender = (metadata & 128) != 0;
        int faceId = (metadata >>> 3) & 7;
        if (!extended && (metadata & 0xF8) != 0 || extended && (plain.length < 12 || faceId > 5)) return null;
        int dimensionId = metadata & 3;
        int dataEnd = extended ? 12 : 9;
        BlockPos position = BlockPos.of(ByteBuffer.wrap(plain, 1, 8).getLong());
        Vec3 location = extended ? new Vec3(position.getX() + decodeFraction(plain[9]),
                position.getY() + decodeFraction(plain[10]), position.getZ() + decodeFraction(plain[11]))
                : Vec3.upFromBottomCenterOf(position, 1.02);
        Direction face = extended ? Direction.from3DDataValue(faceId) : Direction.UP;
        Vec3 senderPosition = location;
        int colorCode = 0;
        if (hasSender) {
            if (plain.length <= dataEnd) return null;
            int senderHeader = plain[dataEnd] & 255;
            BlockPos senderBlock;
            if ((senderHeader & 128) != 0) {
                if ((senderHeader & 0x78) != 0 || plain.length < dataEnd + 9) return null;
                colorCode = senderHeader & 7;
                senderBlock = BlockPos.of(ByteBuffer.wrap(plain, dataEnd + 1, 8).getLong());
                dataEnd += 9;
            } else {
                if ((senderHeader & 64) != 0 || plain.length < dataEnd + 4) return null;
                int senderData = ByteBuffer.wrap(plain, dataEnd, 4).getInt();
                colorCode = (senderData >>> 27) & 7;
                senderBlock = position.offset(decodeDelta(senderData), decodeDelta(senderData >>> 9), decodeDelta(senderData >>> 18));
                if (!BlockPos.of(senderBlock.asLong()).equals(senderBlock)) return null;
                dataEnd += 4;
            }
            senderPosition = Vec3.atCenterOf(senderBlock);
        }
        if (dimensionId != 3 && plain.length != dataEnd || plain.length - dataEnd > 128) return null;
        String dimension = dimensionId == 3 ? decodeUtf8(Arrays.copyOfRange(plain, dataEnd, plain.length)) : DIMENSIONS[dimensionId];
        if (dimension == null || dimension.length() > 128 || !validDimension(dimension)) return null;
        return new Decoded(null, position, dimension, (metadata & 4) != 0, location, face, senderPosition, colorCode);
    }

    private static byte encodeFraction(double value) {
        if (!Double.isFinite(value) || value < -0.000001 || value > 1.000001) {
            throw new IllegalArgumentException("Location outside marker block");
        }
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return (byte) (clamped >= 255.0 / 256.0 ? 255 : Math.min(254, Math.round(clamped * 256.0)));
    }

    private static double decodeFraction(byte value) {
        int code = value & 255;
        return code == 255 ? 1.0 : code / 256.0;
    }

    private static int decodeDelta(int value) {
        return (value & 511) << 23 >> 23;
    }

    private static boolean validDimension(String dimension) {
        return dimension != null && dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+");
    }

    private static String pack(byte[] bytes) {
        StringBuilder result = new StringBuilder((bytes.length * 8 + 14) / 15);
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 255);
            bits += 8;
            if (bits >= 15) {
                bits -= 15;
                result.append(symbol((buffer >>> bits) & 32767));
                buffer &= (1 << bits) - 1;
            }
        }
        if (bits > 0) result.append(symbol(buffer << (15 - bits)));
        return result.toString();
    }

    private static byte[] unpack(String symbols) {
        if (symbols.length() > 273) return null;
        ByteArrayOutputStream result = new ByteArrayOutputStream(symbols.length() * 15 / 8);
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < symbols.length(); i++) {
            char symbol = symbols.charAt(i);
            int value;
            if (symbol >= 0x3400 && symbol <= 0x9FFF) value = symbol - 0x3400;
            else if (symbol >= 0xAC00 && symbol <= 0xBFFF) value = symbol - 0xAC00 + 0x6C00;
            else return null;
            buffer = (buffer << 15) | value;
            bits += 15;
            while (bits >= 8) {
                bits -= 8;
                result.write((buffer >>> bits) & 255);
            }
            buffer &= (1 << bits) - 1;
        }
        return buffer == 0 ? result.toByteArray() : null;
    }

    private static char symbol(int value) {
        return (char) (value < 0x6C00 ? 0x3400 + value : 0xAC00 + value - 0x6C00);
    }

    private static byte[] compress(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(raw);
            deflater.finish();
            byte[] buffer = new byte[raw.length + 64];
            int length = deflater.deflate(buffer);
            return deflater.finished() ? Arrays.copyOf(buffer, length) : raw;
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflate(byte[] data) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            byte[] output = new byte[4096];
            int length = inflater.inflate(output);
            return inflater.finished() && inflater.getRemaining() == 0 ? Arrays.copyOf(output, length) : null;
        } catch (DataFormatException exception) {
            return null;
        } finally {
            inflater.end();
        }
    }

    private static String decodeUtf8(byte[] data) {
        if (data == null || data.length > 4096) return null;
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException exception) {
            return null;
        }
    }

    public record Decoded(String text, BlockPos position, String dimension, boolean danger, Vec3 location,
                          Direction face, Vec3 senderPosition, int colorCode) {
        public boolean marker() {
            return position != null;
        }
    }
}
