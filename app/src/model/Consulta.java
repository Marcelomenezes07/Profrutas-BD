package model;

import java.util.List;

/**
 * Consulta pre-definida exibida na aba "Consultas".
 * Cada parametro (filtro) preenche, na ordem, um "?" do SQL via PreparedStatement.
 */
public class Consulta {
    public enum TipoParametro { NENHUM, INTEIRO, TEXTO, DATA }

    /**
     * Filtro de uma consulta. Quando ha opcoes, a tela mostra uma lista de selecao;
     * a opcao "Todos(as)" corresponde ao valor "%" (curinga do LIKE).
     */
    public record Parametro(String rotulo, TipoParametro tipo, String valorPadrao, String... opcoes) {}

    private final String titulo;
    private final String descricao;
    private final String sql;
    private final List<Parametro> parametros;

    public Consulta(String titulo, String descricao, String sql, Parametro... parametros) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.sql = sql;
        this.parametros = List.of(parametros);
    }

    /** Atalho para consultas com um unico parametro. */
    public Consulta(String titulo, String descricao, String sql,
                    TipoParametro tipoParametro, String rotuloParametro, String valorPadrao) {
        this(titulo, descricao, sql, new Parametro(rotuloParametro, tipoParametro, valorPadrao));
    }

    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getSql() { return sql; }
    public List<Parametro> getParametros() { return parametros; }
    public boolean temParametro() { return !parametros.isEmpty(); }

    @Override
    public String toString() { return titulo; }
}
