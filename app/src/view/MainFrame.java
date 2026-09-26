package view;

import util.ConnectionFactory;

import javax.swing.*;
import java.awt.*;

/** Janela principal com as abas do sistema. */
public class MainFrame extends JFrame {
    private final DashboardPanel dashboard = new DashboardPanel();
    private final ClientePanel clientes = new ClientePanel();
    private final ProdutoPanel produtos = new ProdutoPanel();
    private final FuncionarioPanel funcionarios = new FuncionarioPanel();
    private final ConsultasPanel consultas = new ConsultasPanel();
    private final GraficosEstatisticaPanel estatistica = new GraficosEstatisticaPanel();

    public MainFrame() {
        super("Hortifruti - Sistema Administrativo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 760));
        setSize(1380, 860);
        setLocationRelativeTo(null);

        JTabbedPane abas = new JTabbedPane();
        abas.setFont(UI.NORMAL.deriveFont(Font.BOLD));
        abas.addTab("Dashboard", dashboard);
        abas.addTab("Clientes", clientes);
        abas.addTab("Produtos", produtos);
        abas.addTab("Funcionarios", funcionarios);
        abas.addTab("Consultas", consultas);
        abas.addTab("Graficos Estatistica", estatistica);

        // recarrega os dados sempre que a aba e aberta
        abas.addChangeListener(e -> {
            Component atual = abas.getSelectedComponent();
            if (atual == dashboard) dashboard.carregar();
            else if (atual == clientes) clientes.carregar();
            else if (atual == produtos) produtos.carregar();
            else if (atual == funcionarios) funcionarios.carregar();
            else if (atual == estatistica) estatistica.carregar();
        });

        JLabel barra = new JLabel("  Conectado: " + ConnectionFactory.getUser() + " @ "
            + ConnectionFactory.getUrl().replaceFirst("\\?.*", ""));
        barra.setFont(UI.PEQUENA);
        barra.setForeground(UI.TEXTO_SUAVE);
        barra.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        add(abas, BorderLayout.CENTER);
        add(barra, BorderLayout.SOUTH);
        dashboard.carregar();
    }
}
