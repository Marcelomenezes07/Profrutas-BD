package view;

import dao.FuncionarioDAO;
import model.Funcionario;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/** Cadastro de funcionarios, incluindo o supervisor (auto-relacionamento). */
public class FuncionarioPanel extends CrudPanel {
    private static final String SEM_SUPERVISOR = "(sem supervisor)";

    private final FuncionarioDAO dao = new FuncionarioDAO();
    private List<Funcionario> funcionarios = new ArrayList<>();

    private final JTextField nome = new JTextField();
    private final JTextField cpf = new JTextField();
    private final JTextField telefone = new JTextField();
    private final JTextField cep = new JTextField();
    private final JTextField rua = new JTextField();
    private final JTextField bairro = new JTextField();
    private final JTextField numero = new JTextField();
    private final JTextField apartamento = new JTextField();
    private final JComboBox<Object> supervisor = new JComboBox<>();

    public FuncionarioPanel() {
        super("Funcionarios", new String[] {"Matricula", "Nome", "CPF", "Telefone", "Bairro", "Supervisor"});
        campo("Nome *", nome);
        campo("CPF (11 digitos) *", cpf);
        campo("Telefone", telefone);
        campo("CEP *", cep);
        campo("Rua *", rua);
        campo("Bairro *", bairro);
        campo("Numero *", numero);
        campo("Apartamento", apartamento);
        campo("Supervisor", supervisor);
    }

    @Override
    protected List<Object[]> listarLinhas() throws Exception {
        funcionarios = dao.listar();
        supervisor.removeAllItems();
        supervisor.addItem(SEM_SUPERVISOR);
        List<Object[]> linhas = new ArrayList<>();
        for (Funcionario f : funcionarios) {
            supervisor.addItem(f);
            linhas.add(new Object[] {f.getNrFuncionario(), f.getNome(), f.getCpf(), f.getTelefone(), f.getBairro(),
                f.getNomeSupervisor() == null ? "-" : f.getNomeSupervisor()});
        }
        return linhas;
    }

    @Override
    protected void preencherFormulario(int linha) {
        Funcionario f = funcionarios.get(linha);
        nome.setText(texto(f.getNome()));
        cpf.setText(texto(f.getCpf()));
        telefone.setText(texto(f.getTelefone()));
        cep.setText(texto(f.getCep()));
        rua.setText(texto(f.getRua()));
        bairro.setText(texto(f.getBairro()));
        numero.setText(texto(f.getNumero()));
        apartamento.setText(texto(f.getApartamento()));
        supervisor.setSelectedIndex(0);
        if (f.getNrSupervisor() != null) {
            for (int i = 1; i < supervisor.getItemCount(); i++) {
                if (((Funcionario) supervisor.getItemAt(i)).getNrFuncionario() == f.getNrSupervisor()) {
                    supervisor.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void limparFormulario() {
        for (JTextField f : new JTextField[] {nome, cpf, telefone, cep, rua, bairro, numero, apartamento}) {
            f.setText("");
        }
        if (supervisor.getItemCount() > 0) supervisor.setSelectedIndex(0);
    }

    @Override
    protected void gravar(Integer id) throws Exception {
        Funcionario f = new Funcionario();
        f.setNome(obrigatorio(nome, "Nome"));
        f.setCpf(obrigatorio(cpf, "CPF").replaceAll("\\D", ""));
        if (f.getCpf().length() != 11) throw new IllegalArgumentException("O CPF deve ter exatamente 11 digitos.");
        f.setTelefone(opcional(telefone));
        f.setCep(obrigatorio(cep, "CEP"));
        f.setRua(obrigatorio(rua, "Rua"));
        f.setBairro(obrigatorio(bairro, "Bairro"));
        f.setNumero(obrigatorio(numero, "Numero"));
        f.setApartamento(opcional(apartamento));
        Object sel = supervisor.getSelectedItem();
        f.setNrSupervisor(sel instanceof Funcionario s ? s.getNrFuncionario() : null);
        if (id != null && f.getNrSupervisor() != null && f.getNrSupervisor().equals(id)) {
            throw new IllegalArgumentException("Um funcionario nao pode ser supervisor de si mesmo.");
        }
        if (id == null) {
            dao.inserir(f);
        } else {
            f.setNrFuncionario(id);
            dao.atualizar(f);
        }
    }

    @Override
    protected void remover(int id) throws Exception {
        dao.excluir(id);
    }
}
