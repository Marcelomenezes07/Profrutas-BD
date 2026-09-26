package model;

import java.math.BigDecimal;

public class Produto {
    private int cdProduto;
    private String nome;
    private String descricao;
    private BigDecimal valor;
    private boolean vendidoUnitario;
    private String marca;

    public Produto() {}

    public Produto(int cdProduto, String nome, String descricao, BigDecimal valor,
                   boolean vendidoUnitario, String marca) {
        this.cdProduto = cdProduto;
        this.nome = nome;
        this.descricao = descricao;
        this.valor = valor;
        this.vendidoUnitario = vendidoUnitario;
        this.marca = marca;
    }

    public int getCdProduto() { return cdProduto; }
    public void setCdProduto(int cdProduto) { this.cdProduto = cdProduto; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public boolean isVendidoUnitario() { return vendidoUnitario; }
    public void setVendidoUnitario(boolean vendidoUnitario) { this.vendidoUnitario = vendidoUnitario; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
}
