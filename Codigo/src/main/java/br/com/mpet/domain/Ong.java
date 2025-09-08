package br.com.mpet.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Ong implements Serializable {
    public String nome; // PK
    public String endereco;
    public String telefone;
    public String senhaEnc; // senha criptografada (XOR, hex)

    public Ong() {}

    public Ong(String nome, String endereco, String telefone) {
        this.nome = nome; this.endereco = endereco; this.telefone = telefone;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        writeString(dos, nome);
        writeString(dos, endereco);
    writeString(dos, telefone);
    writeString(dos, senhaEnc);
        dos.flush();
        return baos.toByteArray();
    }

    public static Ong fromBytes(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        Ong o = new Ong();
        o.nome = readString(dis);
        o.endereco = readString(dis);
    o.telefone = readString(dis);
    o.senhaEnc = readString(dis);
        return o;
    }

    private static void writeString(DataOutputStream dos, String s) throws IOException {
        if (s == null) { dos.writeInt(-1); return; }
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(b.length);
        dos.write(b);
    }

    private static String readString(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        if (len < 0) return null;
        byte[] b = new byte[len];
        dis.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
