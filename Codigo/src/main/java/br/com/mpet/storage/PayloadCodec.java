package br.com.mpet.storage;

public interface PayloadCodec {
    byte[] encode(byte[] data);
    byte[] decode(byte[] data);

    static PayloadCodec identity() {
        return new PayloadCodec() {
            @Override public byte[] encode(byte[] data) { return data; }
            @Override public byte[] decode(byte[] data) { return data; }
        };
    }
}
