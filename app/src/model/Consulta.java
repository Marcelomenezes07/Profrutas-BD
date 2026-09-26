package model;

/**
 * Consulta pre-definida exibida na aba "Consultas".
 * Quando possui parametro, o SQL tem exatamente um "?" preenchido via PreparedStatement.
 */
public class Consulta {
    public enum TipoParametro { NENHUM, INTEIRO, TEXTO }

    private final String titulo;
    private final String descricao;
    private final String sql;
    private final TipoParametro tipoParametro;
    private final String rotuloParametro;
    private final String valorPadrao;

    public Consulta(String titulo, String descricao, String sql) {
        this(titulo, descricao, sql, TipoParametro.NENHUM, null, null);
    }

    public Consulta(String titulo, String descricao, String sql,
                    TipoParametro tipoParametro, String rotuloParametro, String valorPadrao) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.sql = sql;
        this.tipoParametro = tipoParametro;
        this.rotuloParametro = rotuloParametro;
        this.valorPadrao = valorPadrao;
    }

    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getSql() { return sql; }
    public TipoParametro getTipoParametro() { return tipoParametro; }
    public String getRotuloParametro() { return rotuloParametro; }
    public String getValorPadrao() { return valorPadrao; }
    public boolean temParametro() { return tipoParametro != TipoParametro.NENHUM; }

    @Override
    public String toString() { return titulo; }
}
