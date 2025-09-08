package br.com.mpet.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Voluntario implements Serializable {
    public String cpf; // PK
    public String nome;
    public String sobrenome;
    public int idade;
    public String senhaEnc; // XOR encrypted password (hex)

    public Voluntario() {}

    public Voluntario(String cpf, String nome, String sobrenome, int idade) {
        this.cpf = cpf; this.nome = nome; this.sobrenome = sobrenome; this.idade = idade;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        writeString(dos, cpf);
        writeString(dos, nome);
        writeString(dos, sobrenome);
        dos.writeInt(idade);
    writeString(dos, senhaEnc);
        dos.flush();
        return baos.toByteArray();
    }

    public static Voluntario fromBytes(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        Voluntario v = new Voluntario();
        v.cpf = readString(dis);
        v.nome = readString(dis);
        v.sobrenome = readString(dis);
        v.idade = dis.readInt();
    v.senhaEnc = readString(dis);
        return v;
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
