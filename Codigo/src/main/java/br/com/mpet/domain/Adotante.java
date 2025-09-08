package br.com.mpet.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Adotante implements Serializable {
    public String cpf; // PK
    public String nome;
    public String sobrenome;
    public int idade;
    public String endereco;
    public String senhaEnc; // senha criptografada (XOR, hex)

    public Adotante() {}

    public Adotante(String cpf, String nome, String sobrenome, int idade, String endereco) {
        this.cpf = cpf; this.nome = nome; this.sobrenome = sobrenome; this.idade = idade; this.endereco = endereco;
        this.senhaEnc = null; // Initialize senhaEnc
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        writeString(dos, cpf);
        writeString(dos, nome);
        writeString(dos, sobrenome);
        dos.writeInt(idade);
    writeString(dos, endereco);
    writeString(dos, senhaEnc); // Serialize senhaEnc
        dos.flush();
        return baos.toByteArray();
    }

    public static Adotante fromBytes(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        Adotante a = new Adotante();
        a.cpf = readString(dis);
        a.nome = readString(dis);
        a.sobrenome = readString(dis);
        a.idade = dis.readInt();
    a.endereco = readString(dis);
    a.senhaEnc = readString(dis); // Deserialize senhaEnc
        return a;
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
