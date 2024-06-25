/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redes;

/**
 * @author flavio
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Semaphore;

public class EnviaDados extends Thread {

    private final int portaLocalEnvio = 2000;
    private final int portaDestino = 2001;
    private final int portaLocalRecebimento = 2003;
    Semaphore sem;
    private final String funcao;
    private static int seqNum = 0;
    private static int expectedAck = 0;
    private static HashMap<Integer,int[]> bufferPackage = new HashMap<>();
    private static int countRepeatedAck = 0;

    private static int lastAck = 0;

    public EnviaDados(Semaphore sem, String funcao) {
        super(funcao);
        this.sem = sem;
        this.funcao = funcao;
    }

    public String getFuncao() {
        return funcao;
    }

    private void enviaPct(int[] dados) {
        //converte int[] para byte[]
        ByteBuffer byteBuffer = ByteBuffer.allocate(dados.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(dados);

        byte[] buffer = byteBuffer.array();

        try {
            System.out.println("Semaforo: " + sem.availablePermits());
            sem.acquire();
            System.out.println("Semaforo: " + sem.availablePermits());

            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnvio)) {
                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, address, portaDestino);

                datagramSocket.send(packet);
                bufferPackage.put(seqNum,dados);
                seqNum++;
            }

            System.out.println("Envio feito." + dados[0]);
        } catch (SocketException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        switch (this.getFuncao()) {
            case "envia":
                //variavel onde os dados lidos serao gravados
                int[] dados = new int[351];
                //contador, para gerar pacotes com 1400 Bytes de tamanho
                //como cada int ocupa 4 Bytes, estamos lendo blocos com 350
                //int's por vez.
                int cont = 1;

                try (FileInputStream fileInput = new FileInputStream("entrada");) {
                    int lido;
                    while ((lido = fileInput.read()) != -1) {
                        dados[cont] = lido;
                        cont++;
                        if (cont == 351) {
                            //envia pacotes a cada 350 int's lidos.
                            //ou seja, 1400 Bytes.
                            dados[0] = seqNum;
                            enviaPct(dados);
                            cont = 1;
                        }
                    }

                    //ultimo pacote eh preenchido com
                    //-1 ate o fim, indicando que acabou
                    //o envio dos dados.
                    for (int i = cont; i < 351; i++)
                        dados[i] = -1;
                    enviaPct(dados);
                } catch (IOException e) {
                    System.out.println("Error message: " + e.getMessage());
                }
                break;
            case "ack":
                try {
                    DatagramSocket serverSocket = new DatagramSocket(portaLocalRecebimento);
                    byte[] receiveData = new byte[4];
                    var ack = 0;
                    while (ack != -1) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        var byteNumber = ByteBuffer.wrap(receivePacket.getData());

                        ack = byteNumber.getInt();


                        if(ack == lastAck){
                            countRepeatedAck++;
                        }else if(countRepeatedAck <= 1){
                            countRepeatedAck--;
                            expectedAck++;
                        }
                        lastAck = ack;

                        System.out.println("Ack recebido " + ack + ".");

                        if(countRepeatedAck == 3){
                            countRepeatedAck = 0;
                            System.out.println("3 Acks Duplicados");
                            for(int i = expectedAck; i <= seqNum; i++){
                                System.out.println("reenviando pacote:" + i);
                               enviaPct(bufferPackage.get(i));
                            }
                        }

                        sem.release();
                    }
                } catch (IOException e) {
                    System.out.println("Excecao: " + e.getMessage());
                }
                break;
            //TODO timer
            default:
                break;
        }

    }
}