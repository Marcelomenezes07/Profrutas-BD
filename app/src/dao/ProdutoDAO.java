package dao;

import model.Produto;
import util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {
    public void inserir(Produto p) throws SQLException {
        String sql = "INSERT INTO PRODUTO (nome, descricao, valor_kg_ou_unitario, vendido_unitario, marca) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getDescricao());
            ps.setBigDecimal(3, p.getValor());
            ps.setBoolean(4, p.isVendidoUnitario());
            ps.setString(5, p.getMarca());
            ps.executeUpdate();
        }
    }

    public List<Produto> listar() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT cd_produto, nome, descricao, valor_kg_ou_unitario, vendido_unitario, marca "
                   + "FROM PRODUTO ORDER BY cd_produto";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Produto(
                    rs.getInt("cd_produto"),
                    rs.getString("nome"),
                    rs.getString("descricao"),
                    rs.getBigDecimal("valor_kg_ou_unitario"),
                    rs.getBoolean("vendido_unitario"),
                    rs.getString("marca")
                ));
            }
        }
        return lista;
    }

    public void atualizar(Produto p) throws SQLException {
        String sql = "UPDATE PRODUTO SET nome=?, descricao=?, valor_kg_ou_unitario=?, vendido_unitario=?, marca=? "
                   + "WHERE cd_produto=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getDescricao());
            ps.setBigDecimal(3, p.getValor());
            ps.setBoolean(4, p.isVendidoUnitario());
            ps.setString(5, p.getMarca());
            ps.setInt(6, p.getCdProduto());
            ps.executeUpdate();
        }
    }

    public void excluir(int cdProduto) throws SQLException {
        String sql = "DELETE FROM PRODUTO WHERE cd_produto=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cdProduto);
            ps.executeUpdate();
        }
    }
}
