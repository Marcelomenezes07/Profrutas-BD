package view;

import dao.ReferenciaDAO;
import dao.RotinasDAO;
import model.ItemLista;
import model.ResultadoConsulta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Integracao com a Etapa 05: executa os procedimentos e mostra o efeito das funcoes e triggers.
 *   - Reajuste por categoria  -> sp_reajustar_precos_categoria + trigger trg_produto_log_preco (log)
 *   - Baixa de estoque FEFO   -> sp_baixar_estoque_fefo (cursor) + fn_estoque_disponivel
 *   - Registrar perda         -> INSERT em PERDA, que dispara trg_perda_baixa_estoque
 */
public class RotinasPanel extends JPanel {
    private final RotinasDAO dao = new RotinasDAO();
    private final ReferenciaDAO referencias = new ReferenciaDAO();

    // reajuste
    private final JComboBox<ItemLista> categoria = new JComboBox<>();
    private final JTextField percentual = new JTextField("10");
    // FEFO
    private final JComboBox<ItemLista> produtoFefo = new JComboBox<>();
    private final JLabel disponivelFefo = new JLabel("-");
    private final JTextField quantidadeFefo = new JTextField();
    // perda
    private final JComboBox<ItemLista> produtoPerda = new JComboBox<>();
    private final JComboBox<ItemLista> lotePerda = new JComboBox<>();
    private final JTextField quantidadePerda = new JTextField();
    private final JComboBox<String> motivoPerda = new JComboBox<>(new String[] {
        "Produto vencido", "Produto amassado", "Excesso de maturacao", "Quebra no transporte",
        "Umidade excessiva no armazenamento", "Praga ou inseto identificado"});
    private final JCheckBox descarte = new JCheckBox("Descarte ja efetivado");

    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel resultado = modeloTabela();
    private final DefaultTableModel log = modeloTabela();
    private final JTabbedPane abasResultado = new JTabbedPane();
    private boolean carregando = false;

    public RotinasPanel() {
        super(new BorderLayout(0, 12));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel topo = new JPanel(new BorderLayout(0, 4));
        topo.setOpaque(false);
        topo.add(UI.titulo("Rotinas do banco (Etapa 05)"), BorderLayout.NORTH);
        JLabel sub = new JLabel("Executa os procedimentos armazenados e mostra o efeito das funcoes e dos triggers. "
            + "As alteracoes sao gravadas no banco.");
        sub.setForeground(UI.TEXTO_SUAVE);
        topo.add(sub, BorderLayout.CENTER);

        motivoPerda.setEditable(true);
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.setOpaque(false);
        cards.add(cardReajuste());
        cards.add(cardFefo());
        cards.add(cardPerda());

        JTable tResultado = new JTable(resultado);
        JTable tLog = new JTable(log);
        UI.estilizarTabela(tResultado);
        UI.estilizarTabela(tLog);
        abasResultado.addTab("Resultado da ultima execucao", new JScrollPane(tResultado));
        abasResultado.addTab("Historico de precos (LOG_PRECO_PRODUTO - gravado pelo trigger)", new JScrollPane(tLog));
        JPanel baixo = UI.cartao(new BorderLayout(0, 8));
        status.setFont(UI.NORMAL.deriveFont(Font.BOLD));
        status.setForeground(UI.VERDE_ESCURO);
        baixo.add(status, BorderLayout.NORTH);
        baixo.add(abasResultado, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(cards, BorderLayout.NORTH);
        centro.add(baixo, BorderLayout.CENTER);

        add(topo, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        produtoFefo.addActionListener(e -> { if (!carregando) atualizarDisponivel(); });
        produtoPerda.addActionListener(e -> { if (!carregando) atualizarLotes(); });
    }

    // ------------------------------------------------------------------ cards

    private JPanel cardReajuste() {
        JPanel c = card("Reajuste de precos por categoria",
            "<html>Procedimento <b>sp_reajustar_precos_categoria</b> (atualizacao de dados). "
            + "Cada preco alterado dispara o trigger <b>trg_produto_log_preco</b>, que grava o historico.</html>");
        JPanel f = formulario();
        linha(f, 0, "Categoria", categoria);
        linha(f, 1, "Percentual (%)", percentual);
        c.add(f, BorderLayout.CENTER);
        JButton b = UI.botao("Executar reajuste", true);
        b.addActionListener(e -> reajustar());
        c.add(b, BorderLayout.SOUTH);
        return c;
    }

    private JPanel cardFefo() {
        JPanel c = card("Baixa de estoque (regra FEFO)",
            "<html>Procedimento com <b>CURSOR</b> <b>sp_baixar_estoque_fefo</b>: consome primeiro o lote que vence "
            + "primeiro. Usa a funcao <b>fn_estoque_disponivel</b> (so lotes nao vencidos).</html>");
        JPanel f = formulario();
        linha(f, 0, "Produto", produtoFefo);
        disponivelFefo.setFont(UI.NORMAL.deriveFont(Font.BOLD));
        linha(f, 1, "Disponivel p/ venda", disponivelFefo);
        linha(f, 2, "Quantidade da baixa", quantidadeFefo);
        c.add(f, BorderLayout.CENTER);
        JButton b = UI.botao("Dar baixa (FEFO)", true);
        b.addActionListener(e -> baixarFefo());
        c.add(b, BorderLayout.SOUTH);
        return c;
    }

    private JPanel cardPerda() {
        JPanel c = card("Registrar perda",
            "<html>INSERT em <b>PERDA</b>, que dispara o trigger <b>trg_perda_baixa_estoque</b>: bloqueia perda "
            + "maior que o lote e desconta a quantidade do estoque.</html>");
        JPanel f = formulario();
        linha(f, 0, "Produto", produtoPerda);
        linha(f, 1, "Lote", lotePerda);
        linha(f, 2, "Quantidade perdida", quantidadePerda);
        linha(f, 3, "Motivo", motivoPerda);
        descarte.setOpaque(false);
        linha(f, 4, "", descarte);
        c.add(f, BorderLayout.CENTER);
        JButton b = UI.botao("Registrar perda", true);
        b.addActionListener(e -> registrarPerda());
        c.add(b, BorderLayout.SOUTH);
        return c;
    }

    // ------------------------------------------------------------------ acoes

    private void reajustar() {
        ItemLista cat = (ItemLista) categoria.getSelectedItem();
        if (cat == null) return;
        BigDecimal pct;
        try {
            pct = UI.lerNumero(percentual.getText(), "Percentual");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return;
        }
        if (!confirmar("Reajustar em " + pct.toPlainString() + "% os precos da categoria\n" + cat.nome() + "?")) return;
        try {
            ResultadoConsulta r = dao.reajustarPrecos(cat.id(), pct);
            mostrar(resultado, r);
            carregarLog();
            Object afetados = r.linhas().isEmpty() ? "?" : r.linhas().get(0)[2];
            status.setText("Reajuste aplicado: " + afetados + " produto(s). O trigger registrou cada alteracao no "
                + "historico de precos.");
            abasResultado.setSelectedIndex(1);
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    private void baixarFefo() {
        ItemLista p = (ItemLista) produtoFefo.getSelectedItem();
        if (p == null) return;
        BigDecimal qtd;
        try {
            qtd = UI.lerNumero(quantidadeFefo.getText(), "Quantidade da baixa");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return;
        }
        if (!confirmar("Dar baixa de " + qtd.toPlainString() + " de " + p.nome() + " (FEFO)?")) return;
        try {
            ResultadoConsulta r = dao.baixarEstoqueFefo(p.id(), qtd);
            mostrar(resultado, r);
            abasResultado.setSelectedIndex(0);
            status.setText("Baixa concluida: " + r.linhas().size() + " lote(s) utilizado(s), do que vence primeiro "
                + "para o ultimo. Veja abaixo quanto saiu de cada lote.");
            atualizarDisponivel();
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    private void registrarPerda() {
        ItemLista p = (ItemLista) produtoPerda.getSelectedItem();
        ItemLista lote = (ItemLista) lotePerda.getSelectedItem();
        if (p == null || lote == null) {
            JOptionPane.showMessageDialog(this, "Escolha o produto e o lote.");
            return;
        }
        BigDecimal qtd;
        try {
            qtd = UI.lerNumero(quantidadePerda.getText(), "Quantidade perdida");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return;
        }
        String motivo = String.valueOf(motivoPerda.getSelectedItem());
        try {
            BigDecimal[] r = dao.registrarPerda(p.id(), lote.id(), qtd, motivo, descarte.isSelected());
            resultado.setDataVector(new Object[][] {{p.nome(), lote.id(), r[0], qtd, r[1]}},
                new Object[] {"produto", "lote", "estoque_antes", "qtd_perdida", "estoque_depois (trigger)"});
            abasResultado.setSelectedIndex(0);
            status.setText("Perda registrada. O trigger descontou o estoque do lote " + lote.id() + ": "
                + r[0].toPlainString() + " -> " + r[1].toPlainString());
            atualizarLotes();
            atualizarDisponivel();
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    // ------------------------------------------------------------------ carga

    public void carregar() {
        carregando = true;
        try {
            preencher(categoria, referencias.listarCategoriasComProdutos());
            List<ItemLista> produtos = referencias.listarProdutos();
            preencher(produtoFefo, produtos);
            preencher(produtoPerda, produtos);
        } catch (Exception e) {
            UI.erro(this, e);
        } finally {
            carregando = false;
        }
        atualizarDisponivel();
        atualizarLotes();
        carregarLog();
    }

    private void atualizarDisponivel() {
        ItemLista p = (ItemLista) produtoFefo.getSelectedItem();
        if (p == null) return;
        try {
            disponivelFefo.setText(dao.estoqueDisponivel(p.id()).toPlainString() + "  (fn_estoque_disponivel)");
        } catch (Exception e) {
            disponivelFefo.setText("erro - execute a Parte 6 do consultas.sql");
        }
    }

    private void atualizarLotes() {
        ItemLista p = (ItemLista) produtoPerda.getSelectedItem();
        lotePerda.removeAllItems();
        if (p == null) return;
        try {
            for (ItemLista l : referencias.listarLotes(p.id())) lotePerda.addItem(l);
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    private void carregarLog() {
        try {
            mostrar(log, dao.listarLogPrecos());
        } catch (Exception e) {
            log.setDataVector(new Object[][] {{"Tabela de log indisponivel: execute a Parte 6 do consultas.sql"}},
                new Object[] {"aviso"});
        }
    }

    // ------------------------------------------------------------------ utilitarios

    private static DefaultTableModel modeloTabela() {
        return new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return UI.classeColuna(this, c); }
        };
    }

    private static void mostrar(DefaultTableModel m, ResultadoConsulta r) {
        m.setDataVector(r.linhas().toArray(new Object[0][]), r.colunas().toArray());
    }

    private static void preencher(JComboBox<ItemLista> combo, List<ItemLista> itens) {
        Object atual = combo.getSelectedItem();
        combo.removeAllItems();
        for (ItemLista i : itens) combo.addItem(i);
        if (atual != null) combo.setSelectedItem(atual);
    }

    private boolean confirmar(String pergunta) {
        return JOptionPane.showConfirmDialog(this, pergunta + "\n\nA alteracao sera gravada no banco.",
            "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private static JPanel card(String titulo, String descricao) {
        JPanel c = UI.cartao(new BorderLayout(0, 10));
        JPanel cab = new JPanel(new BorderLayout(0, 4));
        cab.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(UI.SUBTITULO);
        JLabel d = new JLabel(descricao);
        d.setFont(UI.PEQUENA);
        d.setForeground(UI.TEXTO_SUAVE);
        d.setPreferredSize(new Dimension(300, 48));
        cab.add(t, BorderLayout.NORTH);
        cab.add(d, BorderLayout.CENTER);
        c.add(cab, BorderLayout.NORTH);
        return c;
    }

    private static JPanel formulario() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        return f;
    }

    private static void linha(JPanel f, int y, String rotulo, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 8);
        f.add(new JLabel(rotulo), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        f.add(campo, c);
    }
}
