package dao;

import model.ResultadoConsulta;
import util.ConnectionFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Integracao com as funcoes, procedimentos e triggers da Etapa 05.
 * Os procedimentos sao chamados com CallableStatement ({CALL ...}).
 */
public class RotinasDAO {

    /** FUNCAO 2: estoque vendavel (lotes nao vencidos) de um produto. */
    public BigDecimal estoqueDisponivel(int cdProduto) throws SQLException {
        String sql = "SELECT fn_estoque_disponivel(?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cdProduto);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /**
     * PROCEDIMENTO 1: reajuste de precos por categoria.
     * Cada preco alterado dispara o TRIGGER 1, que grava em LOG_PRECO_PRODUTO.
     */
    public ResultadoConsulta reajustarPrecos(int cdCategoria, BigDecimal percentual) throws SQLException {
        return chamar("{CALL sp_reajustar_precos_categoria(?, ?)}", cs -> {
            cs.setInt(1, cdCategoria);
            cs.setBigDecimal(2, percentual);
        });
    }

    /** PROCEDIMENTO 2 (CURSOR): baixa de estoque pela regra FEFO; devolve o que saiu de cada lote. */
    public ResultadoConsulta baixarEstoqueFefo(int cdProduto, BigDecimal quantidade) throws SQLException {
        return chamar("{CALL sp_baixar_estoque_fefo(?, ?)}", cs -> {
            cs.setInt(1, cdProduto);
            cs.setBigDecimal(2, quantidade);
        });
    }

    /**
     * Registra uma perda. O TRIGGER 2 (BEFORE INSERT ON PERDA) valida a quantidade e
     * desconta do lote; aqui lemos o estoque do lote antes e depois para mostrar o efeito.
     * @return {quantidade antes, quantidade depois}
     */
    public BigDecimal[] registrarPerda(int cdProduto, int nrLote, BigDecimal quantidade,
                                       String motivo, boolean descarteEfetivado) throws SQLException {
        String sqlLote = "SELECT quantidade FROM ESTOQUE WHERE cd_produto=? AND nr_lote=?";
        String sqlPerda = "INSERT INTO PERDA (cd_produto, nr_lote, dt_perda, qt_perdida, motivo_perda, "
                        + "descarte_efetivado) VALUES (?, ?, CURDATE(), ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection()) {
            BigDecimal antes = quantidadeLote(conn, sqlLote, cdProduto, nrLote);
            try (PreparedStatement ps = conn.prepareStatement(sqlPerda)) {
                ps.setInt(1, cdProduto);
                ps.setInt(2, nrLote);
                ps.setBigDecimal(3, quantidade);
                ps.setString(4, motivo);
                ps.setBoolean(5, descarteEfetivado);
                ps.executeUpdate();
            }
            BigDecimal depois = quantidadeLote(conn, sqlLote, cdProduto, nrLote);
            return new BigDecimal[] {antes, depois};
        }
    }

    /** Conteudo da tabela de log preenchida pelo TRIGGER 1. */
    public ResultadoConsulta listarLogPrecos() throws SQLException {
        String sql = """
            SELECT id_log, dt_alteracao, cd_produto, nome_produto,
                   valor_antigo, valor_novo, variacao_pct, usuario
            FROM LOG_PRECO_PRODUTO
            ORDER BY id_log DESC
            """;
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return ler(rs, 0);
        }
    }

    // ---------------------------------------------------------------------------------

    private interface Parametros { void preencher(CallableStatement cs) throws SQLException; }

    /** Executa o procedimento numa transacao: tudo ou nada. */
    private ResultadoConsulta chamar(String sql, Parametros parametros) throws SQLException {
        long inicio = System.currentTimeMillis();
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try (CallableStatement cs = conn.prepareCall(sql)) {
                parametros.preencher(cs);
                ResultadoConsulta resultado = new ResultadoConsulta(List.of(), List.of(), 0);
                boolean temResultado = cs.execute();
                while (true) {
                    if (temResultado) {
                        try (ResultSet rs = cs.getResultSet()) {
                            resultado = ler(rs, inicio);
                        }
                    } else if (cs.getUpdateCount() == -1) {
                        break;
                    }
                    temResultado = cs.getMoreResults();
                }
                conn.commit();
                return resultado;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static BigDecimal quantidadeLote(Connection conn, String sql, int cdProduto, int nrLote)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cdProduto);
            ps.setInt(2, nrLote);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    private static ResultadoConsulta ler(ResultSet rs, long inicio) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        List<String> colunas = new ArrayList<>();
        for (int i = 1; i <= n; i++) colunas.add(md.getColumnLabel(i));
        List<Object[]> linhas = new ArrayList<>();
        while (rs.next()) {
            Object[] linha = new Object[n];
            for (int i = 1; i <= n; i++) linha[i - 1] = rs.getObject(i);
            linhas.add(linha);
        }
        return new ResultadoConsulta(colunas, linhas, inicio == 0 ? 0 : System.currentTimeMillis() - inicio);
    }
}
