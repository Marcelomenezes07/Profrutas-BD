package view;

import util.ConnectionFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;

/** Tela inicial para informar os dados de acesso ao MySQL e testar a conexao. */
public class ConexaoDialog extends JDialog {
    private final JTextField url = new JTextField(ConnectionFactory.getUrl(), 34);
    private final JTextField usuario = new JTextField(ConnectionFactory.getUser());
    private final JPasswordField senha = new JPasswordField(ConnectionFactory.getPass());
    private boolean conectado = false;

    public ConexaoDialog() {
        super((Frame) null, "Conectar ao MySQL", true);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(18, 18, 18, 18));
        p.setBackground(UI.FUNDO);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = UI.titulo("Hortifruti - Sistema Administrativo");
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        p.add(titulo, c);
        JLabel sub = new JLabel("Informe os dados de acesso ao banco 'hortifruti'");
        sub.setForeground(UI.TEXTO_SUAVE);
        c.gridy = 1;
        p.add(sub, c);

        c.gridwidth = 1;
        adicionar(p, c, 2, "URL JDBC:", url);
        adicionar(p, c, 3, "Usuario:", usuario);
        adicionar(p, c, 4, "Senha:", senha);

        JButton conectar = UI.botao("Conectar", true);
        JButton sair = UI.botao("Sair", false);
        conectar.addActionListener(e -> conectar());
        sair.addActionListener(e -> dispose());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(sair);
        botoes.add(conectar);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        c.insets = new Insets(14, 5, 0, 5);
        p.add(botoes, c);

        getRootPane().setDefaultButton(conectar);
        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(senha::requestFocusInWindow);
    }

    private void adicionar(JPanel p, GridBagConstraints c, int linha, String rotulo, JComponent campo) {
        c.gridx = 0; c.gridy = linha; c.weightx = 0;
        p.add(new JLabel(rotulo), c);
        c.gridx = 1; c.weightx = 1;
        p.add(campo, c);
    }

    private void conectar() {
        ConnectionFactory.configurar(url.getText().trim(), usuario.getText().trim(), new String(senha.getPassword()));
        try (Connection ignored = ConnectionFactory.getConnection()) {
            conectado = true;
            dispose();
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    public boolean isConectado() { return conectado; }
}
