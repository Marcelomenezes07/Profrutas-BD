package view;

import dao.ConsultaDAO;
import dao.Consultas;
import model.Consulta;
import model.ResultadoConsulta;
import view.chart.GraficoBarras;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Consultas e visoes: SQL executado, filtros (parametros), tabela de resultado,
 * indicadores (linhas, soma, media, minimo, maximo) e grafico do resultado.
 */
public class ConsultasPanel extends JPanel {
    private static final int MAX_BARRAS = 15;

    private final ConsultaDAO dao = new ConsultaDAO();

    private final JList<Consulta> lista = new JList<>(Consultas.TODAS.toArray(new Consulta[0]));
    private final JLabel tituloConsulta = new JLabel();
    private final JTextArea descricao = new JTextArea();
    private final JTextArea sql = new JTextArea();
    private final JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    private final List<Supplier<String>> leitoresFiltro = new ArrayList<>();
    private final JLabel status = new JLabel(" ");

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return UI.classeColuna(this, c); }
    };
    private final JTable tabela = new JTable(modelo);

    // indicadores e grafico do resultado
    private final JPanel indicadores = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JComboBox<String> colunaRotulo = new JComboBox<>();
    private final JComboBox<String> colunaValor = new JComboBox<>();
    private final GraficoBarras grafico = new GraficoBarras("Grafico do resultado", UI.PALETA[3]);
    private ResultadoConsulta ultimo;
    private boolean ajustandoCombos = false;

    public ConsultasPanel() {
        super(new BorderLayout(12, 12));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        lista.setFont(UI.NORMAL);
        lista.setFixedCellHeight(30);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selecionar(lista.getSelectedValue());
        });
        JPanel esquerda = UI.cartao(new BorderLayout(0, 8));
        JLabel l = new JLabel("Consultas e visoes");
        l.setFont(UI.SUBTITULO);
        esquerda.add(l, BorderLayout.NORTH);
        esquerda.add(new JScrollPane(lista), BorderLayout.CENTER);
        esquerda.setPreferredSize(new Dimension(330, 0));

        tituloConsulta.setFont(UI.TITULO);
        descricao.setFont(UI.NORMAL);
        descricao.setLineWrap(true);
        descricao.setWrapStyleWord(true);
        descricao.setEditable(false);
        descricao.setOpaque(false);
        sql.setFont(UI.MONO);
        sql.setEditable(false);
        sql.setBackground(new Color(0x263238));
        sql.setForeground(new Color(0xE0F2F1));
        sql.setCaretColor(Color.WHITE);
        sql.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane sqlScroll = new JScrollPane(sql);
        sqlScroll.setPreferredSize(new Dimension(0, 150));

        JButton executar = UI.botao("Executar", true);
        executar.addActionListener(e -> executar());
        JButton exportar = UI.botao("Exportar CSV", false);
        exportar.addActionListener(e -> exportarCsv());
        filtros.setOpaque(false);
        JPanel acoes = new JPanel(new BorderLayout(8, 6));
        acoes.setOpaque(false);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botoes.setOpaque(false);
        status.setForeground(UI.TEXTO_SUAVE);
        botoes.add(status);
        botoes.add(executar);
        botoes.add(exportar);
        JLabel rotuloFiltros = new JLabel("Filtros:");
        rotuloFiltros.setFont(UI.NORMAL.deriveFont(Font.BOLD));
        acoes.add(rotuloFiltros, BorderLayout.WEST);
        acoes.add(filtros, BorderLayout.CENTER);
        acoes.add(botoes, BorderLayout.SOUTH);

        JPanel cabecalho = new JPanel(new BorderLayout(0, 6));
        cabecalho.setOpaque(false);
        cabecalho.add(tituloConsulta, BorderLayout.NORTH);
        cabecalho.add(descricao, BorderLayout.CENTER);

        JPanel detalhe = UI.cartao(new BorderLayout(0, 8));
        detalhe.add(cabecalho, BorderLayout.NORTH);
        detalhe.add(sqlScroll, BorderLayout.CENTER);
        detalhe.add(acoes, BorderLayout.SOUTH);

        // resultado: indicadores + tabela + grafico
        UI.estilizarTabela(tabela);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        indicadores.setOpaque(false);

        JPanel controlesGrafico = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controlesGrafico.setOpaque(false);
        controlesGrafico.add(new JLabel("Agrupar por:"));
        controlesGrafico.add(colunaRotulo);
        controlesGrafico.add(new JLabel("Valor:"));
        controlesGrafico.add(colunaValor);
        colunaRotulo.addActionListener(e -> { if (!ajustandoCombos) atualizarGraficoEIndicadores(); });
        colunaValor.addActionListener(e -> { if (!ajustandoCombos) atualizarGraficoEIndicadores(); });
        JPanel painelGrafico = new JPanel(new BorderLayout(0, 6));
        painelGrafico.setOpaque(false);
        painelGrafico.add(controlesGrafico, BorderLayout.NORTH);
        painelGrafico.add(grafico, BorderLayout.CENTER);

        JSplitPane tabelaGrafico = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tabela), painelGrafico);
        tabelaGrafico.setResizeWeight(0.6);
        tabelaGrafico.setBorder(null);

        JPanel resultado = UI.cartao(new BorderLayout(0, 8));
        resultado.add(indicadores, BorderLayout.NORTH);
        resultado.add(tabelaGrafico, BorderLayout.CENTER);

        JSplitPane direita = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detalhe, resultado);
        direita.setResizeWeight(0.38);
        direita.setBorder(null);
        direita.setOpaque(false);

        add(esquerda, BorderLayout.WEST);
        add(direita, BorderLayout.CENTER);
        lista.setSelectedIndex(0);
    }

    // ------------------------------------------------------------------ selecao e filtros

    private void selecionar(Consulta c) {
        if (c == null) return;
        tituloConsulta.setText(c.getTitulo());
        descricao.setText(c.getDescricao());
        sql.setText(c.getSql());
        sql.setCaretPosition(0);
        montarFiltros(c);
        limparResultado();
        status.setText(c.temParametro() ? "Ajuste os filtros e clique em Executar" : "Clique em Executar");
        revalidate();
        repaint();
    }

    /** Cria um campo para cada parametro da consulta (lista, data ou texto). */
    private void montarFiltros(Consulta c) {
        filtros.removeAll();
        leitoresFiltro.clear();
        if (!c.temParametro()) {
            JLabel sem = new JLabel("Esta consulta nao possui filtros.");
            sem.setForeground(UI.TEXTO_SUAVE);
            filtros.add(sem);
        }
        for (Consulta.Parametro p : c.getParametros()) {
            filtros.add(new JLabel(p.rotulo() + ":"));
            if (p.opcoes().length > 0) {
                JComboBox<String> combo = new JComboBox<>(p.opcoes());
                combo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                  boolean selected, boolean focus) {
                        return super.getListCellRendererComponent(list, "%".equals(value) ? "(todos)" : value,
                            index, selected, focus);
                    }
                });
                combo.setSelectedItem(p.valorPadrao());
                filtros.add(combo);
                leitoresFiltro.add(() -> (String) combo.getSelectedItem());
            } else if (p.tipo() == Consulta.TipoParametro.DATA) {
                JSpinner data = UI.campoData(LocalDate.parse(p.valorPadrao()));
                filtros.add(data);
                leitoresFiltro.add(() -> UI.lerData(data).toString());
            } else {
                JTextField campo = new JTextField(p.valorPadrao(), p.tipo() == Consulta.TipoParametro.INTEIRO ? 4 : 10);
                campo.addActionListener(e -> executar());
                filtros.add(campo);
                leitoresFiltro.add(campo::getText);
            }
        }
        filtros.revalidate();
        filtros.repaint();
    }

    // ------------------------------------------------------------------ execucao

    private void executar() {
        Consulta c = lista.getSelectedValue();
        if (c == null) return;
        List<String> valores = new ArrayList<>();
        try {
            for (int i = 0; i < leitoresFiltro.size(); i++) {
                String v = leitoresFiltro.get(i).get();
                Consulta.Parametro p = c.getParametros().get(i);
                if (p.tipo() == Consulta.TipoParametro.INTEIRO && !v.trim().matches("-?\\d+")) {
                    throw new IllegalArgumentException("Informe um numero inteiro em \"" + p.rotulo() + "\".");
                }
                valores.add(v);
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return;
        }
        status.setText("Executando...");
        new SwingWorker<ResultadoConsulta, Void>() {
            @Override
            protected ResultadoConsulta doInBackground() throws Exception {
                return dao.executar(c, valores);
            }

            @Override
            protected void done() {
                try {
                    ResultadoConsulta r = get();
                    ultimo = r;
                    modelo.setDataVector(r.linhas().toArray(new Object[0][]), r.colunas().toArray());
                    ajustarColunas();
                    prepararGrafico(r);
                    status.setText(r.linhas().size() + " linha(s) em " + r.tempoMs() + " ms");
                } catch (Exception e) {
                    status.setText("Erro");
                    UI.erro(ConsultasPanel.this, e.getCause() instanceof Exception x ? x : e);
                }
            }
        }.execute();
    }

    private void limparResultado() {
        ultimo = null;
        modelo.setDataVector(new Object[0][0], new Object[0]);
        ajustandoCombos = true;
        colunaRotulo.removeAllItems();
        colunaValor.removeAllItems();
        ajustandoCombos = false;
        grafico.setTitulo("Grafico do resultado");
        grafico.setDados(new LinkedHashMap<>());
        indicadores.removeAll();
        indicadores.revalidate();
        indicadores.repaint();
    }

    // ------------------------------------------------------------------ indicadores e grafico

    /** Escolhe as colunas padrao do grafico: um texto como rotulo e um valor numerico. */
    private void prepararGrafico(ResultadoConsulta r) {
        ajustandoCombos = true;
        colunaRotulo.removeAllItems();
        colunaValor.removeAllItems();
        String rotuloPadrao = null, valorPadrao = null;
        for (int c = 0; c < r.colunas().size(); c++) {
            String nome = r.colunas().get(c);
            Object v = primeiroValor(r, c);
            colunaRotulo.addItem(nome);
            if (v instanceof Number) {
                colunaValor.addItem(nome);
                if (valorPadrao == null || prioridade(nome) < prioridade(valorPadrao)) valorPadrao = nome;
            } else if (rotuloPadrao == null && v instanceof String) {
                rotuloPadrao = nome;
            }
        }
        if (rotuloPadrao == null && !r.colunas().isEmpty()) rotuloPadrao = r.colunas().get(0);
        colunaRotulo.setSelectedItem(rotuloPadrao);
        if (valorPadrao != null) colunaValor.setSelectedItem(valorPadrao);
        ajustandoCombos = false;
        atualizarGraficoEIndicadores();
    }

    private void atualizarGraficoEIndicadores() {
        indicadores.removeAll();
        if (ultimo == null) return;
        indicadores.add(chip("Linhas", String.valueOf(ultimo.linhas().size())));

        String rotulo = (String) colunaRotulo.getSelectedItem();
        String valor = (String) colunaValor.getSelectedItem();
        if (rotulo == null || valor == null) {
            grafico.setTitulo("Sem coluna numerica para o grafico");
            grafico.setDados(new LinkedHashMap<>());
            indicadores.revalidate();
            indicadores.repaint();
            return;
        }
        int iRotulo = ultimo.colunas().indexOf(rotulo);
        int iValor = ultimo.colunas().indexOf(valor);

        double soma = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        int n = 0;
        Map<String, Double> grupos = new LinkedHashMap<>();
        for (Object[] linha : ultimo.linhas()) {
            if (!(linha[iValor] instanceof Number num)) continue;
            double v = num.doubleValue();
            soma += v;
            min = Math.min(min, v);
            max = Math.max(max, v);
            n++;
            String chave = linha[iRotulo] == null ? "(vazio)" : String.valueOf(linha[iRotulo]).trim();
            grupos.merge(chave, v, Double::sum);
        }
        if (n > 0) {
            indicadores.add(chip("Soma de " + valor, UI.numero(soma)));
            indicadores.add(chip("Media", UI.numero(soma / n)));
            indicadores.add(chip("Minimo", UI.numero(min)));
            indicadores.add(chip("Maximo", UI.numero(max)));
            indicadores.add(chip(rotulo + " distintos", String.valueOf(grupos.size())));
        }

        // mantem as MAX_BARRAS maiores, preservando a ordem original
        Map<String, Double> dados = grupos;
        if (grupos.size() > MAX_BARRAS) {
            Set<String> maiores = new HashSet<>();
            grupos.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_BARRAS).forEach(e -> maiores.add(e.getKey()));
            dados = new LinkedHashMap<>();
            for (Map.Entry<String, Double> e : grupos.entrySet()) {
                if (maiores.contains(e.getKey())) dados.put(e.getKey(), e.getValue());
            }
        }
        grafico.setTitulo(valor + " por " + rotulo + (grupos.size() > MAX_BARRAS ? " (" + MAX_BARRAS + " maiores)" : ""));
        grafico.setDados(dados);
        indicadores.revalidate();
        indicadores.repaint();
    }

    /** Colunas de valor monetario tem preferencia no grafico; depois quantidades; codigos por ultimo. */
    private static int prioridade(String coluna) {
        String[] preferidas = {"faturamento", "total", "subtotal", "valor", "gasto", "perd", "qtd", "quantidade"};
        String c = coluna.toLowerCase();
        for (int i = 0; i < preferidas.length; i++) if (c.contains(preferidas[i])) return i;
        return c.startsWith("nr_") || c.startsWith("cd_") || c.startsWith("id") ? 99 : 50;
    }

    private static Object primeiroValor(ResultadoConsulta r, int coluna) {
        for (Object[] linha : r.linhas()) if (linha[coluna] != null) return linha[coluna];
        return null;
    }

    private static JComponent chip(String rotulo, String valor) {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(UI.VERDE_CLARO);
        p.setBorder(new EmptyBorder(4, 10, 4, 10));
        JLabel r = new JLabel(rotulo.toUpperCase());
        r.setFont(UI.PEQUENA.deriveFont(Font.BOLD, 10f));
        r.setForeground(UI.TEXTO_SUAVE);
        JLabel v = new JLabel(valor);
        v.setFont(UI.SUBTITULO);
        v.setForeground(UI.VERDE_ESCURO);
        p.add(r, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private void ajustarColunas() {
        FontMetrics fm = tabela.getFontMetrics(tabela.getFont());
        FontMetrics fmH = tabela.getFontMetrics(tabela.getTableHeader().getFont());
        for (int c = 0; c < tabela.getColumnCount(); c++) {
            int w = fmH.stringWidth(tabela.getColumnName(c)) + 24;
            for (int r = 0; r < Math.min(tabela.getRowCount(), 200); r++) {
                Object v = tabela.getValueAt(r, c);
                if (v != null) w = Math.max(w, fm.stringWidth(v.toString()) + 16);
            }
            tabela.getColumnModel().getColumn(c).setPreferredWidth(Math.min(w, 420));
        }
    }

    private void exportarCsv() {
        if (modelo.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(this, "Execute uma consulta antes de exportar.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("consulta.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter w = new PrintWriter(fc.getSelectedFile(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < modelo.getColumnCount(); c++) {
                if (c > 0) sb.append(';');
                sb.append(modelo.getColumnName(c));
            }
            w.println(sb);
            for (int r = 0; r < modelo.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < modelo.getColumnCount(); c++) {
                    if (c > 0) sb.append(';');
                    Object v = modelo.getValueAt(r, c);
                    sb.append(v == null ? "" : '"' + v.toString().replace("\"", "\"\"") + '"');
                }
                w.println(sb);
            }
            status.setText("Exportado para " + fc.getSelectedFile().getName());
        } catch (IOException e) {
            UI.erro(this, e);
        }
    }
}
