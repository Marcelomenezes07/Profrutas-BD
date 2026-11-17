package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lote de estoque (tabela ESTOQUE, chave composta cd_produto + nr_lote). */
public class Estoque {
    private int cdProduto;
    private String nomeProduto;       // preenchido pelo JOIN com PRODUTO
    private int nrLote;
    private BigDecimal quantidade;
    private LocalDate dtEntrada;
    private LocalDate dtValidade;
    private String situacao;          // fn_situacao_validade(dt_validade)
    private BigDecimal disponivelProduto; // fn_estoque_disponivel(cd_produto)

    public int getCdProduto() { return cdProduto; }
    public void setCdProduto(int cdProduto) { this.cdProduto = cdProduto; }

    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }

    public int getNrLote() { return nrLote; }
    public void setNrLote(int nrLote) { this.nrLote = nrLote; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public LocalDate getDtEntrada() { return dtEntrada; }
    public void setDtEntrada(LocalDate dtEntrada) { this.dtEntrada = dtEntrada; }

    public LocalDate getDtValidade() { return dtValidade; }
    public void setDtValidade(LocalDate dtValidade) { this.dtValidade = dtValidade; }

    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }

    public BigDecimal getDisponivelProduto() { return disponivelProduto; }
    public void setDisponivelProduto(BigDecimal disponivelProduto) { this.disponivelProduto = disponivelProduto; }
}
