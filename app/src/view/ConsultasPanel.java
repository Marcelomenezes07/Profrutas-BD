package view;

import dao.ConsultaDAO;
import dao.Consultas;
import model.Consulta;
import model.ResultadoConsulta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/** Lista das consultas pre-definidas, com o SQL executado e a tabela de resultados. */
public class ConsultasPanel extends JPanel {
    private final ConsultaDAO dao = new ConsultaDAO();

    private final JList<Consulta> lista = new JList<>(Consultas.TODAS.toArray(new Consulta[0]));
    private final JLabel tituloConsulta = new JLabel();
    private final JTextArea descricao = new JTextArea();
    private final JTextArea sql = new JTextArea();
    private final JLabel rotuloParametro = new JLabel();
    private final JTextField parametro = new JTextField(12);
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return UI.classeColuna(this, c); }
    };
    private final JTable tabela = new JTable(modelo);

    public ConsultasPanel() {
        super(new BorderLayout(12, 12));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        lista.setFont(UI.NORMAL);
        lista.setFixedCellHeight(34);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selecionar(lista.getSelectedValue());
        });
        JPanel esquerda = UI.cartao(new BorderLayout(0, 8));
        JLabel l = new JLabel("Consultas disponiveis");
        l.setFont(UI.SUBTITULO);
        esquerda.add(l, BorderLayout.NORTH);
        esquerda.add(new JScrollPane(lista), BorderLayout.CENTER);
        esquerda.setPreferredSize(new Dimension(300, 0));

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
        sqlScroll.setPreferredSize(new Dimension(0, 220));

        JButton executar = UI.botao("Executar consulta", true);
        executar.addActionListener(e -> executar());
        parametro.addActionListener(e -> executar());
        JButton exportar = UI.botao("Exportar CSV", false);
        exportar.addActionListener(e -> exportarCsv());
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(rotuloParametro);
        acoes.add(parametro);
        acoes.add(executar);
        acoes.add(exportar);
        status.setForeground(UI.TEXTO_SUAVE);
        acoes.add(status);

        JPanel cabecalho = new JPanel(new BorderLayout(0, 6));
        cabecalho.setOpaque(false);
        cabecalho.add(tituloConsulta, BorderLayout.NORTH);
        cabecalho.add(descricao, BorderLayout.CENTER);

        JPanel detalhe = UI.cartao(new BorderLayout(0, 10));
        detalhe.add(cabecalho, BorderLayout.NORTH);
        detalhe.add(sqlScroll, BorderLayout.CENTER);
        detalhe.add(acoes, BorderLayout.SOUTH);

        UI.estilizarTabela(tabela);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JPanel resultado = UI.cartao(new BorderLayout());
        resultado.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JSplitPane direita = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detalhe, resultado);
        direita.setResizeWeight(0.45);
        direita.setBorder(null);
        direita.setOpaque(false);

        add(esquerda, BorderLayout.WEST);
        add(direita, BorderLayout.CENTER);
        lista.setSelectedIndex(0);
    }

    private void selecionar(Consulta c) {
        if (c == null) return;
        tituloConsulta.setText(c.getTitulo());
        descricao.setText(c.getDescricao());
        sql.setText(c.getSql());
        sql.setCaretPosition(0);
        rotuloParametro.setVisible(c.temParametro());
        parametro.setVisible(c.temParametro());
        if (c.temParametro()) {
            rotuloParametro.setText(c.getRotuloParametro() + ":");
            parametro.setText(c.getValorPadrao());
        }
        modelo.setDataVector(new Object[0][0], new Object[0]);
        status.setText(" ");
        revalidate();
    }

    private void executar() {
        Consulta c = lista.getSelectedValue();
        if (c == null) return;
        String valor = parametro.getText();
        if (c.getTipoParametro() == Consulta.TipoParametro.INTEIRO && !valor.trim().matches("-?\\d+")) {
            JOptionPane.showMessageDialog(this, "Informe um numero inteiro em \"" + c.getRotuloParametro() + "\".");
            return;
        }
        status.setText("Executando...");
        new SwingWorker<ResultadoConsulta, Void>() {
            @Override
            protected ResultadoConsulta doInBackground() throws Exception {
                return dao.executar(c, valor);
            }

            @Override
            protected void done() {
                try {
                    ResultadoConsulta r = get();
                    modelo.setDataVector(r.linhas().toArray(new Object[0][]), r.colunas().toArray());
                    ajustarColunas();
                    status.setText(r.linhas().size() + " linha(s) em " + r.tempoMs() + " ms");
                } catch (Exception e) {
                    status.setText("Erro");
                    UI.erro(ConsultasPanel.this, e.getCause() instanceof Exception x ? x : e);
                }
            }
        }.execute();
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
            tabela.getColumnModel().getColumn(c).setPreferredWidth(Math.min(w, 520));
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
