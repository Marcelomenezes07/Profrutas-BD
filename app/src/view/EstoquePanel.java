package view;

import dao.EstoqueDAO;
import dao.ReferenciaDAO;
import model.Estoque;
import model.ItemLista;

import javax.swing.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro de lotes de estoque (INSERT / UPDATE / DELETE na tabela ESTOQUE).
 * As colunas "Situacao" e "Disponivel p/ venda" sao calculadas pelas funcoes da Etapa 05
 * fn_situacao_validade e fn_estoque_disponivel.
 */
public class EstoquePanel extends CrudPanel {
    private final EstoqueDAO dao = new EstoqueDAO();
    private final ReferenciaDAO referencias = new ReferenciaDAO();
    private List<Estoque> lotes = new ArrayList<>();

    private final JComboBox<ItemLista> produto = new JComboBox<>();
    private final JTextField nrLote = new JTextField();
    private final JTextField quantidade = new JTextField();
    private final JSpinner dtEntrada = UI.campoData(LocalDate.now());
    private final JSpinner dtValidade = UI.campoData(LocalDate.now().plusDays(15));
    private boolean preenchendo = false;

    public EstoquePanel() {
        super("Estoque (lotes)", new String[] {"Cod.", "Produto", "Lote", "Quantidade", "Entrada", "Validade",
            "Situacao (fn_situacao_validade)", "Disponivel p/ venda do produto (fn_estoque_disponivel)"});
        campo("Produto *", produto);
        campo("No do lote *", nrLote);
        campo("Quantidade *", quantidade);
        campo("Data de entrada *", dtEntrada);
        campo("Validade *", dtValidade);
        int[] larguras = {45, 140, 45, 85, 90, 90, 200, 290};
        for (int i = 0; i < larguras.length; i++) tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);

        // novo lote: sugere o proximo numero de lote livre do produto escolhido
        produto.addActionListener(e -> {
            if (preenchendo || idSelecionado != null) return;
            ItemLista p = (ItemLista) produto.getSelectedItem();
            if (p == null) return;
            try {
                nrLote.setText(String.valueOf(dao.proximoLote(p.id())));
            } catch (Exception ex) {
                nrLote.setText("");
            }
        });
    }

    @Override
    protected List<Object[]> listarLinhas() throws Exception {
        preenchendo = true;
        try {
            produto.removeAllItems();
            for (ItemLista p : referencias.listarProdutos()) produto.addItem(p);
        } finally {
            preenchendo = false;
        }
        lotes = dao.listar();
        List<Object[]> linhas = new ArrayList<>();
        for (Estoque e : lotes) {
            linhas.add(new Object[] {e.getCdProduto(), e.getNomeProduto(), e.getNrLote(), e.getQuantidade(),
                e.getDtEntrada(), e.getDtValidade(), e.getSituacao(), e.getDisponivelProduto()});
        }
        return linhas;
    }

    /** Chave composta: identifica o registro pela posicao na lista carregada. */
    @Override
    protected Integer chaveDaLinha(int linhaModelo) {
        return linhaModelo;
    }

    @Override
    protected String rotuloRegistro(int id) {
        Estoque e = lotes.get(id);
        return "lote " + e.getNrLote() + " de " + e.getNomeProduto();
    }

    @Override
    protected void preencherFormulario(int linha) {
        Estoque e = lotes.get(linha);
        preenchendo = true;
        try {
            for (int i = 0; i < produto.getItemCount(); i++) {
                if (produto.getItemAt(i).id() == e.getCdProduto()) {
                    produto.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            preenchendo = false;
        }
        nrLote.setText(String.valueOf(e.getNrLote()));
        quantidade.setText(e.getQuantidade().toPlainString().replace('.', ','));
        UI.definirData(dtEntrada, e.getDtEntrada());
        UI.definirData(dtValidade, e.getDtValidade());
        // produto e lote formam a chave primaria: nao mudam na alteracao
        produto.setEnabled(false);
        nrLote.setEditable(false);
    }

    @Override
    protected void limparFormulario() {
        produto.setEnabled(true);
        nrLote.setEditable(true);
        quantidade.setText("");
        UI.definirData(dtEntrada, LocalDate.now());
        UI.definirData(dtValidade, LocalDate.now().plusDays(15));
        if (produto.getItemCount() > 0) {
            produto.setSelectedIndex(0);
            ItemLista p = produto.getItemAt(0);
            try {
                nrLote.setText(String.valueOf(dao.proximoLote(p.id())));
            } catch (Exception ex) {
                nrLote.setText("");
            }
        }
    }

    @Override
    protected void gravar(Integer id) throws Exception {
        Estoque e = new Estoque();
        ItemLista p = (ItemLista) produto.getSelectedItem();
        if (p == null) throw new IllegalArgumentException("Escolha o produto.");
        e.setCdProduto(p.id());
        try {
            e.setNrLote(Integer.parseInt(obrigatorio(nrLote, "No do lote")));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("O numero do lote deve ser inteiro.");
        }
        e.setQuantidade(UI.lerNumero(obrigatorio(quantidade, "Quantidade"), "Quantidade"));
        if (e.getQuantidade().signum() < 0) throw new IllegalArgumentException("A quantidade nao pode ser negativa.");
        e.setDtEntrada(UI.lerData(dtEntrada));
        e.setDtValidade(UI.lerData(dtValidade));
        if (!e.getDtValidade().isAfter(e.getDtEntrada())) {
            throw new IllegalArgumentException("A validade deve ser posterior a data de entrada.");
        }
        if (id == null) {
            dao.inserir(e);
        } else {
            Estoque atual = lotes.get(id);
            e.setCdProduto(atual.getCdProduto());
            e.setNrLote(atual.getNrLote());
            dao.atualizar(e);
        }
    }

    @Override
    protected void remover(int id) throws Exception {
        Estoque e = lotes.get(id);
        dao.excluir(e.getCdProduto(), e.getNrLote());
    }
}
