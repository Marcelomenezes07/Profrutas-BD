package dao;

import model.Estoque;
import util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD da tabela ESTOQUE (lotes). A listagem usa as funcoes da Etapa 05:
 * fn_situacao_validade (situacao do lote) e fn_estoque_disponivel (estoque vendavel do produto).
 */
public class EstoqueDAO {
    public void inserir(Estoque e) throws SQLException {
        String sql = "INSERT INTO ESTOQUE (cd_produto, nr_lote, quantidade, dt_entrada, dt_validade) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, e.getCdProduto());
            ps.setInt(2, e.getNrLote());
            ps.setBigDecimal(3, e.getQuantidade());
            ps.setDate(4, Date.valueOf(e.getDtEntrada()));
            ps.setDate(5, Date.valueOf(e.getDtValidade()));
            ps.executeUpdate();
        }
    }

    public List<Estoque> listar() throws SQLException {
        List<Estoque> lista = new ArrayList<>();
        String sql = """
            SELECT e.cd_produto, p.nome, e.nr_lote, e.quantidade, e.dt_entrada, e.dt_validade,
                   fn_situacao_validade(e.dt_validade)  AS situacao,
                   fn_estoque_disponivel(e.cd_produto)  AS disponivel_produto
            FROM ESTOQUE e
            JOIN PRODUTO p ON p.cd_produto = e.cd_produto
            ORDER BY p.nome, e.dt_validade, e.nr_lote
            """;
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Estoque e = new Estoque();
                e.setCdProduto(rs.getInt("cd_produto"));
                e.setNomeProduto(rs.getString("nome"));
                e.setNrLote(rs.getInt("nr_lote"));
                e.setQuantidade(rs.getBigDecimal("quantidade"));
                e.setDtEntrada(rs.getDate("dt_entrada").toLocalDate());
                e.setDtValidade(rs.getDate("dt_validade").toLocalDate());
                e.setSituacao(rs.getString("situacao"));
                e.setDisponivelProduto(rs.getBigDecimal("disponivel_produto"));
                lista.add(e);
            }
        }
        return lista;
    }

    /** A chave (produto + lote) nao muda: altera quantidade e datas do lote. */
    public void atualizar(Estoque e) throws SQLException {
        String sql = "UPDATE ESTOQUE SET quantidade=?, dt_entrada=?, dt_validade=? "
                   + "WHERE cd_produto=? AND nr_lote=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, e.getQuantidade());
            ps.setDate(2, Date.valueOf(e.getDtEntrada()));
            ps.setDate(3, Date.valueOf(e.getDtValidade()));
            ps.setInt(4, e.getCdProduto());
            ps.setInt(5, e.getNrLote());
            ps.executeUpdate();
        }
    }

    /** Lotes com perdas registradas nao podem ser excluidos (FK de PERDA, ON DELETE RESTRICT). */
    public void excluir(int cdProduto, int nrLote) throws SQLException {
        String sql = "DELETE FROM ESTOQUE WHERE cd_produto=? AND nr_lote=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cdProduto);
            ps.setInt(2, nrLote);
            ps.executeUpdate();
        }
    }

    /** Proximo numero de lote livre para o produto (sugestao no cadastro). */
    public int proximoLote(int cdProduto) throws SQLException {
        String sql = "SELECT COALESCE(MAX(nr_lote), 0) + 1 FROM ESTOQUE WHERE cd_produto=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cdProduto);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
