package com.redes;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Maq2 {
    public static void main(String[] args) {

        Semaphore sem = new Semaphore(3);
        EnviaDados ed1 = new EnviaDados(sem, "envia");
        EnviaDados ed2 = new EnviaDados(sem, "ack");
        EnviaDados timerThread = new EnviaDados(sem, "timer");

        ed2.start();
        ed1.start();
        timerThread.start();

        try {
            ed1.join();
            ed2.join();
            timerThread.join();

        } catch (InterruptedException ex) {
            Logger.getLogger(Maq2.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.exit(0); // Encerrar o programa explicitamente
    }
}
