package dao;

import util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas que geram os pares (X, Y) do grafico de dispersao.
 * Cada SQL devolve 3 colunas: rotulo do ponto, X e Y.
 */
public class CorrelacaoDAO {

    public record Par(String titulo, String eixoX, String eixoY, String sql) {
        @Override public String toString() { return titulo; }
    }

    public record Ponto(String rotulo, double x, double y) {}

    public static final List<Par> PARES = List.of(
        new Par("Pedidos: quantidade de itens x valor total",
            "Quantidade total do pedido (kg/un)", "Valor total do pedido (R$)",
            """
            SELECT CONCAT('Pedido ', pe.nr_venda_mes) AS rotulo,
                   SUM(ps.quantidade)                 AS x,
                   pe.vl_total                        AS y
            FROM PEDIDO pe
            JOIN POSSUI ps ON ps.nr_venda_mes = pe.nr_venda_mes
            GROUP BY pe.nr_venda_mes, pe.vl_total
            """),

        new Par("Clientes: numero de pedidos x total gasto",
            "Numero de pedidos do cliente", "Total gasto pelo cliente (R$)",
            """
            SELECT CONCAT('Cliente ', cl.nr_cliente) AS rotulo,
                   COUNT(pe.nr_venda_mes)             AS x,
                   SUM(pe.vl_total)                   AS y
            FROM CLIENTE cl
            JOIN PEDIDO pe ON pe.nr_cliente = cl.nr_cliente
            GROUP BY cl.nr_cliente
            """),

        new Par("Produtos: preco x faturamento",
            "Preco do produto (R$ por kg/un)", "Faturamento do produto (R$)",
            """
            SELECT p.nome                                      AS rotulo,
                   p.valor_kg_ou_unitario                      AS x,
                   SUM(ps.quantidade * p.valor_kg_ou_unitario) AS y
            FROM PRODUTO p
            JOIN POSSUI ps ON ps.cd_produto = p.cd_produto
            GROUP BY p.cd_produto, p.nome, p.valor_kg_ou_unitario
            """),

        new Par("Produtos: preco x quantidade vendida",
            "Preco do produto (R$ por kg/un)", "Quantidade vendida (kg/un)",
            """
            SELECT p.nome                 AS rotulo,
                   p.valor_kg_ou_unitario AS x,
                   SUM(ps.quantidade)     AS y
            FROM PRODUTO p
            JOIN POSSUI ps ON ps.cd_produto = p.cd_produto
            GROUP BY p.cd_produto, p.nome, p.valor_kg_ou_unitario
            """),

        new Par("Lotes: vida util x quantidade perdida",
            "Vida util do lote (dias entre entrada e validade)", "Quantidade perdida (kg/un)",
            """
            SELECT CONCAT(p.nome, ' - lote ', e.nr_lote)     AS rotulo,
                   DATEDIFF(e.dt_validade, e.dt_entrada)     AS x,
                   SUM(pr.qt_perdida)                        AS y
            FROM ESTOQUE e
            JOIN PRODUTO p ON p.cd_produto = e.cd_produto
            JOIN PERDA pr  ON pr.cd_produto = e.cd_produto
                          AND pr.nr_lote    = e.nr_lote
            GROUP BY e.cd_produto, e.nr_lote, p.nome, e.dt_validade, e.dt_entrada
            """)
    );

    public List<Ponto> pontos(Par par) throws SQLException {
        List<Ponto> pontos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(par.sql())) {
            while (rs.next()) {
                pontos.add(new Ponto(rs.getString("rotulo"), rs.getDouble("x"), rs.getDouble("y")));
            }
        }
        return pontos;
    }
}
