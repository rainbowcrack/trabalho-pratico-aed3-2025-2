package br.com.mpet.persistence.io;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Codec fornece métodos de serialização/desserialização de baixo nível para os
 * tipos primitivos e estruturados usados nos registros binários do projeto.
 * 
 * Regras gerais:
 *  - Endianness: big-endian (mais significativo primeiro) para números multi-byte.
 *  - Strings: length unsigned 16 bits (2 bytes) + dados UTF-8.
 *      * length = 0xFFFF => null (sentinela de null)
 *      * length = 0 => string vazia ""
 *      * length = 1..65534 => dados UTF-8 exatamente como foram codificados (sem trim/normalização)
 *  - Tri-Boolean: 1 byte ASCII ('V'=verdadeiro, 'F'=falso, 'U'=indefinido/null).
 *  - Enum: 1 byte (0 = null; 1..255 = ordinal+1). Suporta até 255 constantes.
 *  - LocalDate: 1 byte flag (0 = null, 1 = presente) + ano(int 4 bytes) + mês(1 byte) + dia(1 byte).
 *  - char: 2 bytes (big-endian) para suportar todo o intervalo UTF-16 (não usar se quiser apenas ASCII).
 *
 * Esta versão NÃO escreve em streams; somente converte para/desde arrays de bytes.
 * Para desserialização incremental usamos um contêiner Decoded<T> contendo valor e próximo offset.
 *
 * Convenções de Null:
 *  - String: length = 0xFFFF.
 *  - Enum: byte = 0x00.
 *  - LocalDate: flag 0.
 *  - Tri-Boolean: 'U'.
 */
public final class Codec {

    private Codec() {}

    /* =============================================================
     * CONTÊINER DE DESSERIALIZAÇÃO
     * ============================================================= */
    public static final class Decoded<T> {
        public final T value;
        public final int nextOffset;
        public Decoded(T value, int nextOffset) { this.value = value; this.nextOffset = nextOffset; }
    }

    public static byte[] encodeStringU16(String s) {
        // Convenção: null => 0xFFFF; "" => length 0; dados 1..65534 bytes
        if (s == null) {
            return new byte[]{(byte)0xFF, (byte)0xFF};
        }
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        if (data.length > 0xFFFE) {
            throw new IllegalArgumentException("String excede 65534 bytes UTF-8: " + data.length);
        }
        int len = data.length; // 0..65534
        byte[] out = new byte[2 + len];
        out[0] = (byte)((len >> 8) & 0xFF);
        out[1] = (byte)(len & 0xFF);
        if (len > 0) System.arraycopy(data, 0, out, 2, len);
        return out;
    }
    // Exemplo:
    // s = null -> [0xFF, 0xFF]
    // s = ""   -> [0x00, 0x00]
    // s = "A"  -> [0x00, 0x01, 0x41]
    // s = "あ" -> UTF-8 3 bytes (0xE3 0x81 0x82) => [0x00,0x03, 0xE3,0x81,0x82]
    // 0000 1011 0000 0110
    // 0000 0000 | 0000 1011 = 0000 0000
    // 0000 1011 0000 0110 = 0000 0110


    public static byte[] encodeTriBoolean(Boolean v) {
        return new byte[]{ (byte)(v == null ? 'U' : (v ? 'V' : 'F')) };
    }
    // Exemplo:
    // v = true  -> [ 'V' ] (0x56)
    // v = false -> [ 'F' ] (0x46)
    // v = null  -> [ 'U' ] (0x55)

    public static byte[] encodeInt(int v) {
        return new byte[]{
            (byte)((v >> 24)&0xFF),
            (byte)((v >> 16)&0xFF),
            (byte)((v >> 8)&0xFF),
            (byte)(v & 0xFF)
        };
    }
    // int v=11
    //obs: Sendo o 0xFF é para garantir que o byte seja tratado como unsigned. E conseguimos o valor correto ao fazer o AND com 0xFF.
    // Já o & 0xFF é para garantir que apenas os 8 bits menos significativos sejam considerados, eliminando qualquer bit extra que possa ter sido introduzido pela operação de deslocamento.
    // (Byte ) v = 0000 0000 0000 0000 0000 0000 0000 1011
    // (Byte) v>>24;
    // 0000 0000 | 0000 0000 0000 0000 0000 1011 = 11
    // (Byte) v>>16;
    // 0000 0000  0000 0000 | 0000 0000 0000 1011 = 11
    // (Byte) v>>8;
    // 0000 0000  0000 0000  0000 0000 | 0000 1011 = 11
    // (Byte) v;
    // 0000 0000  0000 0000  0000 0000  0000 1011 | = 11

    public static byte[] encodeShort(short v) {
        return new byte[]{ (byte)((v>>8)&0xFF), (byte)(v & 0xFF)};
    }
    // short v=11
    // (Byte ) v = 0000 0000 0000 1011
    // (Byte) v>>8;
    // 0000 0000 | 0000 1011 = 11
    // (Byte) v;
    // 0000 0000  0000 1011 | = 11

    public static byte[] encodeLong(long v) {
        return new byte[]{
            (byte)((v>>56)&0xFF),(byte)((v>>48)&0xFF),(byte)((v>>40)&0xFF),(byte)((v>>32)&0xFF),
            (byte)((v>>24)&0xFF),(byte)((v>>16)&0xFF),(byte)((v>>8)&0xFF),(byte)(v & 0xFF)
        };
    }
    // long v=11
    // (Byte ) v = 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 1011
    // (Byte) v>>56;
    // 0000 0000 | 0000 0000 0000 0000 0000 0000 0000 1011 = 11
    // (Byte) v>>48;
    // 0000 0000  0000 0000 | 0000 0000 0000 1011 = 11
    // (Byte) v>>40;
    // 0000 0000  0000 0000  0000 0000 | 0000 1011 = 11
    // (Byte) v>>32;
    // 0000 0000  0000 0000  0000 0000  0000 1011 | = 11
    // (Byte) v>>24;
    // 0000 0000  0000 0000  0000 0000  0000 1011 | = 11
    // (Byte) v>>16;
    // 0000 0000  0000 0000  0000 0000  0000 1011 | = 11
    // (Byte) v>>8;
    // 0000 0000  0000 0000  0000 0000  0000 1011 | = 11
    // (Byte) v;
    // 0000 0000  0000 0000  0000 0000  0000 1011 | = 11
    

    public static byte[] encodeChar(char c) {
        return new byte[]{ (byte)((c>>8)&0xFF), (byte)(c & 0xFF) };
    }
    // char c='A' = 65
    // (Byte ) c = 0000 0000 0100 0001
    // (Byte) c>>8;
    // 0000 0000 | 0100 0001 = 65
    // (Byte) c;
    // 0000 0000  0100 0001 | = 65
    // (Byte) c='あ' = 12354
    // (Byte ) c = 0011 0000 0010 0010
    // (Byte) c>>8;
    // 0000 0000 | 0011 0000 = 48
    // (Byte) c;
    // 0000 0000  0011 0000 | = 48
    


    public static <E extends Enum<E>> byte[] encodeEnum(E e) {
        if (e == null) return new byte[]{ 0 }; // 0 = null
        int ord = e.ordinal(); // 0..(n-1)
        if (ord >= 255) // porque armazenamos ord+1 em 1..255
            throw new IllegalArgumentException("Enum com ordinal >= 255 não suportado: " + ord);
        int stored = ord + 1; // 1..255
        return new byte[]{ (byte)(stored & 0xFF) };
    }
    // Exemplo:
    // enum Role { ADMIN, USER, GUEST }
    // e = USER (ordinal=1) -> [ 0x02 ] (1+1)
    // e = null -> [ 0x00 ]
    // enum Role { ADMIN(0), USER(1), GUEST(2) }
    // Role e = Role.USER;
    //obs: o 0 é o nullo pois pego o ordinal e somo 1. ao fazer isso garanto que o 0 nunca será um valor válido. E assim, posso usar o 0 como sentinela para null.
    // (Byte ) e = (ordinal() = 1) + 1 = 2
    // (Byte) e;
    // 0000 0000  0000 0010 | = 2



    public static byte[] encodeLocalDate(LocalDate d) {
        if (d == null) return new byte[]{0};
        byte[] year = encodeInt(d.getYear());
        byte[] out = new byte[1+4+1+1];
        out[0]=1;
        System.arraycopy(year,0,out,1,4);
        out[5]=(byte)d.getMonthValue();
        out[6]=(byte)d.getDayOfMonth();
        return out;
    }
    // Exemplo:
    // d = 2024-03-05 -> [0x01, 0x00,0x00,0x07,0xE8, 0x03, 0x05]
    // d = null       -> [0x00]

    /* =============================================================
     * DECODE (usa offset) - valida limites básicos
     * ============================================================= */
    private static void requireBytes(byte[] buf, int offset, int needed) {
        if (offset < 0 || offset + needed > buf.length) {
            throw new IllegalArgumentException("Buffer insuficiente: offset="+offset+" need="+needed+" len="+buf.length);
        }
    }
   

    public static Decoded<String> decodeStringU16(byte[] buf, int offset) {
        requireBytes(buf, offset, 2);
        int len = ((buf[offset] & 0xFF) << 8) | (buf[offset+1] & 0xFF);
        if (len == 0xFFFF) { // sentinela null
            return new Decoded<>(null, offset+2);
        }
        requireBytes(buf, offset+2, len);
        if (len == 0) {
            return new Decoded<>("", offset+2);
        }
        String s = new String(buf, offset+2, len, StandardCharsets.UTF_8);
        return new Decoded<>(s, offset+2+len);
    }
    // Exemplo:
    // buf = [0xFF,0xFF]          -> null
    // buf = [0x00,0x00]          -> ""
    // buf = [0x00,0x01,0x41]     -> "A"
    // buf = [0x00,0x03, E3 81 82] -> "あ"


    public static Decoded<Boolean> decodeTriBoolean(byte[] buf, int offset) {
        requireBytes(buf, offset, 1);
        int b = buf[offset] & 0xFF;
        return switch (b) {
            case 'V' -> new Decoded<>(Boolean.TRUE, offset+1);
            case 'F' -> new Decoded<>(Boolean.FALSE, offset+1);
            case 'U' -> new Decoded<>(null, offset+1);
            default -> throw new IllegalArgumentException("Byte inválido tri-boolean: 0x"+Integer.toHexString(b));
        };
    }
    // Exemplo:
    // [ 'V' ] -> true
    // [ 'F' ] -> false
    // [ 'U' ] -> null

    public static Decoded<Integer> decodeInt(byte[] buf, int offset) {
        requireBytes(buf, offset, 4);
        int v = ((buf[offset]&0xFF)<<24)|((buf[offset+1]&0xFF)<<16)|((buf[offset+2]&0xFF)<<8)|(buf[offset+3]&0xFF);
        return new Decoded<>(v, offset+4);
    }
    // Exemplo:
    // [0x00,0x00,0x00,0x0B] -> 11

    public static Decoded<Short> decodeShort(byte[] buf, int offset) {
        requireBytes(buf, offset, 2);
        short v = (short)(((buf[offset]&0xFF)<<8)|(buf[offset+1]&0xFF));
        return new Decoded<>(v, offset+2);
    }
    // Exemplo:
    // [0x00,0x0B] -> 11

    public static Decoded<Long> decodeLong(byte[] buf, int offset) {
        requireBytes(buf, offset, 8);
        long v = 0;
        for (int i=0;i<8;i++) v = (v<<8) | (buf[offset+i] & 0xFFL);
        return new Decoded<>(v, offset+8);
    }
    // Exemplo:
    // [0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x0B] -> 11L

    public static Decoded<Character> decodeChar(byte[] buf, int offset) {
        requireBytes(buf, offset, 2);
        char c = (char)(((buf[offset]&0xFF)<<8)|(buf[offset+1]&0xFF));
        return new Decoded<>(c, offset+2);
    }
    // Exemplo:
    // [0x00,0x41] -> 'A'
    // [0x30,0x22] -> 'あ' (U+3042)

    public static <E extends Enum<E>> Decoded<E> decodeEnum(byte[] buf, int offset, Class<E> enumClass) {
        requireBytes(buf, offset, 1);
        int b = buf[offset] & 0xFF; // 0 = null, 1..255 = ordinal+1
        if (b == 0) return new Decoded<>(null, offset+1);
        int ord = b - 1;
        E[] values = Objects.requireNonNull(enumClass.getEnumConstants(), "Enum sem constantes");
        if (ord < 0 || ord >= values.length) throw new IllegalArgumentException("Ordinal fora do intervalo: "+ord+" para enum "+enumClass.getSimpleName());
        return new Decoded<>(values[ord], offset+1);
    }
    // Exemplo:
    // Role: [0x02] -> USER (ordinal 1)
    // [0x00] -> null

    public static Decoded<LocalDate> decodeLocalDate(byte[] buf, int offset) {
        requireBytes(buf, offset, 1);
        int flag = buf[offset] & 0xFF;
        if (flag == 0) return new Decoded<>(null, offset+1);
        // precisa de 1 + 4 + 1 + 1 = 7 bytes
        requireBytes(buf, offset, 7);
        int year = ((buf[offset+1]&0xFF)<<24)|((buf[offset+2]&0xFF)<<16)|((buf[offset+3]&0xFF)<<8)|(buf[offset+4]&0xFF);
        int month = buf[offset+5] & 0xFF;
        int day = buf[offset+6] & 0xFF;
        return new Decoded<>(LocalDate.of(year, month, day), offset+7);
    }
    // Exemplo:
    // [0x01, 00 00 07 E8, 0x03, 0x05] -> 2024-03-05
    // [0x00] -> null

    /* =============================================================
     * COMPOSIÇÃO / CONCAT
     * ============================================================= */
    public static byte[] concat(byte[]... partes) {
        int total=0; for (byte[] p: partes) if (p!=null) total += p.length;
        byte[] out = new byte[total];
        int pos=0; for (byte[] p: partes) { if (p==null) continue; System.arraycopy(p,0,out,pos,p.length); pos+=p.length; }
        return out;
    }
}
