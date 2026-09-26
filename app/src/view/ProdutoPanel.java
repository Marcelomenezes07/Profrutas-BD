package view;

import dao.ProdutoDAO;
import model.Produto;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Cadastro de produtos (INSERT / UPDATE / DELETE na tabela PRODUTO). */
public class ProdutoPanel extends CrudPanel {
    private final ProdutoDAO dao = new ProdutoDAO();
    private List<Produto> produtos = new ArrayList<>();

    private final JTextField nome = new JTextField();
    private final JTextField descricao = new JTextField();
    private final JTextField valor = new JTextField();
    private final JComboBox<String> tipoVenda = new JComboBox<>(new String[] {"Por quilo (kg)", "Por unidade"});
    private final JTextField marca = new JTextField();

    public ProdutoPanel() {
        super("Produtos", new String[] {"Codigo", "Nome", "Descricao", "Valor (R$)", "Vendido por", "Marca"});
        campo("Nome *", nome);
        campo("Descricao", descricao);
        campo("Valor (R$) *", valor);
        campo("Tipo de venda *", tipoVenda);
        campo("Marca", marca);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(260);
    }

    @Override
    protected List<Object[]> listarLinhas() throws Exception {
        produtos = dao.listar();
        List<Object[]> linhas = new ArrayList<>();
        for (Produto p : produtos) {
            linhas.add(new Object[] {p.getCdProduto(), p.getNome(), p.getDescricao(), p.getValor(),
                p.isVendidoUnitario() ? "Unidade" : "Kg", p.getMarca()});
        }
        return linhas;
    }

    @Override
    protected void preencherFormulario(int linha) {
        Produto p = produtos.get(linha);
        nome.setText(texto(p.getNome()));
        descricao.setText(texto(p.getDescricao()));
        valor.setText(p.getValor().toPlainString().replace('.', ','));
        tipoVenda.setSelectedIndex(p.isVendidoUnitario() ? 1 : 0);
        marca.setText(texto(p.getMarca()));
    }

    @Override
    protected void limparFormulario() {
        nome.setText("");
        descricao.setText("");
        valor.setText("");
        tipoVenda.setSelectedIndex(0);
        marca.setText("");
    }

    @Override
    protected void gravar(Integer id) throws Exception {
        Produto p = new Produto();
        p.setNome(obrigatorio(nome, "Nome"));
        p.setDescricao(opcional(descricao));
        try {
            p.setValor(new BigDecimal(obrigatorio(valor, "Valor").replace(',', '.')));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor invalido. Use numeros, ex.: 8,47");
        }
        if (p.getValor().signum() <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
        p.setVendidoUnitario(tipoVenda.getSelectedIndex() == 1);
        p.setMarca(opcional(marca));
        if (id == null) {
            dao.inserir(p);
        } else {
            p.setCdProduto(id);
            dao.atualizar(p);
        }
    }

    @Override
    protected void remover(int id) throws Exception {
        dao.excluir(id);
    }
}
