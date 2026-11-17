package view;

import dao.DashboardDAO;
import dao.DashboardDAO.Filtro;
import dao.DashboardDAO.Variavel;
import dao.ReferenciaDAO;
import model.ItemLista;
import util.Estatistica;
import util.Estatistica.Descritiva;
import view.chart.GraficoBarras;
import view.chart.GraficoColunas;
import view.chart.GraficoRosca;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Dashboard estatistico: tudo e calculado sobre os dados do banco e respeita os filtros
 * de periodo, categoria e forma de pagamento.
 */
public class DashboardPanel extends JPanel {
    private static final String[] FORMAS = {"Todas", "Dinheiro", "Cartao de Credito", "Cartao de Debito", "Pix", "Boleto"};

    private final DashboardDAO dao = new DashboardDAO();
    private final ReferenciaDAO referencias = new ReferenciaDAO();

    // filtros
    private final JSpinner dataInicio = UI.campoData(LocalDate.now());
    private final JSpinner dataFim = UI.campoData(LocalDate.now());
    private final JComboBox<ItemLista> categoria = new JComboBox<>();
    private final JComboBox<String> forma = new JComboBox<>(FORMAS);
    private final JLabel resumoFiltro = new JLabel(" ");
    private LocalDate[] periodoPadrao;

    // indicadores
    private final JLabel kPedidos = new JLabel("-"), kPedidosSub = sub();
    private final JLabel kFaturamento = new JLabel("-"), kFaturamentoSub = sub();
    private final JLabel kTicket = new JLabel("-"), kTicketSub = sub();
    private final JLabel kClientes = new JLabel("-"), kClientesSub = sub();
    private final JLabel kDevolucao = new JLabel("-"), kDevolucaoSub = sub();
    private final JLabel kPerdas = new JLabel("-"), kPerdasSub = sub();

    // estatistica descritiva
    private final JComboBox<Variavel> variavel = new JComboBox<>(Variavel.values());
    private final JLabel eN = valorEstat(), eMedia = valorEstat(), eMediana = valorEstat(), eModa = valorEstat(),
        eVariancia = valorEstat(), eDesvio = valorEstat(), eCv = valorEstat(), eMinMax = valorEstat();

    // graficos
    private final GraficoColunas gMensal = new GraficoColunas("Faturamento mensal (R$)", UI.VERDE);
    private final GraficoRosca gPagamento = new GraficoRosca("Faturamento por forma de pagamento");
    private final GraficoBarras gProdutos = new GraficoBarras("Top 10 produtos (qtd. vendida)", UI.PALETA[3]);
    private final GraficoBarras gCategorias = new GraficoBarras("Faturamento por categoria (R$)", UI.VERDE);
    private final GraficoBarras gPerdas = new GraficoBarras("Quantidade perdida por motivo", UI.PALETA[2]);
    private final GraficoRosca gDevolucoes = new GraficoRosca("Devolucoes por forma de resolucao");

    public DashboardPanel() {
        super(new BorderLayout(0, 10));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel cabecalho = new JPanel(new BorderLayout(0, 10));
        cabecalho.setOpaque(false);
        cabecalho.add(barraFiltros(), BorderLayout.NORTH);

        JPanel kpis = new JPanel(new GridLayout(1, 6, 10, 0));
        kpis.setOpaque(false);
        kpis.add(card("Pedidos", kPedidos, kPedidosSub));
        kpis.add(card("Faturamento", kFaturamento, kFaturamentoSub));
        kpis.add(card("Ticket medio", kTicket, kTicketSub));
        kpis.add(card("Clientes ativos", kClientes, kClientesSub));
        kpis.add(card("Pedidos c/ devolucao", kDevolucao, kDevolucaoSub));
        kpis.add(card("Prejuizo c/ perdas", kPerdas, kPerdasSub));

        JPanel indicadores = new JPanel(new BorderLayout(0, 10));
        indicadores.setOpaque(false);
        indicadores.add(kpis, BorderLayout.NORTH);
        indicadores.add(cardEstatistica(), BorderLayout.CENTER);
        cabecalho.add(indicadores, BorderLayout.CENTER);

        gMensal.setFormato(v -> UI.numero(Math.round(v)));
        gPagamento.setFormato(UI::moeda);
        gCategorias.setFormato(v -> UI.numero(Math.round(v)));
        gDevolucoes.setFormato(v -> String.valueOf((int) v));

        JPanel graficos = new JPanel(new GridLayout(2, 3, 10, 10));
        graficos.setOpaque(false);
        graficos.add(gMensal);
        graficos.add(gPagamento);
        graficos.add(gProdutos);
        graficos.add(gCategorias);
        graficos.add(gPerdas);
        graficos.add(gDevolucoes);

        add(cabecalho, BorderLayout.NORTH);
        add(graficos, BorderLayout.CENTER);

        variavel.addActionListener(e -> {
            try {
                carregarEstatistica(filtroAtual());
            } catch (IllegalArgumentException ignorado) {
                // filtro invalido: a mensagem aparece ao aplicar os filtros
            }
        });
    }

    // ------------------------------------------------------------------ layout

    private JPanel barraFiltros() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        barra.add(UI.titulo("Dashboard"), BorderLayout.WEST);

        JPanel campos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        campos.setOpaque(false);
        campos.add(new JLabel("De:"));
        campos.add(dataInicio);
        campos.add(new JLabel("Ate:"));
        campos.add(dataFim);
        campos.add(new JLabel("  Categoria:"));
        categoria.setPrototypeDisplayValue(new ItemLista(0, "Frutas Tropicais xxxxxx"));
        campos.add(categoria);
        campos.add(new JLabel("  Pagamento:"));
        campos.add(forma);
        JButton aplicar = UI.botao("Aplicar filtros", true);
        aplicar.addActionListener(e -> atualizarDados());
        JButton limpar = UI.botao("Limpar", false);
        limpar.addActionListener(e -> {
            limparFiltros();
            atualizarDados();
        });
        campos.add(aplicar);
        campos.add(limpar);
        barra.add(campos, BorderLayout.CENTER);

        resumoFiltro.setFont(UI.PEQUENA);
        resumoFiltro.setForeground(UI.TEXTO_SUAVE);
        barra.add(resumoFiltro, BorderLayout.SOUTH);
        return barra;
    }

    private JPanel cardEstatistica() {
        JPanel c = UI.cartao(new BorderLayout(14, 0));
        JPanel esquerda = new JPanel(new BorderLayout(0, 4));
        esquerda.setOpaque(false);
        JLabel t = new JLabel("ESTATISTICA DESCRITIVA");
        t.setFont(UI.PEQUENA.deriveFont(Font.BOLD));
        t.setForeground(UI.TEXTO_SUAVE);
        esquerda.add(t, BorderLayout.NORTH);
        esquerda.add(variavel, BorderLayout.CENTER);
        c.add(esquerda, BorderLayout.WEST);

        JPanel valores = new JPanel(new GridLayout(1, 8, 8, 0));
        valores.setOpaque(false);
        valores.add(estat("n (amostra)", eN));
        valores.add(estat("Media", eMedia));
        valores.add(estat("Mediana", eMediana));
        valores.add(estat("Moda", eModa));
        valores.add(estat("Variancia (s2)", eVariancia));
        valores.add(estat("Desvio padrao (s)", eDesvio));
        valores.add(estat("Coef. variacao", eCv));
        valores.add(estat("Minimo / Maximo", eMinMax));
        eModa.setFont(UI.NORMAL.deriveFont(Font.BOLD));
        eMinMax.setFont(UI.NORMAL.deriveFont(Font.BOLD));
        c.add(valores, BorderLayout.CENTER);
        return c;
    }

    private static JLabel sub() {
        JLabel l = new JLabel(" ");
        l.setFont(UI.PEQUENA);
        l.setForeground(UI.TEXTO_SUAVE);
        return l;
    }

    private static JLabel valorEstat() {
        JLabel l = new JLabel("-");
        l.setFont(UI.SUBTITULO);
        l.setForeground(UI.VERDE_ESCURO);
        return l;
    }

    private static JPanel estat(String rotulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel r = new JLabel(rotulo);
        r.setFont(UI.PEQUENA);
        r.setForeground(UI.TEXTO_SUAVE);
        p.add(r, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    private JPanel card(String rotulo, JLabel valor, JLabel subtitulo) {
        JPanel c = UI.cartao(new BorderLayout(0, 2));
        JLabel r = new JLabel(rotulo.toUpperCase());
        r.setFont(UI.PEQUENA.deriveFont(Font.BOLD));
        r.setForeground(UI.TEXTO_SUAVE);
        valor.setFont(new Font("SansSerif", Font.BOLD, 20));
        valor.setForeground(UI.VERDE_ESCURO);
        c.add(r, BorderLayout.NORTH);
        c.add(valor, BorderLayout.CENTER);
        c.add(subtitulo, BorderLayout.SOUTH);
        return c;
    }

    // ------------------------------------------------------------------ filtros

    private void limparFiltros() {
        if (periodoPadrao != null) {
            UI.definirData(dataInicio, periodoPadrao[0]);
            UI.definirData(dataFim, periodoPadrao[1]);
        }
        if (categoria.getItemCount() > 0) categoria.setSelectedIndex(0);
        forma.setSelectedIndex(0);
    }

    private Filtro filtroAtual() {
        LocalDate ini = UI.lerData(dataInicio);
        LocalDate fim = UI.lerData(dataFim);
        if (fim.isBefore(ini)) throw new IllegalArgumentException("A data final deve ser igual ou posterior a inicial.");
        ItemLista cat = (ItemLista) categoria.getSelectedItem();
        String f = forma.getSelectedIndex() <= 0 ? "%" : (String) forma.getSelectedItem();
        return new Filtro(ini, fim, f, cat == null ? 0 : cat.id());
    }

    /** Chamado ao abrir a aba: carrega as listas dos filtros (uma vez) e os dados. */
    public void carregar() {
        if (periodoPadrao == null) {
            try {
                periodoPadrao = referencias.periodoDosDados();
                categoria.removeAllItems();
                categoria.addItem(new ItemLista(0, "Todas"));
                for (ItemLista c : referencias.listarCategoriasComProdutos()) categoria.addItem(c);
                limparFiltros();
            } catch (Exception e) {
                periodoPadrao = null;
                UI.erro(this, e);
                return;
            }
        }
        atualizarDados();
    }

    // ------------------------------------------------------------------ dados

    /** Carrega indicadores, estatistica e graficos em segundo plano (a interface nao trava). */
    private void atualizarDados() {
        Filtro f;
        try {
            f = filtroAtual();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return;
        }
        resumoFiltro.setText("Filtro: " + UI.dataBr(f.inicio()) + " a " + UI.dataBr(f.fim())
            + "  |  categoria: " + categoria.getSelectedItem()
            + "  |  pagamento: " + forma.getSelectedItem()
            + "   (perdas usam a data da perda e ignoram a forma de pagamento)");
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {
                    dao.indicadores(f),
                    dao.faturamentoMensal(f),
                    dao.faturamentoPorFormaPagamento(f),
                    dao.topProdutosVendidos(f),
                    dao.faturamentoPorCategoria(f),
                    dao.perdasPorMotivo(f),
                    dao.devolucoesPorResolucao(f)
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] r = get();
                    mostrarIndicadores((DashboardDAO.Indicadores) r[0]);
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
        carregarEstatistica(f);
    }

    private void mostrarIndicadores(DashboardDAO.Indicadores k) {
        kPedidos.setText(String.valueOf(k.pedidos()));
        kPedidosSub.setText("no periodo filtrado");
        kFaturamento.setText(UI.moeda(k.faturamento()));
        kFaturamentoSub.setText("soma dos itens vendidos");
        kTicket.setText(UI.moeda(k.ticketMedio()));
        kTicketSub.setText("faturamento / pedidos");
        kClientes.setText(k.clientesAtivos() + " de " + k.totalClientes());
        kClientesSub.setText(String.format("%.1f%% da base de clientes", k.percClientesAtivos()));
        kDevolucao.setText(String.format("%.1f%%", k.percPedidosComDevolucao()));
        kDevolucaoSub.setText("dos pedidos do filtro");
        kPerdas.setText(UI.moeda(k.prejuizoPerdas()));
        kPerdasSub.setText(String.format("%.1f%% do faturamento", k.percPerdaSobreVendas()));
    }

    private void carregarEstatistica(Filtro f) {
        Variavel v = (Variavel) variavel.getSelectedItem();
        if (v == null || periodoPadrao == null) return;
        new SwingWorker<List<Double>, Void>() {
            @Override
            protected List<Double> doInBackground() throws Exception {
                return dao.amostra(v, f);
            }

            @Override
            protected void done() {
                try {
                    DoubleFunction<String> fmt = v.moeda ? UI::moeda : UI::numero;
                    Descritiva d = Estatistica.descritiva(get(), UI::numero);
                    boolean vazio = d.n() == 0;
                    eN.setText(String.valueOf(d.n()));
                    eMedia.setText(vazio ? "-" : fmt.apply(d.media()));
                    eMediana.setText(vazio ? "-" : fmt.apply(d.mediana()));
                    eModa.setText("<html>" + d.moda() + "</html>");
                    eModa.setToolTipText(d.moda());
                    eVariancia.setText(vazio ? "-" : UI.numero(d.variancia()));
                    eDesvio.setText(vazio ? "-" : fmt.apply(d.desvioPadrao()));
                    eCv.setText(vazio || Double.isNaN(d.coefVariacao()) ? "-" : String.format("%.1f%%", d.coefVariacao()));
                    eMinMax.setText(vazio ? "-" : "<html>" + fmt.apply(d.minimo()) + "<br>" + fmt.apply(d.maximo()) + "</html>");
                } catch (Exception e) {
                    UI.erro(DashboardPanel.this, e.getCause() instanceof Exception c ? c : e);
                }
            }
        }.execute();
    }
}
