package dao;

import model.ItemLista;
import util.ConnectionFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Listas usadas para preencher os campos de selecao das telas (produtos, categorias, lotes...). */
public class ReferenciaDAO {

    public List<ItemLista> listarProdutos() throws SQLException {
        return lista("SELECT cd_produto, nome FROM PRODUTO ORDER BY nome");
    }

    public List<ItemLista> listarCategorias() throws SQLException {
        return lista("SELECT cd_categoria, nm_categoria FROM CATEGORIA ORDER BY nm_categoria");
    }

    /** Categorias que possuem ao menos um produto (as unicas em que um reajuste tem efeito). */
    public List<ItemLista> listarCategoriasComProdutos() throws SQLException {
        return lista("""
            SELECT c.cd_categoria,
                   CONCAT(c.nm_categoria, ' (', COUNT(t.cd_produto), ' produto(s))')
            FROM CATEGORIA c
            JOIN TEM t ON t.cd_categoria = c.cd_categoria
            GROUP BY c.cd_categoria, c.nm_categoria
            ORDER BY c.nm_categoria
            """);
    }

    /** Lotes de um produto, com a quantidade atual no rotulo. */
    public List<ItemLista> listarLotes(int cdProduto) throws SQLException {
        String sql = """
            SELECT nr_lote,
                   CONCAT('Lote ', nr_lote, ' - ', quantidade, ' em estoque - validade ',
                          DATE_FORMAT(dt_validade, '%d/%m/%Y'))
            FROM ESTOQUE
            WHERE cd_produto = ?
            ORDER BY dt_validade
            """;
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cdProduto);
            try (ResultSet rs = ps.executeQuery()) {
                List<ItemLista> itens = new ArrayList<>();
                while (rs.next()) itens.add(new ItemLista(rs.getInt(1), rs.getString(2)));
                return itens;
            }
        }
    }

    /** Menor e maior data com movimento (vendas e perdas): periodo padrao dos filtros. */
    public LocalDate[] periodoDosDados() throws SQLException {
        String sql = """
            SELECT LEAST(COALESCE((SELECT MIN(dt_venda) FROM PEDIDO), CURDATE()),
                         COALESCE((SELECT MIN(dt_perda) FROM PERDA),  CURDATE())) AS inicio,
                   GREATEST(COALESCE((SELECT MAX(dt_venda) FROM PEDIDO), CURDATE()),
                            COALESCE((SELECT MAX(dt_perda) FROM PERDA),  CURDATE())) AS fim
            """;
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return new LocalDate[] {rs.getDate("inicio").toLocalDate(), rs.getDate("fim").toLocalDate()};
        }
    }

    private List<ItemLista> lista(String sql) throws SQLException {
        List<ItemLista> itens = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) itens.add(new ItemLista(rs.getInt(1), rs.getString(2)));
        }
        return itens;
    }
}
