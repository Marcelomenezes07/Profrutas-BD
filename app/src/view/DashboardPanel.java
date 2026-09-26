package view;

import dao.DashboardDAO;
import view.chart.GraficoBarras;
import view.chart.GraficoColunas;
import view.chart.GraficoRosca;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/** Dashboard: indicadores (cards) e graficos gerados a partir do banco. */
public class DashboardPanel extends JPanel {
    private final DashboardDAO dao = new DashboardDAO();

    private final JLabel kpiClientes = new JLabel("-");
    private final JLabel kpiPedidos = new JLabel("-");
    private final JLabel kpiFaturamento = new JLabel("-");
    private final JLabel kpiTicket = new JLabel("-");
    private final JLabel kpiPerdas = new JLabel("-");
    private final JLabel kpiDevolucoes = new JLabel("-");

    private final GraficoColunas gMensal = new GraficoColunas("Faturamento mensal (R$)", UI.VERDE);
    private final GraficoRosca gPagamento = new GraficoRosca("Faturamento por forma de pagamento");
    private final GraficoBarras gProdutos = new GraficoBarras("Top 10 produtos (qtd. vendida)", UI.PALETA[3]);
    private final GraficoBarras gCategorias = new GraficoBarras("Faturamento por categoria (R$)", UI.VERDE);
    private final GraficoBarras gPerdas = new GraficoBarras("Quantidade perdida por motivo", UI.PALETA[2]);
    private final GraficoRosca gDevolucoes = new GraficoRosca("Devolucoes por forma de resolucao");

    public DashboardPanel() {
        super(new BorderLayout(0, 12));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(UI.titulo("Dashboard"), BorderLayout.WEST);
        JButton atualizar = UI.botao("Atualizar", true);
        atualizar.addActionListener(e -> carregar());
        topo.add(atualizar, BorderLayout.EAST);

        JPanel kpis = new JPanel(new GridLayout(1, 6, 12, 0));
        kpis.setOpaque(false);
        kpis.add(card("Clientes", kpiClientes));
        kpis.add(card("Pedidos", kpiPedidos));
        kpis.add(card("Faturamento", kpiFaturamento));
        kpis.add(card("Ticket medio", kpiTicket));
        kpis.add(card("Prejuizo c/ perdas", kpiPerdas));
        kpis.add(card("Devolucoes", kpiDevolucoes));

        JPanel cabecalho = new JPanel(new BorderLayout(0, 12));
        cabecalho.setOpaque(false);
        cabecalho.add(topo, BorderLayout.NORTH);
        cabecalho.add(kpis, BorderLayout.CENTER);

        gMensal.setFormato(v -> UI.numero(Math.round(v)));
        gPagamento.setFormato(UI::moeda);
        gCategorias.setFormato(v -> UI.numero(Math.round(v)));
        gDevolucoes.setFormato(v -> String.valueOf((int) v));

        JPanel graficos = new JPanel(new GridLayout(2, 3, 12, 12));
        graficos.setOpaque(false);
        graficos.add(gMensal);
        graficos.add(gPagamento);
        graficos.add(gProdutos);
        graficos.add(gCategorias);
        graficos.add(gPerdas);
        graficos.add(gDevolucoes);

        add(cabecalho, BorderLayout.NORTH);
        add(graficos, BorderLayout.CENTER);
    }

    private JPanel card(String rotulo, JLabel valor) {
        JPanel c = UI.cartao(new BorderLayout(0, 4));
        JLabel r = new JLabel(rotulo.toUpperCase());
        r.setFont(UI.PEQUENA.deriveFont(Font.BOLD));
        r.setForeground(UI.TEXTO_SUAVE);
        valor.setFont(new Font("SansSerif", Font.BOLD, 22));
        valor.setForeground(UI.VERDE_ESCURO);
        c.add(r, BorderLayout.NORTH);
        c.add(valor, BorderLayout.CENTER);
        return c;
    }

    /** Carrega os dados em segundo plano para nao travar a interface. */
    public void carregar() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {
                    dao.indicadores(),
                    dao.faturamentoMensal(),
                    dao.faturamentoPorFormaPagamento(),
                    dao.topProdutosVendidos(),
                    dao.faturamentoPorCategoria(),
                    dao.perdasPorMotivo(),
                    dao.devolucoesPorResolucao()
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] r = get();
                    DashboardDAO.Indicadores k = (DashboardDAO.Indicadores) r[0];
                    kpiClientes.setText(String.valueOf(k.clientes()));
                    kpiPedidos.setText(String.valueOf(k.pedidos()));
                    kpiFaturamento.setText(UI.moeda(k.faturamento()));
                    kpiTicket.setText(UI.moeda(k.ticketMedio()));
                    kpiPerdas.setText(UI.moeda(k.prejuizoPerdas()));
                    kpiDevolucoes.setText(String.valueOf(k.devolucoes()));
                    gMensal.setDados((Map<String, Double>) r[1]);
                    gPagamento.setDados((Map<String, Double>) r[2]);
                    gProdutos.setDados((Map<String, Double>) r[3]);
                    gCategorias.setDados((Map<String, Double>) r[4]);
                    gPerdas.setDados((Map<String, Double>) r[5]);
                    gDevolucoes.setDados((Map<String, Double>) r[6]);
                } catch (Exception e) {
                    UI.erro(DashboardPanel.this, e.getCause() instanceof Exception c ? c : e);
                }
            }
        }.execute();
    }
}
