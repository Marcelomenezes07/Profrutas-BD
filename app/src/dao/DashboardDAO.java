package dao;

import util.ConnectionFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas do dashboard. Todas recebem o mesmo Filtro (periodo, forma de pagamento e
 * categoria), aplicado com PreparedStatement.
 *
 * As medidas de venda sao calculadas a partir dos ITENS vendidos (POSSUI x PRODUTO): assim o
 * filtro de categoria considera so os itens daquela categoria. Sem filtro de categoria o
 * resultado e igual ao vl_total dos pedidos (a Consulta 6 confere que os valores batem).
 */
public class DashboardDAO {

    /** Filtro do dashboard. forma = "%" significa todas; categoria = 0 significa todas. */
    public record Filtro(LocalDate inicio, LocalDate fim, String forma, int categoria) {}

    public record Indicadores(int totalClientes, int clientesAtivos, int pedidos, double faturamento,
                              double ticketMedio, double percClientesAtivos, double percPedidosComDevolucao,
                              double prejuizoPerdas, double percPerdaSobreVendas) {}

    /** Condicao dos itens vendidos: 5 parametros, preenchidos por bind(). */
    private static final String FILTRO_ITENS = "\n" + """
            pe.dt_venda BETWEEN ? AND ?
              AND pe.forma_pagamento LIKE ?
              AND (? = 0 OR EXISTS (SELECT 1 FROM TEM t
                                    WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = ?))""";

    private static final String ITENS = """
            FROM PEDIDO pe
            JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
            JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
            WHERE """ + FILTRO_ITENS;

    public Indicadores indicadores(Filtro f) throws SQLException {
        String sqlVendas = """
            SELECT COUNT(DISTINCT pe.nr_venda_mes)                        AS pedidos,
                   COUNT(DISTINCT pe.nr_cliente)                          AS clientes_ativos,
                   COALESCE(SUM(ps.quantidade * p.valor_kg_ou_unitario), 0) AS faturamento,
                   (SELECT COUNT(*) FROM CLIENTE)                         AS total_clientes
            """ + ITENS;
        String sqlDevolucoes = """
            SELECT COUNT(*)
            FROM DEVOLUCAO d
            WHERE d.nr_venda_mes IN (SELECT pe.nr_venda_mes
            """ + ITENS + ")";
        String sqlPerdas = """
            SELECT COALESCE(SUM(pr.qt_perdida * p.valor_kg_ou_unitario), 0)
            FROM PERDA pr
            JOIN PRODUTO p ON p.cd_produto = pr.cd_produto
            WHERE pr.dt_perda BETWEEN ? AND ?
              AND (? = 0 OR EXISTS (SELECT 1 FROM TEM t
                                    WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = ?))
            """;
        try (Connection conn = ConnectionFactory.getConnection()) {
            int pedidos, ativos, total;
            double faturamento;
            try (PreparedStatement ps = conn.prepareStatement(sqlVendas)) {
                bind(ps, 1, f);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    pedidos = rs.getInt("pedidos");
                    ativos = rs.getInt("clientes_ativos");
                    faturamento = rs.getDouble("faturamento");
                    total = rs.getInt("total_clientes");
                }
            }
            int devolucoes = escalarInt(conn, sqlDevolucoes, f);
            double perdas;
            try (PreparedStatement ps = conn.prepareStatement(sqlPerdas)) {
                ps.setDate(1, Date.valueOf(f.inicio()));
                ps.setDate(2, Date.valueOf(f.fim()));
                ps.setInt(3, f.categoria());
                ps.setInt(4, f.categoria());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    perdas = rs.getDouble(1);
                }
            }
            return new Indicadores(total, ativos, pedidos, faturamento,
                pedidos == 0 ? 0 : faturamento / pedidos,
                total == 0 ? 0 : 100.0 * ativos / total,
                pedidos == 0 ? 0 : 100.0 * devolucoes / pedidos,
                perdas,
                faturamento == 0 ? 0 : 100.0 * perdas / faturamento);
        }
    }

    /** Variaveis disponiveis para a estatistica descritiva. */
    public enum Variavel {
        VALOR_PEDIDO("Valor do pedido (R$)", true, """
            SELECT SUM(ps.quantidade * p.valor_kg_ou_unitario)
            """ + ITENS + " GROUP BY pe.nr_venda_mes"),
        ITENS_PEDIDO("Itens por pedido", false, """
            SELECT COUNT(*)
            """ + ITENS + " GROUP BY pe.nr_venda_mes"),
        QTD_ITEM("Quantidade vendida por item (kg/un)", false, """
            SELECT ps.quantidade
            """ + ITENS);

        public final String rotulo;
        public final boolean moeda;
        public final String sql;

        Variavel(String rotulo, boolean moeda, String sql) {
            this.rotulo = rotulo;
            this.moeda = moeda;
            this.sql = sql;
        }

        @Override public String toString() { return rotulo; }
    }

    /** Amostra (lista de valores) da variavel escolhida, dentro do filtro. */
    public List<Double> amostra(Variavel v, Filtro f) throws SQLException {
        List<Double> valores = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(v.sql)) {
            bind(ps, 1, f);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) valores.add(rs.getDouble(1));
            }
        }
        return valores;
    }

    public Map<String, Double> faturamentoMensal(Filtro f) throws SQLException {
        return serie("""
            SELECT DATE_FORMAT(pe.dt_venda, '%Y-%m') AS mes,
                   SUM(ps.quantidade * p.valor_kg_ou_unitario) AS total
            """ + ITENS + """

            GROUP BY DATE_FORMAT(pe.dt_venda, '%Y-%m')
            ORDER BY mes
            """, f);
    }

    public Map<String, Double> faturamentoPorFormaPagamento(Filtro f) throws SQLException {
        return serie("""
            SELECT pe.forma_pagamento, SUM(ps.quantidade * p.valor_kg_ou_unitario) AS total
            """ + ITENS + """

            GROUP BY pe.forma_pagamento
            ORDER BY total DESC
            """, f);
    }

    public Map<String, Double> topProdutosVendidos(Filtro f) throws SQLException {
        return serie("""
            SELECT p.nome, SUM(ps.quantidade) AS qtd
            """ + ITENS + """

            GROUP BY p.cd_produto, p.nome
            ORDER BY qtd DESC
            LIMIT 10
            """, f);
    }

    public Map<String, Double> faturamentoPorCategoria(Filtro f) throws SQLException {
        return serie("""
            SELECT c.nm_categoria, SUM(ps.quantidade * p.valor_kg_ou_unitario) AS faturamento
            FROM PEDIDO pe
            JOIN POSSUI    ps ON ps.nr_venda_mes = pe.nr_venda_mes
            JOIN PRODUTO   p  ON p.cd_produto    = ps.cd_produto
            JOIN TEM       tc ON tc.cd_produto   = p.cd_produto
            JOIN CATEGORIA c  ON c.cd_categoria  = tc.cd_categoria
            WHERE """ + FILTRO_ITENS + """

              AND (? = 0 OR c.cd_categoria = ?)
            GROUP BY c.cd_categoria, c.nm_categoria
            ORDER BY faturamento DESC
            LIMIT 8
            """, f, f.categoria(), f.categoria());
    }

    /** Perdas: filtro por periodo (data da perda) e categoria; forma de pagamento nao se aplica. */
    public Map<String, Double> perdasPorMotivo(Filtro f) throws SQLException {
        String sql = """
            SELECT COALESCE(pr.motivo_perda, 'Nao informado'), SUM(pr.qt_perdida) AS qtd
            FROM PERDA pr
            JOIN PRODUTO p ON p.cd_produto = pr.cd_produto
            WHERE pr.dt_perda BETWEEN ? AND ?
              AND (? = 0 OR EXISTS (SELECT 1 FROM TEM t
                                    WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = ?))
            GROUP BY pr.motivo_perda
            ORDER BY qtd DESC
            """;
        Map<String, Double> dados = new LinkedHashMap<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(f.inicio()));
            ps.setDate(2, Date.valueOf(f.fim()));
            ps.setInt(3, f.categoria());
            ps.setInt(4, f.categoria());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) dados.put(rs.getString(1), rs.getDouble(2));
            }
        }
        return dados;
    }

    /** Devolucoes dos pedidos que entram no filtro, por forma de resolucao. */
    public Map<String, Double> devolucoesPorResolucao(Filtro f) throws SQLException {
        return serie("""
            SELECT COALESCE(d.forma_resolucao, 'Nao informada') AS resolucao, COUNT(*) AS qtd
            FROM DEVOLUCAO d
            WHERE d.nr_venda_mes IN (SELECT pe.nr_venda_mes
            """ + ITENS + """
            )
            GROUP BY d.forma_resolucao
            ORDER BY qtd DESC
            """, f);
    }

    // ---------------------------------------------------------------------------------

    /** Preenche os 5 parametros de FILTRO_ITENS a partir da posicao indicada. */
    private static int bind(PreparedStatement ps, int pos, Filtro f) throws SQLException {
        ps.setDate(pos++, Date.valueOf(f.inicio()));
        ps.setDate(pos++, Date.valueOf(f.fim()));
        ps.setString(pos++, f.forma());
        ps.setInt(pos++, f.categoria());
        ps.setInt(pos++, f.categoria());
        return pos;
    }

    private static int escalarInt(Connection conn, String sql, Filtro f) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, 1, f);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Consulta de 2 colunas (rotulo, valor) com o filtro e, opcionalmente, inteiros extras. */
    private static Map<String, Double> serie(String sql, Filtro f, int... extras) throws SQLException {
        Map<String, Double> dados = new LinkedHashMap<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int pos = bind(ps, 1, f);
            for (int x : extras) ps.setInt(pos++, x);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rotulo = rs.getString(1);
                    dados.put(rotulo == null ? "(vazio)" : rotulo, rs.getDouble(2));
                }
            }
        }
        return dados;
    }
}
