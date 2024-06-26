package com.redes;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Maq1 {
    public static void main(String[] args) {
        RecebeDados rd = new RecebeDados();
        rd.start();

        try {
            rd.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Maq1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
