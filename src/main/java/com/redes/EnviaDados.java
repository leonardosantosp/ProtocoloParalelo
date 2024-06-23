package com.redes;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
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
    private static final int windowSize = 4; // Tamanho da janela deslizante
    private static boolean[] ackReceived = new boolean[windowSize];
    private static int[][] dadosParaEnvio = new int[windowSize][350];
    private static Timer timer = new Timer(true);
    private static boolean finished = false;

    public EnviaDados(Semaphore sem, String funcao) {
        super(funcao);
        this.sem = sem;
        this.funcao = funcao;
    }

    public String getFuncao() {
        return funcao;
    }

    private void enviaPct(int[] dados, int seqNum) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(dados.length * 4 + 4);
        byteBuffer.putInt(seqNum);
        for (int dado : dados) {
            byteBuffer.putInt(dado);
        }

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
            }

            System.out.println("Envio feito. SeqNum: " + seqNum);
        } catch (SocketException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startTimer() {
        TimerTask resendTask = new TimerTask() {
            @Override
            public void run() {
                if (finished) {
                    timer.cancel();
                    return;
                }
                for (int i = 0; i < windowSize; i++) {
                    if (!ackReceived[i]) {
                        System.out.println("Reenviando pacote. SeqNum: " + (seqNum + i));
                        enviaPct(dadosParaEnvio[i], (seqNum + i) % windowSize);
                    }
                }
            }
        };
        timer.schedule(resendTask, 5000, 5000); // Reenvio a cada 5 segundos
    }

    @Override
    public void run() {
        switch (this.getFuncao()) {
            case "envia":
                int[] dados = new int[350];
                int cont = 0;
                try (FileInputStream fileInput = new FileInputStream("entrada")) {
                    int lido;
                    while ((lido = fileInput.read()) != -1) {
                        dados[cont] = lido;
                        cont++;
                        if (cont == 350) {
                            System.arraycopy(dados, 0, dadosParaEnvio[seqNum % windowSize], 0, 350);
                            enviaPct(dadosParaEnvio[seqNum % windowSize], seqNum);
                            seqNum++;
                            cont = 0;
                        }
                    }

                    if (cont > 0) {
                        for (int i = cont; i < 350; i++)
                            dados[i] = -1;
                        System.arraycopy(dados, 0, dadosParaEnvio[seqNum % windowSize], 0, 350);
                        enviaPct(dadosParaEnvio[seqNum % windowSize], seqNum);
                        seqNum++;
                    }

                    finished = true;
                    timer.cancel(); // Cancelar o timer após o envio de todos os pacotes

                } catch (IOException e) {
                    System.out.println("Error message: " + e.getMessage());
                }
                break;
            case "ack":
                try (DatagramSocket serverSocket = new DatagramSocket(portaLocalRecebimento)) {
                    byte[] receiveData = new byte[10];
                    String retorno = "";
                    while (!retorno.equals("F")) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        retorno = new String(receivePacket.getData()).trim();
                        System.out.println("Ack recebido: " + retorno);
                        if (retorno.startsWith("A")) {
                            int ackNum = Integer.parseInt(retorno.substring(1).trim());
                            ackReceived[ackNum % windowSize] = true;
                            sem.release();
                        }
                    }
                    finished = true;
                } catch (IOException e) {
                    System.out.println("Excecao: " + e.getMessage());
                }
                break;
            case "timer":
                startTimer(); // Start the timer when "timer" case is executed
                break;
            default:
                break;
        }
    }
}
