package main;

import view.ConexaoDialog;
import view.MainFrame;
import view.UI;

import javax.swing.*;

public class Exec {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UI.aplicarTema();
            ConexaoDialog login = new ConexaoDialog();
            login.setVisible(true);
            if (login.isConectado()) {
                new MainFrame().setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
