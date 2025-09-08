package br.com.mpet.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Animal implements Serializable {
    public String id; // PK
    public String nome;
    public int idade;
    public String especie;
    public boolean seCastrado;

    public Animal() {}

    public Animal(String id, String nome, int idade, String especie, boolean seCastrado) {
        this.id = id; this.nome = nome; this.idade = idade; this.especie = especie; this.seCastrado = seCastrado;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        writeString(dos, id);
        writeString(dos, nome);
        dos.writeInt(idade);
        writeString(dos, especie);
        dos.writeBoolean(seCastrado);
        dos.flush();
        return baos.toByteArray();
    }

    public static Animal fromBytes(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        Animal a = new Animal();
        a.id = readString(dis);
        a.nome = readString(dis);
        a.idade = dis.readInt();
        a.especie = readString(dis);
        a.seCastrado = dis.readBoolean();
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
