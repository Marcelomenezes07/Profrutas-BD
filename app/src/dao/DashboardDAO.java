package dao;

import util.ConnectionFactory;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Consultas usadas pelos indicadores e graficos do dashboard. */
public class DashboardDAO {

    public record Indicadores(int clientes, int pedidos, double faturamento, double ticketMedio,
                              int produtos, double prejuizoPerdas, int devolucoes) {}

    public Indicadores indicadores() throws SQLException {
        String sql = """
            SELECT (SELECT COUNT(*) FROM CLIENTE)                                AS total_clientes,
                   (SELECT COUNT(*) FROM PEDIDO)                                 AS total_pedidos,
                   (SELECT COALESCE(SUM(vl_total), 0) FROM PEDIDO)               AS faturamento,
                   (SELECT COALESCE(AVG(vl_total), 0) FROM PEDIDO)               AS ticket_medio,
                   (SELECT COUNT(*) FROM PRODUTO)                                AS total_produtos,
                   (SELECT COALESCE(SUM(pr.qt_perdida * p.valor_kg_ou_unitario), 0)
                      FROM PERDA pr JOIN PRODUTO p ON p.cd_produto = pr.cd_produto) AS prejuizo_perdas,
                   (SELECT COUNT(*) FROM DEVOLUCAO)                              AS total_devolucoes
            """;
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return new Indicadores(
                rs.getInt("total_clientes"),
                rs.getInt("total_pedidos"),
                rs.getDouble("faturamento"),
                rs.getDouble("ticket_medio"),
                rs.getInt("total_produtos"),
                rs.getDouble("prejuizo_perdas"),
                rs.getInt("total_devolucoes"));
        }
    }

    public Map<String, Double> faturamentoPorFormaPagamento() throws SQLException {
        return serie("""
            SELECT forma_pagamento, SUM(vl_total) AS total
            FROM PEDIDO
            GROUP BY forma_pagamento
            ORDER BY total DESC
            """);
    }

    public Map<String, Double> faturamentoMensal() throws SQLException {
        return serie("""
            SELECT DATE_FORMAT(dt_venda, '%Y-%m') AS mes, SUM(vl_total) AS total
            FROM PEDIDO
            GROUP BY DATE_FORMAT(dt_venda, '%Y-%m')
            ORDER BY mes
            """);
    }

    public Map<String, Double> topProdutosVendidos() throws SQLException {
        return serie("""
            SELECT p.nome, SUM(ps.quantidade) AS qtd
            FROM POSSUI ps
            JOIN PRODUTO p ON p.cd_produto = ps.cd_produto
            GROUP BY p.cd_produto, p.nome
            ORDER BY qtd DESC
            LIMIT 10
            """);
    }

    public Map<String, Double> faturamentoPorCategoria() throws SQLException {
        return serie("""
            SELECT c.nm_categoria, SUM(ps.quantidade * p.valor_kg_ou_unitario) AS faturamento
            FROM CATEGORIA c
            JOIN TEM     t  ON t.cd_categoria = c.cd_categoria
            JOIN PRODUTO p  ON p.cd_produto   = t.cd_produto
            JOIN POSSUI  ps ON ps.cd_produto  = p.cd_produto
            GROUP BY c.cd_categoria, c.nm_categoria
            ORDER BY faturamento DESC
            LIMIT 8
            """);
    }

    public Map<String, Double> perdasPorMotivo() throws SQLException {
        return serie("""
            SELECT motivo_perda, SUM(qt_perdida) AS qtd
            FROM PERDA
            GROUP BY motivo_perda
            ORDER BY qtd DESC
            """);
    }

    public Map<String, Double> devolucoesPorResolucao() throws SQLException {
        return serie("""
            SELECT COALESCE(forma_resolucao, 'Nao informada') AS resolucao, COUNT(*) AS qtd
            FROM DEVOLUCAO
            GROUP BY forma_resolucao
            ORDER BY qtd DESC
            """);
    }

    /** Executa uma consulta de 2 colunas (rotulo, valor) e devolve na ordem do ORDER BY. */
    private Map<String, Double> serie(String sql) throws SQLException {
        Map<String, Double> dados = new LinkedHashMap<>();
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String rotulo = rs.getString(1);
                dados.put(rotulo == null ? "(vazio)" : rotulo, rs.getDouble(2));
            }
        }
        return dados;
    }
}
