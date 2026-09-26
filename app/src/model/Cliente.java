package model;

public class Cliente {
    private int nrCliente;
    private String cpfCnpj;
    private String telefone;
    private String telefone2;
    private String cep;
    private String rua;
    private String bairro;
    private String numero;
    private String apartamento;

    public Cliente() {}

    public Cliente(int nrCliente, String cpfCnpj, String telefone, String telefone2, String cep,
                   String rua, String bairro, String numero, String apartamento) {
        this.nrCliente = nrCliente;
        this.cpfCnpj = cpfCnpj;
        this.telefone = telefone;
        this.telefone2 = telefone2;
        this.cep = cep;
        this.rua = rua;
        this.bairro = bairro;
        this.numero = numero;
        this.apartamento = apartamento;
    }

    public int getNrCliente() { return nrCliente; }
    public void setNrCliente(int nrCliente) { this.nrCliente = nrCliente; }

    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getTelefone2() { return telefone2; }
    public void setTelefone2(String telefone2) { this.telefone2 = telefone2; }

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
}
