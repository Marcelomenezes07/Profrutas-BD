package model;

public class Funcionario {
    private int nrFuncionario;
    private String nome;
    private String cpf;
    private String telefone;
    private String cep;
    private String rua;
    private String bairro;
    private String numero;
    private String apartamento;
    private Integer nrSupervisor;     // pode ser nulo (sem supervisor)
    private String nomeSupervisor;    // preenchido pelo LEFT JOIN na listagem

    public Funcionario() {}

    public int getNrFuncionario() { return nrFuncionario; }
    public void setNrFuncionario(int nrFuncionario) { this.nrFuncionario = nrFuncionario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getApartamento() { return apartamento; }
    public void setApartamento(String apartamento) { this.apartamento = apartamento; }

    public Integer getNrSupervisor() { return nrSupervisor; }
    public void setNrSupervisor(Integer nrSupervisor) { this.nrSupervisor = nrSupervisor; }

    public String getNomeSupervisor() { return nomeSupervisor; }
    public void setNomeSupervisor(String nomeSupervisor) { this.nomeSupervisor = nomeSupervisor; }

    @Override
    public String toString() {
        return nrFuncionario + " - " + nome;
    }
}
