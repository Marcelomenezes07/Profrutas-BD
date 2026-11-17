package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/** Cores, fontes e utilitarios visuais compartilhados pelas telas. */
public final class UI {
    private UI() {}

    public static final Color VERDE = new Color(0x2E7D32);
    public static final Color VERDE_ESCURO = new Color(0x1B5E20);
    public static final Color VERDE_CLARO = new Color(0xE8F5E9);
    public static final Color FUNDO = new Color(0xF4F6F4);
    public static final Color CARTAO = Color.WHITE;
    public static final Color BORDA = new Color(0xDDE3DD);
    public static final Color TEXTO = new Color(0x1F2A1F);
    public static final Color TEXTO_SUAVE = new Color(0x5F6B5F);
    public static final Color VERMELHO = new Color(0xC62828);

    /** Paleta categorica dos graficos. */
    public static final Color[] PALETA = {
        new Color(0x2E7D32), new Color(0xF9A825), new Color(0xEF6C00), new Color(0x1565C0),
        new Color(0x8E24AA), new Color(0x00897B), new Color(0xC62828), new Color(0x6D4C41),
        new Color(0x7CB342), new Color(0x546E7A)
    };

    public static final Font TITULO = new Font("SansSerif", Font.BOLD, 20);
    public static final Font SUBTITULO = new Font("SansSerif", Font.BOLD, 14);
    public static final Font NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font PEQUENA = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final NumberFormat NUMERO = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
    static {
        NUMERO.setMaximumFractionDigits(2);
    }

    public static String moeda(double v) { return MOEDA.format(v); }
    public static String numero(double v) { return NUMERO.format(v); }
    public static String dataBr(java.time.LocalDate d) {
        return d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static void aplicarTema() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }
        UIManager.put("nimbusBase", VERDE_ESCURO);
        UIManager.put("nimbusBlueGrey", new Color(0xB7C4B7));
        UIManager.put("nimbusFocus", VERDE);
        UIManager.put("nimbusSelectionBackground", VERDE);
        UIManager.put("control", FUNDO);
        UIManager.put("Table.alternateRowColor", new Color(0xF7FAF7));
    }

    public static JButton botao(String texto, boolean primario) {
        JButton b = new JButton(texto);
        b.setFont(NORMAL.deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        if (primario) {
            b.setBackground(VERDE);
            b.setForeground(Color.WHITE);
        }
        return b;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(TITULO);
        l.setForeground(TEXTO);
        return l;
    }

    /** Painel branco com borda fina, usado como "cartao". */
    public static JPanel cartao(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(CARTAO);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDA),
            new EmptyBorder(12, 14, 12, 14)));
        return p;
    }

    public static void estilizarTabela(JTable t) {
        t.setFont(NORMAL);
        t.setRowHeight(24);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDA);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(NORMAL.deriveFont(Font.BOLD));
        h.setReorderingAllowed(false);
        DefaultTableCellRenderer numeros = new DefaultTableCellRenderer();
        numeros.setHorizontalAlignment(SwingConstants.RIGHT);
        t.setDefaultRenderer(Number.class, numeros);
        t.setDefaultRenderer(Integer.class, numeros);
        t.setDefaultRenderer(java.math.BigDecimal.class, numeros);
        DefaultTableCellRenderer datas = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object v) {
                if (v instanceof java.sql.Date d) v = dataBr(d.toLocalDate());
                else if (v instanceof java.time.LocalDate d) v = dataBr(d);
                super.setValue(v);
            }
        };
        datas.setHorizontalAlignment(SwingConstants.CENTER);
        t.setDefaultRenderer(java.sql.Date.class, datas);
        t.setDefaultRenderer(java.util.Date.class, datas);
        t.setDefaultRenderer(java.time.LocalDate.class, datas);
    }

    /** Campo de data (dd/MM/yyyy) com setas para mudar o dia. */
    public static JSpinner campoData(java.time.LocalDate inicial) {
        JSpinner sp = new JSpinner(new SpinnerDateModel());
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        definirData(sp, inicial);
        return sp;
    }

    public static void definirData(JSpinner sp, java.time.LocalDate data) {
        sp.setValue(java.util.Date.from(data.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
    }

    public static java.time.LocalDate lerData(JSpinner sp) {
        try {
            sp.commitEdit();
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Data invalida. Use o formato dd/mm/aaaa.");
        }
        return ((java.util.Date) sp.getValue()).toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    /** Converte um numero digitado (aceita virgula ou ponto). */
    public static java.math.BigDecimal lerNumero(String texto, String campo) {
        try {
            return new java.math.BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor invalido em \"" + campo + "\". Use numeros, ex.: 12,5");
        }
    }

    /** Tipo da coluna inferido pelo primeiro valor nao nulo (ordenacao numerica correta). */
    public static Class<?> classeColuna(javax.swing.table.TableModel m, int coluna) {
        for (int r = 0; r < m.getRowCount(); r++) {
            Object v = m.getValueAt(r, coluna);
            if (v != null) return v.getClass();
        }
        return Object.class;
    }

    /** Traduz os erros mais comuns do MySQL para mensagens compreensiveis. */
    public static String mensagemErro(Exception e) {
        if (e instanceof SQLException sql) {
            return switch (sql.getErrorCode()) {
                case 1451 -> "Nao e possivel excluir: o registro esta vinculado a outras tabelas "
                           + "(ex.: pedidos, estoque, fornecimentos).\n\nDetalhe: " + sql.getMessage();
                case 1452 -> "Referencia invalida: o registro relacionado nao existe.\n\nDetalhe: " + sql.getMessage();
                case 1062 -> "Valor duplicado: ja existe um registro com esse dado unico (CPF/CNPJ, nome...).\n\nDetalhe: "
                           + sql.getMessage();
                case 3819 -> "O valor informado viola uma restricao (CHECK) da tabela.\n\nDetalhe: " + sql.getMessage();
                case 1406 -> "Um dos campos excede o tamanho maximo permitido.\n\nDetalhe: " + sql.getMessage();
                case 1048 -> "Um campo obrigatorio ficou vazio.\n\nDetalhe: " + sql.getMessage();
                case 1045 -> "Usuario ou senha do MySQL incorretos.";
                case 1644 -> sql.getMessage();   // regra de negocio (SIGNAL em procedimento/trigger)
                case 1305 -> "Funcao ou procedimento inexistente no banco. Execute antes a Parte 6 (Etapa 05) "
                           + "do arquivo consultas.sql.\n\nDetalhe: " + sql.getMessage();
                case 1146 -> "Tabela ou visao inexistente no banco. Se for uma das visoes (vw_...), execute antes "
                           + "a Parte 5 (Entrega 4) do arquivo consultas.sql.\n\nDetalhe: " + sql.getMessage();
                case 1049 -> "O banco 'hortifruti' nao existe. Execute modelo_fisico.sql e inser_to.sql primeiro.";
                case 0 -> "Nao foi possivel conectar ao MySQL. Verifique se o servidor esta em execucao.\n\nDetalhe: "
                        + sql.getMessage();
                default -> "Erro do banco (" + sql.getErrorCode() + "): " + sql.getMessage();
            };
        }
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    public static void erro(Component pai, Exception e) {
        JOptionPane.showMessageDialog(pai, mensagemErro(e), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
