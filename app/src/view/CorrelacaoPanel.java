package view;

import dao.CorrelacaoDAO;
import dao.CorrelacaoDAO.Par;
import dao.CorrelacaoDAO.Ponto;
import util.Estatistica;
import util.Estatistica.Correlacao;
import view.chart.GraficoDispersao;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Aba de correlacao: grafico de dispersao + Pearson + reta de regressao. */
public class CorrelacaoPanel extends JPanel {
    private final CorrelacaoDAO dao = new CorrelacaoDAO();
    private final JComboBox<Par> pares = new JComboBox<>(CorrelacaoDAO.PARES.toArray(new Par[0]));
    private final GraficoDispersao grafico = new GraficoDispersao();
    private final JTextArea explicacao = new JTextArea();
    private final JTextArea sql = new JTextArea();

    public CorrelacaoPanel() {
        super(new BorderLayout(0, 12));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel topo = new JPanel(new BorderLayout(12, 0));
        topo.setOpaque(false);
        topo.add(UI.titulo("Correlacao (dispersao + Pearson)"), BorderLayout.WEST);
        JPanel seletor = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        seletor.setOpaque(false);
        seletor.add(new JLabel("Variaveis:"));
        pares.setFont(UI.NORMAL);
        seletor.add(pares);
        JButton atualizar = UI.botao("Atualizar", true);
        seletor.add(atualizar);
        topo.add(seletor, BorderLayout.EAST);
        pares.addActionListener(e -> carregar());
        atualizar.addActionListener(e -> carregar());

        explicacao.setEditable(false);
        explicacao.setLineWrap(true);
        explicacao.setWrapStyleWord(true);
        explicacao.setFont(UI.NORMAL);
        explicacao.setOpaque(false);
        sql.setEditable(false);
        sql.setFont(UI.MONO);
        sql.setBackground(new Color(0x263238));
        sql.setForeground(new Color(0xE0F2F1));
        sql.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel lateral = UI.cartao(new BorderLayout(0, 10));
        JLabel l = new JLabel("Interpretacao");
        l.setFont(UI.SUBTITULO);
        JPanel texto = new JPanel(new BorderLayout(0, 6));
        texto.setOpaque(false);
        texto.add(l, BorderLayout.NORTH);
        texto.add(explicacao, BorderLayout.CENTER);
        lateral.add(texto, BorderLayout.NORTH);
        JScrollPane sqlScroll = new JScrollPane(sql);
        sqlScroll.setBorder(BorderFactory.createTitledBorder("SQL que gera os pontos"));
        lateral.add(sqlScroll, BorderLayout.CENTER);
        lateral.setPreferredSize(new Dimension(380, 0));

        add(topo, BorderLayout.NORTH);
        add(grafico, BorderLayout.CENTER);
        add(lateral, BorderLayout.EAST);
    }

    public void carregar() {
        Par par = (Par) pares.getSelectedItem();
        if (par == null) return;
        sql.setText(par.sql().strip());
        sql.setCaretPosition(0);
        new SwingWorker<List<Ponto>, Void>() {
            @Override
            protected List<Ponto> doInBackground() throws Exception {
                return dao.pontos(par);
            }

            @Override
            protected void done() {
                try {
                    grafico.setDados(par.titulo(), par.eixoX(), par.eixoY(), get());
                    explicacao.setText(interpretar(par, grafico.getCorrelacao()));
                } catch (Exception e) {
                    UI.erro(CorrelacaoPanel.this, e.getCause() instanceof Exception x ? x : e);
                }
            }
        }.execute();
    }

    private static String interpretar(Par par, Correlacao c) {
        if (c.n() < 2 || Double.isNaN(c.pearson())) {
            return "Dados insuficientes para calcular a correlacao.";
        }
        String sentido = c.b() >= 0 ? "aumenta" : "diminui";
        return String.format(
            "X = %s%nY = %s%n%n"
            + "r = %.4f  ->  %s.%n%n"
            + "R² = %.4f: cerca de %.1f%% da variacao de Y e explicada linearmente por X.%n%n"
            + "Reta de regressao (vermelha): y = %.3f %s %.3f·x. "
            + "Em media, a cada 1 unidade a mais em X, Y %s %s.%n%n"
            + "Media de X = %s | Media de Y = %s | n = %d%n%n"
            + "Passe o mouse sobre um ponto para ver seus valores.",
            par.eixoX(), par.eixoY(),
            c.pearson(), Estatistica.interpretar(c.pearson()),
            c.r2(), c.r2() * 100,
            c.a(), c.b() < 0 ? "-" : "+", Math.abs(c.b()),
            sentido, UI.numero(Math.abs(c.b())),
            UI.numero(c.mediaX()), UI.numero(c.mediaY()), c.n());
    }
}
