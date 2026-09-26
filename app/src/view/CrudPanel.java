package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

/**
 * Estrutura comum das telas de cadastro: tabela com busca a esquerda e
 * formulario a direita com os botoes Novo / Salvar / Excluir.
 * Cada subclasse implementa o acesso ao seu DAO.
 */
public abstract class CrudPanel extends JPanel {
    protected final DefaultTableModel modelo;
    protected final JTable tabela;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JPanel formulario = new JPanel(new GridBagLayout());
    private final JLabel modoLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");
    private final Component espacador = Box.createGlue();
    private int linhaForm = 0;

    /** Chave primaria do registro em edicao (null = novo registro). */
    protected Integer idSelecionado;

    protected CrudPanel(String titulo, String[] colunas) {
        super(new BorderLayout(0, 12));
        setBackground(UI.FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        modelo = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return UI.classeColuna(this, c); }
        };
        tabela = new JTable(modelo);
        UI.estilizarTabela(tabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(modelo);
        tabela.setRowSorter(sorter);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int v = tabela.getSelectedRow();
            if (v >= 0) {
                int linha = tabela.convertRowIndexToModel(v);
                idSelecionado = (Integer) modelo.getValueAt(linha, 0);
                preencherFormulario(linha);
                atualizarModo();
            }
        });

        JTextField busca = new JTextField();
        busca.putClientProperty("JTextField.placeholderText", "Buscar...");
        busca.getDocument().addDocumentListener(new DocumentListener() {
            void filtrar() {
                String t = busca.getText().trim();
                sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t)));
            }
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        JPanel topo = new JPanel(new BorderLayout(12, 0));
        topo.setOpaque(false);
        topo.add(UI.titulo(titulo), BorderLayout.WEST);
        JPanel buscaPainel = new JPanel(new BorderLayout(6, 0));
        buscaPainel.setOpaque(false);
        buscaPainel.add(new JLabel("Buscar:"), BorderLayout.WEST);
        buscaPainel.add(busca, BorderLayout.CENTER);
        buscaPainel.setPreferredSize(new Dimension(320, 28));
        topo.add(buscaPainel, BorderLayout.EAST);

        JPanel lista = UI.cartao(new BorderLayout());
        lista.add(new JScrollPane(tabela), BorderLayout.CENTER);

        modoLabel.setFont(UI.SUBTITULO);
        formulario.setOpaque(false);

        JButton novo = UI.botao("Novo", false);
        JButton salvar = UI.botao("Salvar", true);
        JButton excluir = UI.botao("Excluir", false);
        excluir.setForeground(UI.VERMELHO);
        novo.addActionListener(e -> novo());
        salvar.addActionListener(e -> salvar());
        excluir.addActionListener(e -> excluir());
        JPanel botoes = new JPanel(new GridLayout(1, 3, 8, 0));
        botoes.setOpaque(false);
        botoes.add(novo);
        botoes.add(excluir);
        botoes.add(salvar);

        statusLabel.setFont(UI.PEQUENA);
        statusLabel.setForeground(UI.TEXTO_SUAVE);
        JPanel rodape = new JPanel(new BorderLayout(0, 8));
        rodape.setOpaque(false);
        rodape.add(botoes, BorderLayout.NORTH);
        rodape.add(statusLabel, BorderLayout.SOUTH);

        JPanel lateral = UI.cartao(new BorderLayout(0, 12));
        lateral.add(modoLabel, BorderLayout.NORTH);
        JScrollPane formScroll = new JScrollPane(formulario);
        formScroll.setBorder(null);
        formScroll.getViewport().setOpaque(false);
        formScroll.setOpaque(false);
        lateral.add(formScroll, BorderLayout.CENTER);
        lateral.add(rodape, BorderLayout.SOUTH);
        lateral.setPreferredSize(new Dimension(380, 0));

        add(topo, BorderLayout.NORTH);
        add(lista, BorderLayout.CENTER);
        add(lateral, BorderLayout.EAST);
        atualizarModo();
    }

    /** Adiciona um campo (rotulo + componente) ao formulario. */
    protected void campo(String rotulo, JComponent componente) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = linhaForm;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 10);
        JLabel l = new JLabel(rotulo);
        l.setFont(UI.NORMAL);
        formulario.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        formulario.add(componente, c);
        linhaForm++;
        formulario.remove(espacador);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = 1000;
        filler.weighty = 1;
        formulario.add(espacador, filler);
    }

    private void atualizarModo() {
        if (idSelecionado == null) {
            modoLabel.setText("Novo registro");
            modoLabel.setForeground(UI.VERDE);
        } else {
            modoLabel.setText("Editando registro #" + idSelecionado);
            modoLabel.setForeground(UI.PALETA[3]);
        }
    }

    public void carregar() {
        try {
            modelo.setRowCount(0);
            for (Object[] linha : listarLinhas()) modelo.addRow(linha);
            statusLabel.setText(modelo.getRowCount() + " registro(s) carregado(s)");
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    private void novo() {
        idSelecionado = null;
        tabela.clearSelection();
        limparFormulario();
        atualizarModo();
    }

    private void salvar() {
        try {
            boolean inserindo = idSelecionado == null;
            gravar(idSelecionado);
            carregar();
            novo();
            statusLabel.setText(inserindo ? "Registro inserido com sucesso." : "Registro alterado com sucesso.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Dados invalidos", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    private void excluir() {
        if (idSelecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um registro na tabela para excluir.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "Excluir o registro #" + idSelecionado + "?",
            "Confirmar exclusao", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            remover(idSelecionado);
            carregar();
            novo();
            statusLabel.setText("Registro excluido.");
        } catch (Exception e) {
            UI.erro(this, e);
        }
    }

    // ---------- utilitarios para as subclasses ----------

    protected static String obrigatorio(JTextField campo, String nome) {
        String v = campo.getText().trim();
        if (v.isEmpty()) throw new IllegalArgumentException("O campo \"" + nome + "\" e obrigatorio.");
        return v;
    }

    /** Campo opcional: texto vazio vira NULL no banco. */
    protected static String opcional(JTextField campo) {
        String v = campo.getText().trim();
        return v.isEmpty() ? null : v;
    }

    protected static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    // ---------- operacoes implementadas por cada cadastro ----------

    protected abstract List<Object[]> listarLinhas() throws Exception;

    protected abstract void preencherFormulario(int linhaModelo);

    protected abstract void limparFormulario();

    /** Insere (id == null) ou atualiza (id != null). */
    protected abstract void gravar(Integer id) throws Exception;

    protected abstract void remover(int id) throws Exception;
}
