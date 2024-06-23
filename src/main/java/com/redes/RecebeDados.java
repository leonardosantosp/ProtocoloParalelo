package com.redes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecebeDados extends Thread {

    private final int portaLocalReceber = 2001;
    private final int portaLocalEnviar = 2002;
    private final int portaDestino = 2003;

    private int expectedSeqNum = 0;

    private void enviaAck(boolean fim, int seqNum) {
        try {
            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnviar)) {
                String sendString = fim ? "F" : "A" + seqNum;

                byte[] sendData = sendString.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length, address, portaDestino);

                datagramSocket.send(packet);
            }
        } catch (SocketException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try (DatagramSocket serverSocket = new DatagramSocket(portaLocalReceber);
             FileOutputStream fileOutput = new FileOutputStream("saida")) {
            byte[] receiveData = new byte[1404]; // Ajuste o tamanho do buffer para incluir o número de sequência
            boolean fim = false;
            while (!fim) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                System.out.println("dado recebido");

                ByteBuffer byteBuffer = ByteBuffer.wrap(receivePacket.getData());
                int seqNum = byteBuffer.getInt(); // Lê o número de sequência

                if (seqNum == expectedSeqNum) {
                    for (int i = 4; i < receivePacket.getLength(); i += 4) {
                        int dados = byteBuffer.getInt(i);
                        if (dados == -1) {
                            fim = true;
                            break;
                        }
                        fileOutput.write(dados);
                    }
                    enviaAck(fim, seqNum);
                    expectedSeqNum++;
                } else {
                    enviaAck(fim, expectedSeqNum - 1); // Reenvia o ack do último pacote correto
                }
            }
        } catch (IOException e) {
            System.out.println("Excecao: " + e.getMessage());
        }
    }
}
