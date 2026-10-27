package dao;

import model.Consulta;

import java.util.List;

/**
 * SQL explicito das consultas exibidas na aba "Consultas".
 * O mesmo SQL esta documentado no arquivo consultas.sql (entregavel).
 */
public final class Consultas {
    private Consultas() {}

    public static final List<Consulta> TODAS = List.of(
        new Consulta("1. Faturamento por categoria",
            "Receita obtida em cada categoria de produto, a partir dos itens vendidos (POSSUI) x preco (PRODUTO), com participacao percentual no total. JOIN entre CATEGORIA, TEM, PRODUTO e POSSUI + funcao de janela.",
            """
            SELECT c.nm_categoria                                          AS categoria,
                   COUNT(DISTINCT p.cd_produto)                            AS qtd_produtos,
                   COUNT(DISTINCT ps.nr_venda_mes)                         AS qtd_pedidos,
                   ROUND(SUM(ps.quantidade), 3)                            AS qtd_vendida,
                   ROUND(SUM(ps.quantidade * p.valor_kg_ou_unitario), 2)   AS faturamento,
                   ROUND(100 * SUM(ps.quantidade * p.valor_kg_ou_unitario)
                         / SUM(SUM(ps.quantidade * p.valor_kg_ou_unitario)) OVER (), 2) AS perc_faturamento
            FROM CATEGORIA c
            JOIN TEM     t  ON t.cd_categoria = c.cd_categoria
            JOIN PRODUTO p  ON p.cd_produto   = t.cd_produto
            JOIN POSSUI  ps ON ps.cd_produto  = p.cd_produto
            GROUP BY c.cd_categoria, c.nm_categoria
            ORDER BY faturamento DESC
            """),

        new Consulta("2. Ranking de clientes",
            "Clientes ordenados pelo valor total gasto, com ticket medio, ultima compra, tipo (PF/PJ) e quantidade de devolucoes. JOIN + LEFT JOIN, HAVING parametrizado e RANK().",
            """
            SELECT RANK() OVER (ORDER BY SUM(pe.vl_total) DESC)             AS posicao,
                   cl.nr_cliente,
                   cl.cpf_cnpj,
                   CASE WHEN CHAR_LENGTH(cl.cpf_cnpj) > 14
                        THEN 'Pessoa Juridica' ELSE 'Pessoa Fisica' END     AS tipo_cliente,
                   cl.bairro,
                   COUNT(pe.nr_venda_mes)                                   AS qtd_pedidos,
                   SUM(pe.vl_total)                                         AS total_gasto,
                   ROUND(AVG(pe.vl_total), 2)                               AS ticket_medio,
                   MAX(pe.dt_venda)                                         AS ultima_compra,
                   COUNT(d.n_devolucao)                                     AS qtd_devolucoes
            FROM CLIENTE cl
            JOIN PEDIDO pe         ON pe.nr_cliente  = cl.nr_cliente
            LEFT JOIN DEVOLUCAO d  ON d.nr_venda_mes = pe.nr_venda_mes
            GROUP BY cl.nr_cliente, cl.cpf_cnpj, cl.bairro
            HAVING COUNT(pe.nr_venda_mes) >= ?
            ORDER BY total_gasto DESC
            """,
            Consulta.TipoParametro.INTEIRO, "Minimo de pedidos", "1"),

        new Consulta("3. Hierarquia de funcionarios",
            "Arvore completa de supervisao (auto-relacionamento) com nivel, supervisor direto, numero de subordinados e cadeia hierarquica. CTE recursiva + auto-JOIN + subconsulta correlacionada.",
            """
            WITH RECURSIVE hierarquia AS (
                SELECT f.nr_funcionario,
                       f.nome,
                       f.nr_funcionario_supervisor,
                       0                            AS nivel,
                       CAST(f.nome AS CHAR(1000))   AS cadeia
                FROM FUNCIONARIO f
                WHERE f.nr_funcionario_supervisor IS NULL
                UNION ALL
                SELECT f.nr_funcionario,
                       f.nome,
                       f.nr_funcionario_supervisor,
                       h.nivel + 1,
                       CONCAT(h.cadeia, ' > ', f.nome)
                FROM FUNCIONARIO f
                JOIN hierarquia h ON f.nr_funcionario_supervisor = h.nr_funcionario
            )
            SELECT h.nr_funcionario,
                   CONCAT(REPEAT('    ', h.nivel), h.nome)                  AS funcionario,
                   h.nivel,
                   COALESCE(s.nome, '(topo da hierarquia)')                 AS supervisor,
                   (SELECT COUNT(*)
                      FROM FUNCIONARIO x
                     WHERE x.nr_funcionario_supervisor = h.nr_funcionario)  AS subordinados_diretos,
                   h.cadeia                                                 AS cadeia_hierarquica
            FROM hierarquia h
            LEFT JOIN FUNCIONARIO s ON s.nr_funcionario = h.nr_funcionario_supervisor
            ORDER BY h.cadeia
            """),

        new Consulta("4. Indice de perdas por lote",
            "Quantidade perdida, percentual de perda do lote, prejuizo estimado e descartes pendentes. JOIN com chave estrangeira composta (PERDA -> ESTOQUE) e PRODUTO.",
            """
            SELECT p.cd_produto,
                   p.nome                                                   AS produto,
                   e.nr_lote,
                   e.quantidade                                             AS qtd_em_estoque,
                   COUNT(pr.nr_perda)                                       AS ocorrencias,
                   SUM(pr.qt_perdida)                                       AS qtd_perdida,
                   ROUND(100 * SUM(pr.qt_perdida)
                         / (e.quantidade + SUM(pr.qt_perdida)), 2)          AS perc_perda_lote,
                   ROUND(SUM(pr.qt_perdida * p.valor_kg_ou_unitario), 2)    AS prejuizo_estimado,
                   SUM(CASE WHEN pr.descarte_efetivado THEN 0 ELSE 1 END)   AS descartes_pendentes
            FROM PERDA pr
            JOIN ESTOQUE e ON e.cd_produto = pr.cd_produto
                          AND e.nr_lote    = pr.nr_lote
            JOIN PRODUTO p ON p.cd_produto = e.cd_produto
            GROUP BY p.cd_produto, p.nome, e.nr_lote, e.quantidade
            ORDER BY prejuizo_estimado DESC
            """),

        new Consulta("5. Lotes vencidos / a vencer",
            "Lotes com estoque vencidos ou que vencem nos proximos N dias, com valor em risco e fornecedores do produto. LEFT JOIN em cadeia, GROUP_CONCAT e funcoes de data.",
            """
            SELECT p.nome                                                   AS produto,
                   e.nr_lote,
                   e.quantidade,
                   e.dt_entrada,
                   e.dt_validade,
                   DATEDIFF(e.dt_validade, CURDATE())                       AS dias_para_vencer,
                   CASE WHEN e.dt_validade <  CURDATE() THEN 'VENCIDO'
                        WHEN e.dt_validade <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 'CRITICO'
                        ELSE 'ATENCAO' END                                  AS situacao,
                   ROUND(e.quantidade * p.valor_kg_ou_unitario, 2)          AS valor_em_risco,
                   COALESCE(GROUP_CONCAT(DISTINCT CONCAT(f.nome, ' (', fo.modo_transporte, ')')
                            ORDER BY f.nome SEPARATOR ', '), '-')           AS fornecedores
            FROM ESTOQUE e
            JOIN PRODUTO p          ON p.cd_produto     = e.cd_produto
            LEFT JOIN FORNECE fo    ON fo.cd_produto    = p.cd_produto
            LEFT JOIN FORNECEDOR f  ON f.cd_fornecedor  = fo.cd_fornecedor
            WHERE e.quantidade > 0
              AND e.dt_validade <= DATE_ADD(CURDATE(), INTERVAL ? DAY)
            GROUP BY p.cd_produto, p.nome, e.nr_lote, e.quantidade, e.dt_entrada, e.dt_validade, p.valor_kg_ou_unitario
            ORDER BY e.dt_validade
            """,
            Consulta.TipoParametro.INTEIRO, "Janela (dias a partir de hoje)", "30"),

        new Consulta("6. Conferencia de pedidos",
            "Itens de cada pedido, valor registrado x valor recalculado pelos itens e situacao da devolucao. JOIN entre 4 tabelas + LEFT JOIN, GROUP_CONCAT e CASE. Use % para todas as formas de pagamento.",
            """
            SELECT pe.nr_venda_mes,
                   pe.dt_venda,
                   cl.cpf_cnpj                                              AS cliente,
                   pe.forma_pagamento,
                   COUNT(ps.cd_produto)                                     AS qtd_itens,
                   GROUP_CONCAT(CONCAT(p.nome, ' x', ps.quantidade)
                                ORDER BY p.nome SEPARATOR '; ')             AS itens,
                   pe.vl_total                                              AS vl_registrado,
                   ROUND(SUM(ps.quantidade * p.valor_kg_ou_unitario), 2)    AS vl_calculado,
                   CASE WHEN ABS(pe.vl_total - SUM(ps.quantidade * p.valor_kg_ou_unitario)) <= 0.01
                        THEN 'OK' ELSE 'DIVERGENTE' END                     AS conferencia,
                   CASE WHEN d.n_devolucao IS NULL THEN 'Sem devolucao'
                        WHEN d.devolucao_efetivada  THEN CONCAT('Devolvido (', d.forma_resolucao, ')')
                        ELSE CONCAT('Devolucao pendente (', d.forma_resolucao, ')') END AS devolucao
            FROM PEDIDO pe
            JOIN CLIENTE cl        ON cl.nr_cliente   = pe.nr_cliente
            JOIN POSSUI  ps        ON ps.nr_venda_mes = pe.nr_venda_mes
            JOIN PRODUTO p         ON p.cd_produto    = ps.cd_produto
            LEFT JOIN DEVOLUCAO d  ON d.nr_venda_mes  = pe.nr_venda_mes
            WHERE pe.forma_pagamento LIKE ?
            GROUP BY pe.nr_venda_mes, pe.dt_venda, cl.cpf_cnpj, pe.forma_pagamento, pe.vl_total,
                     d.n_devolucao, d.devolucao_efetivada, d.forma_resolucao
            ORDER BY pe.dt_venda, pe.nr_venda_mes
            """,
            Consulta.TipoParametro.TEXTO, "Forma de pagamento (% = todas)", "%"),

        new Consulta("7. Vendas mensais x forma de pagamento",
            "Tabela pivot: faturamento de cada mes distribuido pelas formas de pagamento, com linha de total (WITH ROLLUP). Agregacao condicional com SUM + CASE.",
            """
            SELECT COALESCE(mes, 'TOTAL')                                   AS mes,
                   qtd_pedidos, clientes_distintos, faturamento,
                   dinheiro, cartao_credito, cartao_debito, pix, boleto
            FROM (
                SELECT DATE_FORMAT(dt_venda, '%Y-%m')                                       AS mes,
                       COUNT(*)                                                             AS qtd_pedidos,
                       COUNT(DISTINCT nr_cliente)                                           AS clientes_distintos,
                       SUM(vl_total)                                                        AS faturamento,
                       SUM(CASE WHEN forma_pagamento = 'Dinheiro'          THEN vl_total ELSE 0 END) AS dinheiro,
                       SUM(CASE WHEN forma_pagamento = 'Cartao de Credito' THEN vl_total ELSE 0 END) AS cartao_credito,
                       SUM(CASE WHEN forma_pagamento = 'Cartao de Debito'  THEN vl_total ELSE 0 END) AS cartao_debito,
                       SUM(CASE WHEN forma_pagamento = 'Pix'               THEN vl_total ELSE 0 END) AS pix,
                       SUM(CASE WHEN forma_pagamento = 'Boleto'            THEN vl_total ELSE 0 END) AS boleto
                FROM PEDIDO
                GROUP BY DATE_FORMAT(dt_venda, '%Y-%m') WITH ROLLUP
            ) v
            ORDER BY (v.mes IS NULL), v.mes
            """),

        new Consulta("8. Produtos nunca vendidos",
            "Produtos sem nenhuma venda registrada, com estoque atual, numero de fornecedores e categorias - candidatos a promocao. NOT EXISTS + subconsultas correlacionadas.",
            """
            SELECT p.cd_produto,
                   p.nome,
                   p.marca,
                   p.valor_kg_ou_unitario,
                   CASE WHEN p.vendido_unitario THEN 'Unidade' ELSE 'Kg' END AS vendido_por,
                   (SELECT COALESCE(SUM(e.quantidade), 0)
                      FROM ESTOQUE e WHERE e.cd_produto = p.cd_produto)      AS qtd_em_estoque,
                   (SELECT COUNT(*)
                      FROM FORNECE fo WHERE fo.cd_produto = p.cd_produto)    AS qtd_fornecedores,
                   COALESCE(GROUP_CONCAT(c.nm_categoria SEPARATOR ', '), '-') AS categorias
            FROM PRODUTO p
            LEFT JOIN TEM t       ON t.cd_produto   = p.cd_produto
            LEFT JOIN CATEGORIA c ON c.cd_categoria = t.cd_categoria
            WHERE NOT EXISTS (SELECT 1 FROM POSSUI ps WHERE ps.cd_produto = p.cd_produto)
            GROUP BY p.cd_produto, p.nome, p.marca, p.valor_kg_ou_unitario, p.vendido_unitario
            ORDER BY qtd_em_estoque DESC
            """),

        // ---- Entrega 4: novas consultas e visoes (Parte 5 do consultas.sql) ----
        //ENTREGA 4
        new Consulta("9. Clientes que nunca compraram (anti join)",
            "ANTI JOIN: CLIENTE LEFT JOIN PEDIDO ... WHERE pedido IS NULL. Devolve os clientes cadastrados sem nenhum pedido - lista para campanha de primeira compra.",
            """
            SELECT cl.nr_cliente,
                   cl.cpf_cnpj,
                   CASE WHEN CHAR_LENGTH(cl.cpf_cnpj) > 14
                        THEN 'Pessoa Juridica' ELSE 'Pessoa Fisica' END AS tipo_cliente,
                   COALESCE(cl.telefone, cl.telefone2, '(sem telefone)') AS contato,
                   cl.bairro
            FROM CLIENTE cl
            LEFT JOIN PEDIDO pe ON pe.nr_cliente = cl.nr_cliente
            WHERE pe.nr_venda_mes IS NULL
            ORDER BY cl.bairro, cl.nr_cliente
            """),

        new Consulta("10. Vendido x perdido por produto (full outer join)",
            "FULL OUTER JOIN emulado (o MySQL nao tem a sintaxe): LEFT JOIN + UNION ALL + RIGHT JOIN ... IS NULL entre o total vendido e o total perdido por produto. Mostra produtos so vendidos, so perdidos e com ambos.",
            """
            WITH vendas AS (
                SELECT ps.cd_produto,
                       SUM(ps.quantidade)                         AS qtd_vendida,
                       SUM(ps.quantidade * p.valor_kg_ou_unitario) AS valor_vendido
                FROM POSSUI ps
                JOIN PRODUTO p ON p.cd_produto = ps.cd_produto
                GROUP BY ps.cd_produto
            ),
            perdas AS (
                SELECT pr.cd_produto,
                       SUM(pr.qt_perdida)                          AS qtd_perdida,
                       SUM(pr.qt_perdida * p.valor_kg_ou_unitario) AS valor_perdido
                FROM PERDA pr
                JOIN PRODUTO p ON p.cd_produto = pr.cd_produto
                GROUP BY pr.cd_produto
            ),
            comparativo AS (
                -- parte 1: todos os produtos vendidos, com ou sem perda
                SELECT v.cd_produto,
                       v.qtd_vendida, v.valor_vendido,
                       pr.qtd_perdida, pr.valor_perdido
                FROM vendas v
                LEFT JOIN perdas pr ON pr.cd_produto = v.cd_produto
                UNION ALL
                -- parte 2: produtos com perda que nunca foram vendidos
                SELECT pr.cd_produto,
                       v.qtd_vendida, v.valor_vendido,
                       pr.qtd_perdida, pr.valor_perdido
                FROM vendas v
                RIGHT JOIN perdas pr ON pr.cd_produto = v.cd_produto
                WHERE v.cd_produto IS NULL
            )
            SELECT c.cd_produto,
                   p.nome                                         AS produto,
                   COALESCE(c.qtd_vendida, 0)                     AS qtd_vendida,
                   COALESCE(ROUND(c.valor_vendido, 2), 0)         AS valor_vendido,
                   COALESCE(c.qtd_perdida, 0)                     AS qtd_perdida,
                   COALESCE(ROUND(c.valor_perdido, 2), 0)         AS valor_perdido,
                   CASE WHEN c.qtd_vendida IS NULL THEN 'So perda (nunca vendido)'
                        WHEN c.qtd_perdida IS NULL THEN 'So venda (sem perda)'
                        WHEN c.qtd_perdida > c.qtd_vendida THEN 'Perde mais do que vende'
                        ELSE 'Venda e perda' END                  AS situacao
            FROM comparativo c
            JOIN PRODUTO p ON p.cd_produto = c.cd_produto
            ORDER BY situacao, valor_perdido DESC
            """),

        new Consulta("11. Clientes acima da media de gasto (subconsultas)",
            "SUBCONSULTAS: tabela derivada com o gasto por cliente + subconsulta escalar com a media desses gastos (media de uma soma). Identifica os melhores clientes.",
            """
            SELECT g.nr_cliente,
                   cl.cpf_cnpj,
                   cl.bairro,
                   g.qtd_pedidos,
                   g.total_gasto,
                   ROUND((SELECT AVG(t.total)
                            FROM (SELECT SUM(vl_total) AS total
                                    FROM PEDIDO
                                   GROUP BY nr_cliente) t), 2)       AS media_geral,
                   ROUND(g.total_gasto
                         - (SELECT AVG(t.total)
                              FROM (SELECT SUM(vl_total) AS total
                                      FROM PEDIDO
                                     GROUP BY nr_cliente) t), 2)     AS acima_da_media
            FROM (SELECT nr_cliente,
                         COUNT(*)      AS qtd_pedidos,
                         SUM(vl_total) AS total_gasto
                    FROM PEDIDO
                   GROUP BY nr_cliente) g
            JOIN CLIENTE cl ON cl.nr_cliente = g.nr_cliente
            WHERE g.total_gasto > (SELECT AVG(t.total)
                                     FROM (SELECT SUM(vl_total) AS total
                                             FROM PEDIDO
                                            GROUP BY nr_cliente) t)
            ORDER BY g.total_gasto DESC
            """),

        new Consulta("12. Pedidos acima da media da forma de pagamento (subconsultas)",
            "SUBCONSULTAS: correlacionada (media da mesma forma de pagamento no mesmo mes - usa o indice idx_pedido_pagamento_data) + IN aninhado (pedidos com produto que teve perda).",
            """
            SELECT pe.nr_venda_mes,
                   pe.dt_venda,
                   pe.forma_pagamento,
                   pe.vl_total,
                   ROUND((SELECT AVG(p2.vl_total)
                            FROM PEDIDO p2
                           WHERE p2.forma_pagamento = pe.forma_pagamento
                             AND p2.dt_venda >= DATE_FORMAT(pe.dt_venda, '%Y-%m-01')
                             AND p2.dt_venda <  DATE_FORMAT(pe.dt_venda, '%Y-%m-01') + INTERVAL 1 MONTH), 2)
                                                                     AS media_forma_no_mes
            FROM PEDIDO pe
            WHERE pe.vl_total > (SELECT AVG(p2.vl_total)
                                   FROM PEDIDO p2
                                  WHERE p2.forma_pagamento = pe.forma_pagamento
                                    AND p2.dt_venda >= DATE_FORMAT(pe.dt_venda, '%Y-%m-01')
                                    AND p2.dt_venda <  DATE_FORMAT(pe.dt_venda, '%Y-%m-01') + INTERVAL 1 MONTH)
              AND pe.nr_venda_mes IN (SELECT ps.nr_venda_mes
                                        FROM POSSUI ps
                                       WHERE ps.cd_produto IN (SELECT DISTINCT pr.cd_produto
                                                                 FROM PERDA pr))
            ORDER BY pe.dt_venda
            """),

        new Consulta("13. Visao: vendas detalhadas",
            "Conteudo da visao vw_vendas_detalhadas (PEDIDO + CLIENTE + POSSUI + PRODUTO + DEVOLUCAO): uma linha por item vendido. Requer a Parte 5 do consultas.sql executada no banco.",
            """
            SELECT *
            FROM vw_vendas_detalhadas
            ORDER BY dt_venda, nr_venda_mes
            """),

        new Consulta("14. Visao: situacao dos lotes",
            "Conteudo da visao vw_situacao_lotes (ESTOQUE + PRODUTO + PERDA + FORNECE + FORNECEDOR): validade, valor em estoque, perdas e fornecedores de cada lote. Requer a Parte 5 do consultas.sql executada no banco.",
            """
            SELECT *
            FROM vw_situacao_lotes
            ORDER BY dt_validade
            """)
    );
}
