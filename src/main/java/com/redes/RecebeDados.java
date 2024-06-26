package com.redes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
//import java.nio.IntBuffer;
//import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecebeDados extends Thread {

    private final int portaLocalReceber = 2001;
    private final int portaLocalEnviar = 2002;
    private final int portaDestino = 2003;
    private static long numSeqExpected = 0;



    private void enviaAck(boolean fim, long seq) {

        try {
            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnviar)) {
                ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putLong(fim ? -1 : seq);
                byte[] sendData = buffer.array();

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
        try {
            DatagramSocket serverSocket = new DatagramSocket(portaLocalReceber);
            byte[] receiveData = new byte[2808];
            try (FileOutputStream fileOutput = new FileOutputStream("saida")) {
                boolean fim = false;
                while (!fim) {

                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);

                    byte[] tmp = receivePacket.getData();

                    long seq = ((long)(tmp[0] & 0xff) << 56) +
                            ((long)(tmp[1] & 0xff) << 48) +
                            ((long)(tmp[2] & 0xff) << 40) +
                            ((long)(tmp[3] & 0xff) << 32) +
                            ((long)(tmp[4] & 0xff) << 24) +
                            ((long)(tmp[5] & 0xff) << 16) +
                            ((long)(tmp[6] & 0xff) << 8) +
                            ((long)(tmp[7] & 0xff));

                    //probabilidade de 60% de perder
                    //gero um numero aleatorio contido entre [0,1]
                    //se numero cair no intervalo [0, 0,6]
                    //significa perda, logo, você não envia ACK
                    //para esse pacote, e não escreve ele no arquivo saida.
                    //se o numero cair no intervalo [0,6, 1,0]
                    //assume-se o recebimento com sucesso.

                    //Random random = new Random();
                    //double chance = random.nextDouble(); // Gera um número entre 0.0 e 1.0
                    //
                    //if (chance < 0.6  && numSeqExpected != 0) {
                    //    System.out.println("pacote " + seq + " perdido.");
                    //} else {

                    System.out.println("dado recebido");
                    for (int i = 8; i < tmp.length; i = i + 8) {
                        long dados = ((long)(tmp[i] & 0xff) << 56) +
                                ((long)(tmp[i + 1] & 0xff) << 48) +
                                ((long)(tmp[i + 2] & 0xff) << 40) +
                                ((long)(tmp[i + 3] & 0xff) << 32) +
                                ((long)(tmp[i + 4] & 0xff) << 24) +
                                ((long)(tmp[i + 5] & 0xff) << 16) +
                                ((long)(tmp[i + 6] & 0xff) << 8) +
                                ((long)(tmp[i + 7] & 0xff));
                        if (dados == -1) {
                            fim = true;
                            break;
                        }
                        fileOutput.write((int) dados);

                    }
                    enviaAck(fim, seq);

                    //}

                    //if(numSeqExpected == seq){
                    //    enviaAck(fim, seq);
                    //    numSeqExpected++;
                    //}else{
                    //    enviaAck(fim, numSeqExpected - 1);
                    //}
                }
            }
        } catch (IOException e) {
            System.out.println("Excecao: " + e.getMessage());
        }
    }
}