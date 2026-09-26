package view;

import dao.ClienteDAO;
import model.Cliente;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/** Cadastro de clientes (INSERT / UPDATE / DELETE na tabela CLIENTE). */
public class ClientePanel extends CrudPanel {
    private final ClienteDAO dao = new ClienteDAO();
    private List<Cliente> clientes = new ArrayList<>();

    private final JTextField cpfCnpj = new JTextField();
    private final JTextField telefone = new JTextField();
    private final JTextField telefone2 = new JTextField();
    private final JTextField cep = new JTextField();
    private final JTextField rua = new JTextField();
    private final JTextField bairro = new JTextField();
    private final JTextField numero = new JTextField();
    private final JTextField apartamento = new JTextField();

    public ClientePanel() {
        super("Clientes", new String[] {"Nº", "CPF/CNPJ", "Telefone", "Telefone 2", "CEP", "Rua", "Bairro", "Numero", "Apto"});
        campo("CPF/CNPJ *", cpfCnpj);
        campo("Telefone", telefone);
        campo("Telefone 2", telefone2);
        campo("CEP *", cep);
        campo("Rua *", rua);
        campo("Bairro *", bairro);
        campo("Numero *", numero);
        campo("Apartamento", apartamento);
    }

    @Override
    protected List<Object[]> listarLinhas() throws Exception {
        clientes = dao.listar();
        List<Object[]> linhas = new ArrayList<>();
        for (Cliente c : clientes) {
            linhas.add(new Object[] {c.getNrCliente(), c.getCpfCnpj(), c.getTelefone(), c.getTelefone2(),
                c.getCep(), c.getRua(), c.getBairro(), c.getNumero(), c.getApartamento()});
        }
        return linhas;
    }

    @Override
    protected void preencherFormulario(int linha) {
        Cliente c = clientes.get(linha);
        cpfCnpj.setText(texto(c.getCpfCnpj()));
        telefone.setText(texto(c.getTelefone()));
        telefone2.setText(texto(c.getTelefone2()));
        cep.setText(texto(c.getCep()));
        rua.setText(texto(c.getRua()));
        bairro.setText(texto(c.getBairro()));
        numero.setText(texto(c.getNumero()));
        apartamento.setText(texto(c.getApartamento()));
    }

    @Override
    protected void limparFormulario() {
        for (JTextField f : new JTextField[] {cpfCnpj, telefone, telefone2, cep, rua, bairro, numero, apartamento}) {
            f.setText("");
        }
    }

    @Override
    protected void gravar(Integer id) throws Exception {
        Cliente c = new Cliente();
        c.setCpfCnpj(obrigatorio(cpfCnpj, "CPF/CNPJ"));
        int digitos = c.getCpfCnpj().replaceAll("\\D", "").length();
        if (digitos != 11 && digitos != 14) {
            throw new IllegalArgumentException("CPF deve ter 11 digitos e CNPJ 14 digitos.");
        }
        c.setTelefone(opcional(telefone));
        c.setTelefone2(opcional(telefone2));
        c.setCep(obrigatorio(cep, "CEP"));
        c.setRua(obrigatorio(rua, "Rua"));
        c.setBairro(obrigatorio(bairro, "Bairro"));
        c.setNumero(obrigatorio(numero, "Numero"));
        c.setApartamento(opcional(apartamento));
        if (id == null) {
            dao.inserir(c);
        } else {
            c.setNrCliente(id);
            dao.atualizar(c);
        }
    }

    @Override
    protected void remover(int id) throws Exception {
        dao.excluir(id);
    }
}
