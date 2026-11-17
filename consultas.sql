-- =========================================================
-- Sistema Administrativo para Empresa de Varejo do Ramo Hortifruti
-- Etapa 03 - SQL utilizado pela interface (Java + JDBC)
-- Banco: MySQL 8+ | Schema: hortifruti
--
-- CONTEUDO
--   PARTE 1 - Consultas da aba "Consultas" (1 a 8)
--   PARTE 2 - Consultas do Dashboard (indicadores e graficos)
--   PARTE 3 - Grafico de dispersao + correlacao de Pearson
--   PARTE 4 - Insercao / alteracao / exclusao (CLIENTE, PRODUTO,
--             FUNCIONARIO)
--   PARTE 5 - Entrega 4: indices, visoes e novas consultas (9 a 12)
--   PARTE 6 - Etapa 05: funcoes, procedimentos e triggers
--   PARTE 7 - Entrega final: SQL usado pela interface (CRUD de ESTOQUE,
--             rotinas da Etapa 05, dashboard com filtros e estatistica)
--
-- Obs.: na aplicacao, os valores variaveis sao "?" preenchidos via
-- PreparedStatement. Neste arquivo eles aparecem como valores de
-- exemplo (ou SET @variavel) para que tudo possa ser executado
-- diretamente no DBeaver / MySQL.
-- =========================================================
USE hortifruti;


-- #########################################################
-- PARTE 1 - CONSULTAS DA ABA "CONSULTAS"
-- #########################################################

-- =========================================================
-- CONSULTA 1 - Faturamento por categoria de produto
-- Recursos: JOIN entre 4 tabelas (N:N CATEGORIA-TEM-PRODUTO e
-- PRODUTO-POSSUI), agregacoes (COUNT DISTINCT, SUM), expressao
-- calculada e participacao percentual via funcao de janela.
-- =========================================================
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
ORDER BY faturamento DESC;


-- =========================================================
-- CONSULTA 2 - Ranking de clientes por valor gasto
-- Recursos: JOIN + LEFT JOIN (clientes com/sem devolucao), GROUP BY,
-- HAVING parametrizado, CASE (PF/PJ), funcao de janela RANK().
-- Parametro: quantidade minima de pedidos do cliente.
-- =========================================================
SET @min_pedidos = 1;
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
HAVING COUNT(pe.nr_venda_mes) >= @min_pedidos      -- na aplicacao: >= ?
ORDER BY total_gasto DESC;


-- =========================================================
-- CONSULTA 3 - Hierarquia completa de funcionarios
-- Recursos: CTE recursiva (WITH RECURSIVE) sobre o autorrelacionamento
-- supervisiona, auto-JOIN para obter o nome do supervisor e
-- subconsulta correlacionada para contar subordinados diretos.
-- =========================================================
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
ORDER BY h.cadeia;


-- =========================================================
-- CONSULTA 4 - Indice de perdas e prejuizo por lote
-- Recursos: JOIN com chave estrangeira composta (PERDA -> ESTOQUE),
-- JOIN com PRODUTO, agregacoes, percentual calculado e contagem
-- condicional (SUM + CASE) de descartes pendentes.
-- =========================================================
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
ORDER BY prejuizo_estimado DESC;


-- =========================================================
-- CONSULTA 5 - Lotes vencidos ou a vencer nos proximos N dias,
-- com os fornecedores do produto
-- Recursos: JOIN + LEFT JOIN em cadeia (ESTOQUE-PRODUTO-FORNECE-
-- FORNECEDOR), GROUP_CONCAT, funcoes de data (DATEDIFF, DATE_ADD),
-- CASE e filtro parametrizado.
-- Parametro: janela de dias a partir de hoje.
-- =========================================================
SET @dias = 30;
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
  AND e.dt_validade <= DATE_ADD(CURDATE(), INTERVAL @dias DAY)   -- na aplicacao: INTERVAL ? DAY
GROUP BY p.cd_produto, p.nome, e.nr_lote, e.quantidade, e.dt_entrada, e.dt_validade, p.valor_kg_ou_unitario
ORDER BY e.dt_validade;


-- =========================================================
-- CONSULTA 6 - Conferencia e detalhamento dos pedidos
-- Para cada pedido: itens vendidos, valor registrado x valor
-- recalculado a partir dos itens (POSSUI x PRODUTO) e situacao da
-- devolucao, se houver.
-- Recursos: JOIN entre 4 tabelas + LEFT JOIN, GROUP_CONCAT ordenado,
-- expressoes calculadas sobre agregacoes, CASE aninhado e filtro
-- parametrizado por forma de pagamento (LIKE).
-- Parametro: forma de pagamento (ou % para todas).
-- =========================================================
SET @forma = '%';
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
WHERE pe.forma_pagamento LIKE @forma                             -- na aplicacao: LIKE ?
GROUP BY pe.nr_venda_mes, pe.dt_venda, cl.cpf_cnpj, pe.forma_pagamento, pe.vl_total,
         d.n_devolucao, d.devolucao_efetivada, d.forma_resolucao
ORDER BY pe.dt_venda, pe.nr_venda_mes;


-- =========================================================
-- CONSULTA 7 - Vendas mensais por forma de pagamento (tabela pivot)
-- Recursos: agregacao condicional (SUM + CASE) para pivotar as
-- formas de pagamento em colunas, funcoes de data e WITH ROLLUP
-- para a linha de total geral.
-- =========================================================
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
ORDER BY (v.mes IS NULL), v.mes;


-- =========================================================
-- CONSULTA 8 - Produtos que nunca foram vendidos, com estoque e
-- fornecedores (candidatos a promocao)
-- Recursos: NOT EXISTS (anti-join), subconsultas escalares
-- correlacionadas e LEFT JOIN.
-- =========================================================
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
ORDER BY qtd_em_estoque DESC;


-- #########################################################
-- PARTE 2 - CONSULTAS DO DASHBOARD (indicadores e graficos)
-- Usadas em dao/DashboardDAO.java
-- #########################################################

-- Indicadores (cards)
SELECT (SELECT COUNT(*) FROM CLIENTE)                                AS total_clientes,
       (SELECT COUNT(*) FROM PEDIDO)                                 AS total_pedidos,
       (SELECT COALESCE(SUM(vl_total), 0) FROM PEDIDO)               AS faturamento,
       (SELECT COALESCE(AVG(vl_total), 0) FROM PEDIDO)               AS ticket_medio,
       (SELECT COUNT(*) FROM PRODUTO)                                AS total_produtos,
       (SELECT COALESCE(SUM(pr.qt_perdida * p.valor_kg_ou_unitario), 0)
          FROM PERDA pr JOIN PRODUTO p ON p.cd_produto = pr.cd_produto) AS prejuizo_perdas,
       (SELECT COUNT(*) FROM DEVOLUCAO)                              AS total_devolucoes;

-- Grafico: faturamento por forma de pagamento
SELECT forma_pagamento, SUM(vl_total) AS total
FROM PEDIDO
GROUP BY forma_pagamento
ORDER BY total DESC;

-- Grafico: faturamento mensal
SELECT DATE_FORMAT(dt_venda, '%Y-%m') AS mes, SUM(vl_total) AS total
FROM PEDIDO
GROUP BY DATE_FORMAT(dt_venda, '%Y-%m')
ORDER BY mes;

-- Grafico: top 10 produtos por quantidade vendida
SELECT p.nome, SUM(ps.quantidade) AS qtd
FROM POSSUI ps
JOIN PRODUTO p ON p.cd_produto = ps.cd_produto
GROUP BY p.cd_produto, p.nome
ORDER BY qtd DESC
LIMIT 10;

-- Grafico: faturamento por categoria (8 maiores)
SELECT c.nm_categoria, SUM(ps.quantidade * p.valor_kg_ou_unitario) AS faturamento
FROM CATEGORIA c
JOIN TEM     t  ON t.cd_categoria = c.cd_categoria
JOIN PRODUTO p  ON p.cd_produto   = t.cd_produto
JOIN POSSUI  ps ON ps.cd_produto  = p.cd_produto
GROUP BY c.cd_categoria, c.nm_categoria
ORDER BY faturamento DESC
LIMIT 8;

-- Grafico: quantidade perdida por motivo
SELECT motivo_perda, SUM(qt_perdida) AS qtd
FROM PERDA
GROUP BY motivo_perda
ORDER BY qtd DESC;

-- Grafico: devolucoes por forma de resolucao
SELECT COALESCE(forma_resolucao, 'Nao informada') AS resolucao, COUNT(*) AS qtd
FROM DEVOLUCAO
GROUP BY forma_resolucao
ORDER BY qtd DESC;


-- #########################################################
-- PARTE 3 - GRAFICO DE DISPERSAO (aba "Correlacao")
-- Usadas em dao/CorrelacaoDAO.java. Cada consulta gera os pares
-- (X, Y); a aplicacao (util/Estatistica.java) calcula o coeficiente
-- de Pearson (r), o R2 e a reta de regressao y = a + b*x.
-- #########################################################

-- Pedidos: quantidade de itens (X) x valor total (Y)
SELECT CONCAT('Pedido ', pe.nr_venda_mes) AS rotulo,
       SUM(ps.quantidade)                 AS x,
       pe.vl_total                        AS y
FROM PEDIDO pe
JOIN POSSUI ps ON ps.nr_venda_mes = pe.nr_venda_mes
GROUP BY pe.nr_venda_mes, pe.vl_total;

-- Clientes: numero de pedidos (X) x total gasto (Y)
SELECT CONCAT('Cliente ', cl.nr_cliente) AS rotulo,
       COUNT(pe.nr_venda_mes)             AS x,
       SUM(pe.vl_total)                   AS y
FROM CLIENTE cl
JOIN PEDIDO pe ON pe.nr_cliente = cl.nr_cliente
GROUP BY cl.nr_cliente;

-- Produtos: preco (X) x faturamento (Y)
SELECT p.nome                                      AS rotulo,
       p.valor_kg_ou_unitario                      AS x,
       SUM(ps.quantidade * p.valor_kg_ou_unitario) AS y
FROM PRODUTO p
JOIN POSSUI ps ON ps.cd_produto = p.cd_produto
GROUP BY p.cd_produto, p.nome, p.valor_kg_ou_unitario;

-- Produtos: preco (X) x quantidade vendida (Y)
SELECT p.nome                 AS rotulo,
       p.valor_kg_ou_unitario AS x,
       SUM(ps.quantidade)     AS y
FROM PRODUTO p
JOIN POSSUI ps ON ps.cd_produto = p.cd_produto
GROUP BY p.cd_produto, p.nome, p.valor_kg_ou_unitario;

-- Lotes: vida util em dias (X) x quantidade perdida (Y)
SELECT CONCAT(p.nome, ' - lote ', e.nr_lote)     AS rotulo,
       DATEDIFF(e.dt_validade, e.dt_entrada)     AS x,
       SUM(pr.qt_perdida)                        AS y
FROM ESTOQUE e
JOIN PRODUTO p ON p.cd_produto = e.cd_produto
JOIN PERDA pr  ON pr.cd_produto = e.cd_produto
              AND pr.nr_lote    = e.nr_lote
GROUP BY e.cd_produto, e.nr_lote, p.nome, e.dt_validade, e.dt_entrada;

-- Conferencia: coeficiente de Pearson e reta de regressao calculados
-- direto no SQL (par quantidade x valor do pedido). Resultado: r = 0,6696
--   r = (n*Sxy - Sx*Sy) / sqrt((n*Sxx - Sx^2) * (n*Syy - Sy^2))
--   b = (n*Sxy - Sx*Sy) / (n*Sxx - Sx^2)        a = (Sy - b*Sx) / n
WITH pares AS (
    SELECT SUM(ps.quantidade) AS x, pe.vl_total AS y
    FROM PEDIDO pe
    JOIN POSSUI ps ON ps.nr_venda_mes = pe.nr_venda_mes
    GROUP BY pe.nr_venda_mes, pe.vl_total
),
somas AS (
    SELECT COUNT(*) AS n, SUM(x) AS sx, SUM(y) AS sy,
           SUM(x * y) AS sxy, SUM(x * x) AS sxx, SUM(y * y) AS syy
    FROM pares
)
SELECT n,
       ROUND((n * sxy - sx * sy)
             / SQRT((n * sxx - sx * sx) * (n * syy - sy * sy)), 4)          AS pearson_r,
       ROUND(POW((n * sxy - sx * sy)
             / SQRT((n * sxx - sx * sx) * (n * syy - sy * sy)), 2), 4)     AS r2,
       ROUND((n * sxy - sx * sy) / (n * sxx - sx * sx), 4)                  AS inclinacao_b,
       ROUND((sy - (n * sxy - sx * sy) / (n * sxx - sx * sx) * sx) / n, 4)  AS intercepto_a
FROM somas;


-- #########################################################
-- PARTE 4 - INSERCAO / ALTERACAO / EXCLUSAO
-- Tabelas manipuladas pela interface: CLIENTE, PRODUTO e FUNCIONARIO
-- (abas "Clientes", "Produtos" e "Funcionarios").
--
-- Para cada tabela ha:
--   (a) o comando EXATO usado na aplicacao (com "?"), a classe/metodo
--       Java que o executa e o botao da tela que o dispara;
--   (b) um exemplo executavel com valores reais, seguido de SELECT
--       para conferir o efeito.
--
-- Os exemplos rodam dentro de uma TRANSACAO encerrada com ROLLBACK,
-- ou seja, podem ser executados quantas vezes quiser sem alterar os
-- dados do banco. Para gravar de verdade, troque ROLLBACK por COMMIT.
-- #########################################################


-- =========================================================
-- 4.1 CLIENTE  (dao/ClienteDAO.java - aba "Clientes")
-- =========================================================

-- (a) Comandos usados na aplicacao ---------------------------
--
-- LISTAR  - ClienteDAO.listar()    - ao abrir a aba
--   SELECT nr_cliente, cpf_cnpj, telefone, telefone2, cep, rua, bairro, numero, apartamento
--   FROM CLIENTE ORDER BY nr_cliente;
--
-- INSERIR - ClienteDAO.inserir()   - botao "Salvar" com "Novo registro"
--   INSERT INTO CLIENTE (cpf_cnpj, telefone, telefone2, cep, rua, bairro, numero, apartamento)
--   VALUES (?, ?, ?, ?, ?, ?, ?, ?);
--   (nr_cliente e AUTO_INCREMENT, por isso nao e informado)
--
-- ALTERAR - ClienteDAO.atualizar() - botao "Salvar" com um registro selecionado
--   UPDATE CLIENTE SET cpf_cnpj=?, telefone=?, telefone2=?, cep=?, rua=?, bairro=?,
--          numero=?, apartamento=?
--   WHERE nr_cliente=?;
--
-- EXCLUIR - ClienteDAO.excluir()   - botao "Excluir" (pede confirmacao)
--   DELETE FROM CLIENTE WHERE nr_cliente=?;

-- (b) Exemplo executavel -------------------------------------
START TRANSACTION;

-- INSERT: novo cliente pessoa fisica (telefone2 e apartamento ficam NULL)
INSERT INTO CLIENTE (cpf_cnpj, telefone, telefone2, cep, rua, bairro, numero, apartamento)
VALUES ('999.888.777-66', '81999990000', NULL, '50050-000', 'Rua do Sol', 'Boa Vista', '120', NULL);

SET @novo_cliente = LAST_INSERT_ID();          -- codigo gerado pelo AUTO_INCREMENT
SELECT * FROM CLIENTE WHERE nr_cliente = @novo_cliente;

-- UPDATE: cliente mudou de endereco e informou um segundo telefone
UPDATE CLIENTE
SET telefone2   = '81988887777',
    rua         = 'Av. Rui Barbosa',
    bairro      = 'Gracas',
    numero      = '455',
    apartamento = '302'
WHERE nr_cliente = @novo_cliente;

SELECT * FROM CLIENTE WHERE nr_cliente = @novo_cliente;

-- DELETE: remove o cliente (possivel porque ele nao tem pedidos)
DELETE FROM CLIENTE WHERE nr_cliente = @novo_cliente;

SELECT COUNT(*) AS encontrado_apos_delete FROM CLIENTE WHERE nr_cliente = @novo_cliente;  -- 0

ROLLBACK;

-- Restricoes que a interface trata (descomente para ver o erro):
--
-- Erro 1451 - cliente com pedidos nao pode ser excluido (FK de PEDIDO):
--   DELETE FROM CLIENTE WHERE nr_cliente = 12;
--
-- Erro 1062 - CPF/CNPJ duplicado (UNIQUE uq_cliente_cpf_cnpj):
--   INSERT INTO CLIENTE (cpf_cnpj, cep, rua, bairro, numero)
--   VALUES ('043.321.819-60', '50000-000', 'Rua X', 'Centro', '1');
--
-- Erro 3819 - CPF/CNPJ curto demais (CHECK ck_cliente_cpf_cnpj_tam):
--   INSERT INTO CLIENTE (cpf_cnpj, cep, rua, bairro, numero)
--   VALUES ('123', '50000-000', 'Rua X', 'Centro', '1');


-- =========================================================
-- 4.2 PRODUTO  (dao/ProdutoDAO.java - aba "Produtos")
-- =========================================================

-- (a) Comandos usados na aplicacao ---------------------------
--
-- LISTAR  - ProdutoDAO.listar()
--   SELECT cd_produto, nome, descricao, valor_kg_ou_unitario, vendido_unitario, marca
--   FROM PRODUTO ORDER BY cd_produto;
--
-- INSERIR - ProdutoDAO.inserir()
--   INSERT INTO PRODUTO (nome, descricao, valor_kg_ou_unitario, vendido_unitario, marca)
--   VALUES (?, ?, ?, ?, ?);
--
-- ALTERAR - ProdutoDAO.atualizar()
--   UPDATE PRODUTO SET nome=?, descricao=?, valor_kg_ou_unitario=?, vendido_unitario=?, marca=?
--   WHERE cd_produto=?;
--
-- EXCLUIR - ProdutoDAO.excluir()
--   DELETE FROM PRODUTO WHERE cd_produto=?;

-- (b) Exemplo executavel -------------------------------------
START TRANSACTION;

-- INSERT: produto vendido por quilo (vendido_unitario = FALSE)
INSERT INTO PRODUTO (nome, descricao, valor_kg_ou_unitario, vendido_unitario, marca)
VALUES ('Caju', 'Caju selecionado, qualidade para revenda', 7.50, FALSE, 'Sitio Boa Terra');

SET @novo_produto = LAST_INSERT_ID();
SELECT * FROM PRODUTO WHERE cd_produto = @novo_produto;

-- UPDATE: reajuste de preco de 8% e mudanca para venda por unidade
UPDATE PRODUTO
SET valor_kg_ou_unitario = ROUND(valor_kg_ou_unitario * 1.08, 2),
    vendido_unitario     = TRUE
WHERE cd_produto = @novo_produto;

SELECT cd_produto, nome, valor_kg_ou_unitario, vendido_unitario
FROM PRODUTO WHERE cd_produto = @novo_produto;           -- 8.10, 1

-- DELETE: remove o produto (sem estoque, vendas, categorias ou fornecedores)
DELETE FROM PRODUTO WHERE cd_produto = @novo_produto;

SELECT COUNT(*) AS encontrado_apos_delete FROM PRODUTO WHERE cd_produto = @novo_produto;  -- 0

ROLLBACK;

-- Restricoes que a interface trata (descomente para ver o erro):
--
-- Erro 1451 - produto com estoque/vendas nao pode ser excluido
-- (FKs de ESTOQUE, POSSUI, TEM e FORNECE com ON DELETE RESTRICT):
--   DELETE FROM PRODUTO WHERE cd_produto = 1;
--
-- Erro 3819 - valor precisa ser positivo (CHECK ck_produto_valor_positivo):
--   UPDATE PRODUTO SET valor_kg_ou_unitario = 0 WHERE cd_produto = 1;


-- =========================================================
-- 4.3 FUNCIONARIO  (dao/FuncionarioDAO.java - aba "Funcionarios")
-- Tabela com auto-relacionamento: nr_funcionario_supervisor aponta
-- para outro FUNCIONARIO (FK com ON DELETE SET NULL).
-- =========================================================

-- (a) Comandos usados na aplicacao ---------------------------
--
-- LISTAR  - FuncionarioDAO.listar()  (auto-JOIN para mostrar o nome do supervisor)
--   SELECT f.nr_funcionario, f.nome, f.cpf, f.telefone, f.cep, f.rua, f.bairro, f.numero,
--          f.apartamento, f.nr_funcionario_supervisor, s.nome AS nome_supervisor
--   FROM FUNCIONARIO f
--   LEFT JOIN FUNCIONARIO s ON s.nr_funcionario = f.nr_funcionario_supervisor
--   ORDER BY f.nr_funcionario;
--
-- INSERIR - FuncionarioDAO.inserir()
--   INSERT INTO FUNCIONARIO (nome, cpf, telefone, cep, rua, bairro, numero, apartamento,
--                            nr_funcionario_supervisor)
--   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
--   (quando "(sem supervisor)" e escolhido, o ultimo ? recebe NULL via setNull)
--
-- ALTERAR - FuncionarioDAO.atualizar()
--   UPDATE FUNCIONARIO SET nome=?, cpf=?, telefone=?, cep=?, rua=?, bairro=?, numero=?,
--          apartamento=?, nr_funcionario_supervisor=?
--   WHERE nr_funcionario=?;
--
-- EXCLUIR - FuncionarioDAO.excluir()
--   DELETE FROM FUNCIONARIO WHERE nr_funcionario=?;

-- (b) Exemplo executavel -------------------------------------
START TRANSACTION;

-- INSERT: novo supervisor (sem supervisor acima dele)
INSERT INTO FUNCIONARIO (nome, cpf, telefone, cep, rua, bairro, numero, apartamento, nr_funcionario_supervisor)
VALUES ('Marcos Teixeira', '99988877766', '81977776666', '50070-000', 'Rua da Aurora', 'Boa Vista', '80', NULL, NULL);
SET @supervisor = LAST_INSERT_ID();

-- INSERT: novo funcionario subordinado ao supervisor acima
INSERT INTO FUNCIONARIO (nome, cpf, telefone, cep, rua, bairro, numero, apartamento, nr_funcionario_supervisor)
VALUES ('Paula Ramos', '99988877755', '81966665555', '52060-000', 'Rua do Futuro', 'Aflitos', '15', '101', @supervisor);
SET @subordinado = LAST_INSERT_ID();

SELECT f.nr_funcionario, f.nome, s.nome AS supervisor
FROM FUNCIONARIO f
LEFT JOIN FUNCIONARIO s ON s.nr_funcionario = f.nr_funcionario_supervisor
WHERE f.nr_funcionario IN (@supervisor, @subordinado);

-- UPDATE: troca de telefone do subordinado
UPDATE FUNCIONARIO
SET telefone = '81955554444'
WHERE nr_funcionario = @subordinado;

-- DELETE do supervisor: pela FK com ON DELETE SET NULL, o subordinado
-- NAO e apagado, apenas fica sem supervisor
DELETE FROM FUNCIONARIO WHERE nr_funcionario = @supervisor;

SELECT nr_funcionario, nome, telefone, nr_funcionario_supervisor  -- supervisor agora e NULL
FROM FUNCIONARIO WHERE nr_funcionario = @subordinado;

-- DELETE do subordinado
DELETE FROM FUNCIONARIO WHERE nr_funcionario = @subordinado;

ROLLBACK;

-- Restricoes que a interface trata (descomente para ver o erro):
--
-- Erro 1062 - CPF duplicado (UNIQUE uq_funcionario_cpf):
--   UPDATE FUNCIONARIO SET cpf = '87347143455' WHERE nr_funcionario = 2;
--
-- Erro 3819 - CPF deve ter 11 caracteres (CHECK ck_funcionario_cpf_tam):
--   UPDATE FUNCIONARIO SET cpf = '123' WHERE nr_funcionario = 2;
--
-- Erro 1452 - supervisor inexistente (FK do auto-relacionamento):
--   UPDATE FUNCIONARIO SET nr_funcionario_supervisor = 9999 WHERE nr_funcionario = 2;


-- #########################################################
-- PARTE 5 - ENTREGA 4: INDICES, NOVAS CONSULTAS E VISOES
--
--   5.1 Indices  (2)  - idx_pedido_pagamento_data, idx_estoque_validade
--   5.2 Visoes   (2)  - vw_vendas_detalhadas, vw_situacao_lotes
--   5.3 Consultas (4) - anti join, full outer join, 2 com subconsultas
--
-- ATENCAO: os CREATE INDEX desta parte devem ser executados UMA vez.
-- Para reexecutar a parte inteira, rode antes os comandos de remocao
-- que estao no final (secao 5.4).
-- #########################################################


-- =========================================================
-- 5.1 INDICES
--
-- O MySQL (InnoDB) ja cria automaticamente um indice para cada chave
-- primaria e para cada chave estrangeira (ex.: PEDIDO.nr_cliente,
-- PERDA(cd_produto, nr_lote)). Por isso os indices abaixo ficam em
-- colunas que NAO sao chave, mas que as consultas filtram com
-- frequencia - sem eles, essas buscas leem a tabela inteira.
-- =========================================================

-- INDICE 1 - PEDIDO (forma_pagamento, dt_venda)
-- Necessidade: relatorios de venda filtram por forma de pagamento e
-- por periodo (ex.: "vendas no Pix em agosto"). A coluna de igualdade
-- (forma_pagamento) vem primeiro e a de intervalo (dt_venda) depois,
-- que e a ordem que permite ao MySQL usar as duas partes do indice.
-- Usado por: Consulta 6 (filtro LIKE pela forma de pagamento) e
--            Consulta 12 (subconsulta correlacionada por forma de
--            pagamento + mes - secao 5.3).
CREATE INDEX idx_pedido_pagamento_data ON PEDIDO (forma_pagamento, dt_venda);

-- INDICE 2 - ESTOQUE (dt_validade)
-- Necessidade: o controle de validade e diario no hortifruti
-- (produtos pereciveis). Buscar "lotes que vencem nos proximos N dias"
-- e um filtro de intervalo sobre dt_validade, que nao e chave.
-- Usado por: Consulta 5 (lotes vencidos/a vencer) e pela visao
--            vw_situacao_lotes quando filtrada por validade.
CREATE INDEX idx_estoque_validade ON ESTOQUE (dt_validade);

-- Conferencia dos indices criados
SHOW INDEX FROM PEDIDO  WHERE Key_name = 'idx_pedido_pagamento_data';
SHOW INDEX FROM ESTOQUE WHERE Key_name = 'idx_estoque_validade';

-- Prova de uso: o EXPLAIN mostra o indice na coluna "key".
-- (Com poucas linhas o otimizador as vezes prefere ler a tabela toda,
--  pois e mais barato; com o volume real de uma loja o indice e usado.)
EXPLAIN
SELECT nr_venda_mes, dt_venda, vl_total
FROM PEDIDO
WHERE forma_pagamento = 'Pix'
  AND dt_venda BETWEEN '2026-08-01' AND '2026-08-31';

EXPLAIN
SELECT cd_produto, nr_lote, dt_validade
FROM ESTOQUE
WHERE dt_validade BETWEEN '2026-09-01' AND '2026-10-31';


-- =========================================================
-- 5.2 VISOES
-- =========================================================

-- ---------------------------------------------------------
-- VISAO 1 - vw_vendas_detalhadas  (4 joins)
--   PEDIDO -> CLIENTE -> POSSUI -> PRODUTO, + LEFT JOIN DEVOLUCAO
--
-- Justificativa semantica: no varejo, a unidade de analise das vendas
-- e o ITEM vendido (qual produto, quanto, para quem, por quanto e
-- como foi pago). No banco essa informacao esta espalhada em 5
-- tabelas, e qualquer relatorio de vendas (por produto, por cliente,
-- por bairro, por mes, por forma de pagamento) precisaria repetir os
-- mesmos joins. A visao entrega uma linha por item vendido, ja com o
-- subtotal calculado, o tipo de cliente (PF/PJ) e a situacao da
-- devolucao do pedido - pronta para o gerente consultar.
-- ---------------------------------------------------------
CREATE OR REPLACE VIEW vw_vendas_detalhadas AS
SELECT pe.nr_venda_mes,
       pe.dt_venda,
       DATE_FORMAT(pe.dt_venda, '%Y-%m')                      AS mes,
       pe.forma_pagamento,
       cl.nr_cliente,
       cl.cpf_cnpj,
       CASE WHEN CHAR_LENGTH(cl.cpf_cnpj) > 14
            THEN 'Pessoa Juridica' ELSE 'Pessoa Fisica' END   AS tipo_cliente,
       cl.bairro,
       p.cd_produto,
       p.nome                                                 AS produto,
       CASE WHEN p.vendido_unitario THEN 'Unidade' ELSE 'Kg' END AS vendido_por,
       ps.quantidade,
       p.valor_kg_ou_unitario                                 AS preco_unitario,
       ROUND(ps.quantidade * p.valor_kg_ou_unitario, 2)       AS subtotal,
       CASE WHEN d.n_devolucao IS NULL THEN 'Sem devolucao'
            WHEN d.devolucao_efetivada  THEN 'Devolvido'
            ELSE 'Devolucao pendente' END                     AS situacao_devolucao
FROM PEDIDO pe
JOIN CLIENTE cl        ON cl.nr_cliente   = pe.nr_cliente
JOIN POSSUI  ps        ON ps.nr_venda_mes = pe.nr_venda_mes
JOIN PRODUTO p         ON p.cd_produto    = ps.cd_produto
LEFT JOIN DEVOLUCAO d  ON d.nr_venda_mes  = pe.nr_venda_mes;

-- Exemplos de uso da visao 1
-- a) Vendas por bairro e tipo de cliente
SELECT bairro, tipo_cliente,
       COUNT(DISTINCT nr_venda_mes) AS pedidos,
       SUM(subtotal)                AS faturamento
FROM vw_vendas_detalhadas
GROUP BY bairro, tipo_cliente
ORDER BY faturamento DESC;

-- b) Itens vendidos no Pix em agosto (o filtro chega ao indice 1)
SELECT nr_venda_mes, dt_venda, produto, quantidade, subtotal
FROM vw_vendas_detalhadas
WHERE forma_pagamento = 'Pix'
  AND dt_venda BETWEEN '2026-08-01' AND '2026-08-31'
ORDER BY dt_venda;


-- ---------------------------------------------------------
-- VISAO 2 - vw_situacao_lotes  (4 joins)
--   ESTOQUE -> PRODUTO, + LEFT JOIN perdas do lote,
--   + LEFT JOIN FORNECE -> LEFT JOIN FORNECEDOR
--
-- Justificativa semantica: em um hortifruti os produtos sao
-- pereciveis e o estoque e controlado por LOTE. Para decidir o que
-- vender primeiro, o que colocar em promocao e qual fornecedor
-- acionar quando um lote vence ou estraga, o responsavel pelo estoque
-- precisa ver, numa unica linha por lote: o produto, a validade, a
-- situacao (vencido / critico / atencao / ok), o valor parado em
-- estoque, quanto ja se perdeu daquele lote, os descartes pendentes e
-- os fornecedores do produto (rastreabilidade). Hoje isso exige
-- juntar 5 tabelas.
-- ---------------------------------------------------------
CREATE OR REPLACE VIEW vw_situacao_lotes AS
SELECT p.cd_produto,
       p.nome                                                     AS produto,
       e.nr_lote,
       e.dt_entrada,
       e.dt_validade,
       DATEDIFF(e.dt_validade, CURDATE())                         AS dias_para_vencer,
       CASE WHEN e.dt_validade <  CURDATE()                         THEN 'VENCIDO'
            WHEN e.dt_validade <= CURDATE() + INTERVAL 7 DAY        THEN 'CRITICO'
            WHEN e.dt_validade <= CURDATE() + INTERVAL 30 DAY       THEN 'ATENCAO'
            ELSE 'OK' END                                         AS situacao,
       e.quantidade                                               AS qtd_estoque,
       ROUND(e.quantidade * p.valor_kg_ou_unitario, 2)            AS valor_estoque,
       COALESCE(pr.qtd_perdida, 0)                                AS qtd_perdida,
       COALESCE(pr.descartes_pendentes, 0)                        AS descartes_pendentes,
       ROUND(100 * COALESCE(pr.qtd_perdida, 0)
             / (e.quantidade + COALESCE(pr.qtd_perdida, 0)), 2)   AS perc_perda,
       COUNT(DISTINCT fo.cd_fornecedor)                           AS qtd_fornecedores,
       COALESCE(GROUP_CONCAT(DISTINCT f.nome ORDER BY f.nome SEPARATOR ', '),
                '(sem fornecedor)')                               AS fornecedores
FROM ESTOQUE e
JOIN PRODUTO p ON p.cd_produto = e.cd_produto
LEFT JOIN (SELECT cd_produto, nr_lote,
                  SUM(qt_perdida)                                       AS qtd_perdida,
                  SUM(CASE WHEN descarte_efetivado THEN 0 ELSE 1 END)   AS descartes_pendentes
           FROM PERDA
           GROUP BY cd_produto, nr_lote) pr
       ON pr.cd_produto = e.cd_produto
      AND pr.nr_lote    = e.nr_lote
LEFT JOIN FORNECE fo    ON fo.cd_produto    = p.cd_produto
LEFT JOIN FORNECEDOR f  ON f.cd_fornecedor  = fo.cd_fornecedor
GROUP BY p.cd_produto, p.nome, p.valor_kg_ou_unitario, e.nr_lote, e.dt_entrada,
         e.dt_validade, e.quantidade, pr.qtd_perdida, pr.descartes_pendentes;

-- Exemplos de uso da visao 2
-- a) Resumo do estoque por situacao de validade
SELECT situacao,
       COUNT(*)            AS lotes,
       SUM(valor_estoque)  AS valor_em_estoque,
       SUM(qtd_perdida)    AS qtd_ja_perdida
FROM vw_situacao_lotes
GROUP BY situacao
ORDER BY FIELD(situacao, 'VENCIDO', 'CRITICO', 'ATENCAO', 'OK');

-- b) Lotes que exigem acao (vencidos ou vencendo em ate 30 dias),
--    com o valor parado e os fornecedores a acionar.
--    O filtro por dt_validade e atendido pelo INDICE 2.
SELECT produto, nr_lote, dt_validade, dias_para_vencer, situacao,
       qtd_estoque, valor_estoque, descartes_pendentes, fornecedores
FROM vw_situacao_lotes
WHERE dt_validade <= CURDATE() + INTERVAL 30 DAY
ORDER BY dt_validade;


-- =========================================================
-- 5.3 NOVAS CONSULTAS
-- =========================================================

-- ---------------------------------------------------------
-- CONSULTA 9 - ANTI JOIN (LEFT JOIN ... WHERE ... IS NULL)
-- Clientes cadastrados que nunca fizeram nenhum pedido.
--
-- Anti join: devolve as linhas da tabela da ESQUERDA que NAO tem par
-- na tabela da direita. O LEFT JOIN mantem todos os clientes; os que
-- nao tem pedido ficam com as colunas de PEDIDO nulas, e o
-- WHERE pe.nr_venda_mes IS NULL fica so com eles.
-- Uso no negocio: lista para campanha de primeira compra.
-- ---------------------------------------------------------
SELECT cl.nr_cliente,
       cl.cpf_cnpj,
       CASE WHEN CHAR_LENGTH(cl.cpf_cnpj) > 14
            THEN 'Pessoa Juridica' ELSE 'Pessoa Fisica' END AS tipo_cliente,
       COALESCE(cl.telefone, cl.telefone2, '(sem telefone)') AS contato,
       cl.bairro
FROM CLIENTE cl
LEFT JOIN PEDIDO pe ON pe.nr_cliente = cl.nr_cliente
WHERE pe.nr_venda_mes IS NULL
ORDER BY cl.bairro, cl.nr_cliente;


-- ---------------------------------------------------------
-- CONSULTA 10 - FULL OUTER JOIN
-- Comparativo por produto: o que foi VENDIDO x o que foi PERDIDO.
--
-- Um produto pode ter vendas e perdas, so vendas ou so perdas. Para
-- ver os tres casos juntos e preciso um FULL OUTER JOIN entre o total
-- vendido por produto e o total perdido por produto.
-- O MySQL nao possui FULL OUTER JOIN; a forma padrao de obter o mesmo
-- resultado e:
--     A LEFT JOIN B                       (todos de A, com ou sem B)
--     UNION ALL
--     A RIGHT JOIN B WHERE A.chave IS NULL (os de B que nao estao em A)
-- Em SGBDs que suportam a sintaxe (PostgreSQL, SQL Server, Oracle),
-- seria simplesmente: FROM vendas v FULL OUTER JOIN perdas pr
--                     ON pr.cd_produto = v.cd_produto
-- Uso no negocio: produtos que perdem mais do que vendem indicam
-- compra excessiva ou armazenamento inadequado.
-- ---------------------------------------------------------
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
ORDER BY situacao, valor_perdido DESC;


-- ---------------------------------------------------------
-- CONSULTA 11 - SUBCONSULTAS (tabela derivada + subconsulta escalar)
-- Clientes cujo total gasto e maior que a media de gasto por cliente.
--
-- Subconsulta 1 (no FROM / tabela derivada): calcula o gasto de cada
--   cliente.
-- Subconsulta 2 (escalar, no WHERE): calcula a MEDIA desses gastos -
--   ela mesma usa outra subconsulta derivada (media de uma soma).
-- Uso no negocio: identificar os melhores clientes para fidelizacao.
-- ---------------------------------------------------------
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
ORDER BY g.total_gasto DESC;


-- ---------------------------------------------------------
-- CONSULTA 12 - SUBCONSULTAS (correlacionada + IN)
-- Pedidos com valor acima da media da MESMA forma de pagamento no
-- MESMO mes, que contenham ao menos um produto com perda registrada.
--
-- Subconsulta 1 (correlacionada, no WHERE): para cada pedido "pe",
--   recalcula a media dos pedidos com a mesma forma de pagamento e no
--   mesmo mes. Ela filtra por forma_pagamento (igualdade) + intervalo
--   de dt_venda, exatamente o formato atendido pelo
--   INDICE 1 idx_pedido_pagamento_data.
-- Subconsulta 2 (IN aninhado): pedidos que tem algum item cujo
--   produto aparece na tabela PERDA.
-- Uso no negocio: vendas acima do padrao envolvendo produtos sensiveis
-- a perda - candidatas a conferencia de qualidade.
-- ---------------------------------------------------------
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
ORDER BY pe.dt_venda;


-- =========================================================
-- 5.4 REMOCAO (somente para reexecutar a Parte 5 do zero)
-- =========================================================
-- DROP VIEW IF EXISTS vw_vendas_detalhadas;
-- DROP VIEW IF EXISTS vw_situacao_lotes;
-- DROP INDEX idx_pedido_pagamento_data ON PEDIDO;
-- DROP INDEX idx_estoque_validade      ON ESTOQUE;


-- #########################################################
-- PARTE 6 - ETAPA 05: FUNCOES, PROCEDIMENTOS E TRIGGERS
--
--   6.1 Tabela de log   - LOG_PRECO_PRODUTO
--   6.2 Funcoes      (2) - fn_situacao_validade, fn_estoque_disponivel
--   6.3 Procedimentos(2) - sp_reajustar_precos_categoria (atualizacao)
--                          sp_baixar_estoque_fefo        (CURSOR)
--   6.4 Triggers     (2) - trg_produto_log_preco   (grava log)
--                          trg_perda_baixa_estoque (estoque x perda)
--   6.5 Testes (transacao com ROLLBACK - nao altera os dados)
--   6.6 Remocao
--
-- Como as pecas se ligam:
--   sp_reajustar_precos_categoria --altera precos--> trg_produto_log_preco
--                                                     --grava--> LOG_PRECO_PRODUTO
--   sp_baixar_estoque_fefo --usa--> fn_estoque_disponivel
--   INSERT em PERDA --dispara--> trg_perda_baixa_estoque --baixa--> ESTOQUE
--
-- EXECUCAO: esta parte usa DELIMITER $$ (necessario para criar blocos
-- BEGIN ... END). No DBeaver execute como script (Alt+X). Cada objeto
-- e precedido de DROP ... IF EXISTS, entao a parte pode ser
-- reexecutada sem erro.
-- #########################################################


-- =========================================================
-- 6.1 TABELA DE LOG
--
-- LOG_PRECO_PRODUTO guarda o historico de alteracoes de preco,
-- preenchido automaticamente pelo trigger trg_produto_log_preco.
--
-- Decisoes de modelagem:
--  * NAO tem chave estrangeira para PRODUTO: um log de auditoria
--    precisa sobreviver mesmo que o produto seja excluido depois.
--    Por isso tambem guarda uma copia do nome do produto no momento
--    da alteracao (nome_produto).
--  * Guarda preco antigo, preco novo e a variacao %, para que o
--    historico possa ser lido sem recalculos.
--  * Guarda o usuario do banco e a data/hora: responde "quem mudou o
--    preco e quando".
--  * Indice (cd_produto, dt_alteracao): a consulta tipica do log e
--    "historico de precos do produto X em ordem cronologica".
-- =========================================================
CREATE TABLE IF NOT EXISTS LOG_PRECO_PRODUTO (
    id_log         INT AUTO_INCREMENT PRIMARY KEY,
    cd_produto     INT           NOT NULL,
    nome_produto   VARCHAR(100)  NOT NULL,
    valor_antigo   DECIMAL(10,2) NOT NULL,
    valor_novo     DECIMAL(10,2) NOT NULL,
    variacao_pct   DECIMAL(7,2)  NOT NULL,
    usuario        VARCHAR(100)  NOT NULL,
    dt_alteracao   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_preco_produto_data (cd_produto, dt_alteracao)
);


-- =========================================================
-- 6.2 FUNCOES
-- =========================================================

-- ---------------------------------------------------------
-- FUNCAO 1 - fn_situacao_validade(p_dt_validade)
-- Retorna a situacao de um lote pela validade:
--   'VENCIDO'  - validade ja passou
--   'CRITICO'  - vence em ate 7 dias  (vender com prioridade)
--   'ATENCAO'  - vence em ate 30 dias (planejar promocao)
--   'OK'       - mais de 30 dias
--
-- Estrutura condicional: IF / ELSEIF / ELSE.
--
-- JUSTIFICATIVA SEMANTICA: num hortifruti quase tudo e perecivel, e a
-- classificacao do lote pela validade e o que decide o que vai para a
-- frente da gondola, o que entra em promocao e o que deve ser
-- descartado. Essa regra estava escrita duas vezes (Consulta 5 e visao
-- vw_situacao_lotes); se a loja decidir que "critico" passa a ser 5
-- dias, seria preciso lembrar de alterar todos os lugares. A funcao
-- centraliza a regra de negocio em um unico ponto.
--
-- Declarada NO SQL (nao le tabelas) e NOT DETERMINISTIC (depende da
-- data de hoje). Essas declaracoes sao exigidas pelo MySQL quando o
-- log binario esta ativo (padrao do MySQL 8).
-- ---------------------------------------------------------
DROP FUNCTION IF EXISTS fn_situacao_validade;

DELIMITER $$
CREATE FUNCTION fn_situacao_validade(p_dt_validade DATE)
RETURNS VARCHAR(10)
NOT DETERMINISTIC
NO SQL
BEGIN
    DECLARE v_dias INT;

    IF p_dt_validade IS NULL THEN
        RETURN NULL;
    END IF;

    SET v_dias = DATEDIFF(p_dt_validade, CURDATE());

    IF v_dias < 0 THEN
        RETURN 'VENCIDO';
    ELSEIF v_dias <= 7 THEN
        RETURN 'CRITICO';
    ELSEIF v_dias <= 30 THEN
        RETURN 'ATENCAO';
    ELSE
        RETURN 'OK';
    END IF;
END$$
DELIMITER ;


-- ---------------------------------------------------------
-- FUNCAO 2 - fn_estoque_disponivel(p_cd_produto)
-- Retorna a quantidade de um produto que pode ser vendida: a soma dos
-- lotes com validade de hoje em diante. Lotes vencidos nao contam.
--
-- JUSTIFICATIVA SEMANTICA: estoque fisico e estoque vendavel nao sao a
-- mesma coisa em um hortifruti. Um lote vencido continua no ESTOQUE
-- ate ser descartado, mas nao pode ser vendido. Antes de aceitar uma
-- venda, o sistema precisa saber quanto de fato esta disponivel. A
-- funcao e usada pelo procedimento sp_baixar_estoque_fefo para
-- recusar uma baixa maior que o disponivel e pode ser usada em
-- qualquer consulta (ex.: lista de produtos com estoque vendavel).
--
-- Declarada READS SQL DATA (le a tabela ESTOQUE, sem alterar).
-- ---------------------------------------------------------
DROP FUNCTION IF EXISTS fn_estoque_disponivel;

DELIMITER $$
CREATE FUNCTION fn_estoque_disponivel(p_cd_produto INT)
RETURNS DECIMAL(10,3)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(10,3);

    SELECT COALESCE(SUM(quantidade), 0)
      INTO v_total
      FROM ESTOQUE
     WHERE cd_produto  = p_cd_produto
       AND dt_validade >= CURDATE();

    RETURN v_total;
END$$
DELIMITER ;


-- =========================================================
-- 6.3 PROCEDIMENTOS
-- =========================================================

-- ---------------------------------------------------------
-- PROCEDIMENTO 1 - sp_reajustar_precos_categoria(categoria, percentual)
-- [procedimento de ATUALIZACAO DE DADOS]
--
-- Reajusta em p_percentual % o preco de todos os produtos de uma
-- categoria. Ex.: CALL sp_reajustar_precos_categoria(2, 10);
--   -> +10% em todos os produtos da categoria 2.
--
-- Validacoes (SIGNAL interrompe e devolve a mensagem):
--   * percentual entre -50 e +100 (evita erro de digitacao, como
--     digitar 100 querendo dizer 10, e preco zerado ou negativo);
--   * a categoria precisa existir.
--
-- JUSTIFICATIVA SEMANTICA: no hortifruti os precos acompanham a
-- sazonalidade e mudam por GRUPO de produtos - na entressafra das
-- frutas tropicais, todas sobem juntas; na safra, todas baixam.
-- Alterar produto por produto e lento e sujeito a erro. O
-- procedimento faz o reajuste do grupo inteiro em uma operacao,
-- com regras de seguranca, e cada preco alterado fica registrado
-- automaticamente no log pelo trigger trg_produto_log_preco.
-- ---------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_reajustar_precos_categoria;

DELIMITER $$
CREATE PROCEDURE sp_reajustar_precos_categoria(
    IN p_cd_categoria INT,
    IN p_percentual   DECIMAL(6,2)
)
BEGIN
    DECLARE v_categoria VARCHAR(60);
    DECLARE v_afetados  INT;
    DECLARE v_msg       VARCHAR(255);

    -- validacao 1: percentual dentro da faixa permitida
    IF p_percentual IS NULL OR p_percentual < -50 OR p_percentual > 100 THEN
        SET v_msg = CONCAT('Percentual invalido (', COALESCE(p_percentual, 'nulo'),
                           '%). Use um valor entre -50 e 100.');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    -- validacao 2: a categoria precisa existir
    SELECT nm_categoria INTO v_categoria
      FROM CATEGORIA
     WHERE cd_categoria = p_cd_categoria;

    IF v_categoria IS NULL THEN
        SET v_msg = CONCAT('Categoria ', COALESCE(p_cd_categoria, 'nula'), ' nao existe.');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    -- atualizacao: todos os produtos ligados a categoria (tabela TEM)
    UPDATE PRODUTO p
      JOIN TEM t ON t.cd_produto = p.cd_produto
       SET p.valor_kg_ou_unitario = ROUND(p.valor_kg_ou_unitario * (1 + p_percentual / 100), 2)
     WHERE t.cd_categoria = p_cd_categoria;

    SET v_afetados = ROW_COUNT();

    -- resumo para quem chamou
    SELECT v_categoria AS categoria,
           p_percentual AS percentual_aplicado,
           v_afetados   AS produtos_reajustados;
END$$
DELIMITER ;


-- ---------------------------------------------------------
-- PROCEDIMENTO 2 - sp_baixar_estoque_fefo(produto, quantidade)
-- [procedimento com CURSOR]
--
-- Da baixa de uma quantidade vendida de um produto seguindo a regra
-- FEFO (First Expired, First Out): consome primeiro o lote que vence
-- primeiro, passando para o proximo lote so quando o anterior acaba.
--
-- Passo a passo:
--   1. Valida a quantidade (> 0).
--   2. Usa fn_estoque_disponivel: se o disponivel (lotes validos) for
--      menor que o pedido, recusa e NADA e alterado.
--   3. O CURSOR percorre os lotes validos do produto em ordem de
--      validade. Para cada lote:
--         retirar  = MIN(quanto ainda falta, quanto tem no lote)
--         atualiza o lote (quantidade - retirar)
--         registra o que saiu daquele lote
--         falta    = falta - retirar
--      e para assim que "falta" chega a zero.
--   4. Devolve uma tabela com quanto saiu de cada lote.
--
-- POR QUE PRECISA DE CURSOR (nao e possivel com um UPDATE):
--   * A quantidade retirada de cada lote DEPENDE DO RESULTADO DO LOTE
--     ANTERIOR: o saldo que "falta" e um estado que passa de uma linha
--     para a proxima. Um UPDATE avalia a mesma expressao para cada
--     linha de forma independente e nao carrega um saldo entre elas.
--   * A ORDEM importa (validade crescente) e o processamento PARA no
--     meio, quando o pedido e atendido; um UPDATE nao tem "parar
--     quando atingir o total".
--   * Para cada lote sao feitas DUAS acoes com valores calculados
--     naquele passo (atualizar o lote e registrar a retirada).
--
-- JUSTIFICATIVA SEMANTICA: produtos pereciveis devem sair na ordem de
-- validade. Se a baixa for feita no lote errado (o mais novo), o lote
-- mais antigo vence na prateleira e vira perda. A regra FEFO e a
-- pratica padrao do varejo de alimentos para reduzir desperdicio.
--
-- Transacao: o procedimento NAO abre nem fecha transacao - quem chama
-- decide (COMMIT/ROLLBACK). A validacao do passo 2 garante que o loop
-- sempre consegue atender o pedido inteiro, entao a baixa nunca fica
-- pela metade.
-- ---------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_baixar_estoque_fefo;

DELIMITER $$
CREATE PROCEDURE sp_baixar_estoque_fefo(
    IN p_cd_produto INT,
    IN p_quantidade DECIMAL(10,3)
)
BEGIN
    DECLARE v_falta       DECIMAL(10,3);
    DECLARE v_disponivel  DECIMAL(10,3);
    DECLARE v_retirar     DECIMAL(10,3);
    DECLARE v_lote        INT;
    DECLARE v_qtd_lote    DECIMAL(10,3);
    DECLARE v_validade    DATE;
    DECLARE v_fim         BOOLEAN DEFAULT FALSE;
    DECLARE v_msg         VARCHAR(255);

    -- cursor: lotes validos e com saldo, do que vence primeiro ao ultimo
    DECLARE cur_lotes CURSOR FOR
        SELECT nr_lote, quantidade, dt_validade
          FROM ESTOQUE
         WHERE cd_produto  = p_cd_produto
           AND quantidade  > 0
           AND dt_validade >= CURDATE()
         ORDER BY dt_validade, nr_lote;

    -- quando o FETCH nao encontra mais lotes
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fim = TRUE;

    -- 1. validacao da quantidade
    IF p_quantidade IS NULL OR p_quantidade <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A quantidade da baixa deve ser maior que zero.';
    END IF;

    -- 2. validacao do estoque vendavel (FUNCAO 2)
    SET v_disponivel = fn_estoque_disponivel(p_cd_produto);
    IF v_disponivel < p_quantidade THEN
        SET v_msg = CONCAT('Estoque insuficiente para o produto ', p_cd_produto,
                           ': disponivel ', v_disponivel, ', solicitado ', p_quantidade, '.');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    -- tabela temporaria com o detalhamento da baixa
    DROP TEMPORARY TABLE IF EXISTS tmp_baixa_fefo;
    CREATE TEMPORARY TABLE tmp_baixa_fefo (
        nr_lote       INT,
        dt_validade   DATE,
        qtd_antes     DECIMAL(10,3),
        qtd_retirada  DECIMAL(10,3),
        qtd_depois    DECIMAL(10,3)
    );

    -- 3. percorre os lotes com o cursor
    SET v_falta = p_quantidade;
    OPEN cur_lotes;

    percorre_lotes: LOOP
        FETCH cur_lotes INTO v_lote, v_qtd_lote, v_validade;
        IF v_fim THEN
            LEAVE percorre_lotes;
        END IF;

        -- quanto sai deste lote depende do que ainda falta
        SET v_retirar = LEAST(v_falta, v_qtd_lote);

        UPDATE ESTOQUE
           SET quantidade = quantidade - v_retirar
         WHERE cd_produto = p_cd_produto
           AND nr_lote    = v_lote;

        INSERT INTO tmp_baixa_fefo
        VALUES (v_lote, v_validade, v_qtd_lote, v_retirar, v_qtd_lote - v_retirar);

        SET v_falta = v_falta - v_retirar;

        -- pedido atendido: para antes de tocar nos proximos lotes
        IF v_falta <= 0 THEN
            LEAVE percorre_lotes;
        END IF;
    END LOOP percorre_lotes;

    CLOSE cur_lotes;

    -- 4. resultado: quanto saiu de cada lote
    SELECT p_cd_produto AS cd_produto, b.*
      FROM tmp_baixa_fefo b
     ORDER BY b.dt_validade, b.nr_lote;

    DROP TEMPORARY TABLE tmp_baixa_fefo;
END$$
DELIMITER ;


-- =========================================================
-- 6.4 TRIGGERS
-- =========================================================

-- ---------------------------------------------------------
-- TRIGGER 1 - trg_produto_log_preco   (AFTER UPDATE ON PRODUTO)
-- [trigger que ATUALIZA A TABELA DE LOG]
--
-- Sempre que o preco de um produto muda, grava uma linha em
-- LOG_PRECO_PRODUTO com preco antigo, preco novo, variacao %, usuario
-- e data/hora. Alteracoes que nao mexem no preco (nome, descricao,
-- marca) NAO geram log.
--
-- JUSTIFICATIVA SEMANTICA: o preco e o dado mais sensivel do varejo.
-- A loja precisa responder "quem mudou o preco do tomate, quando e de
-- quanto?" - para conferir reajustes, investigar erros de etiqueta e
-- atender reclamacoes de cliente (o preco cobrado deve ser o
-- anunciado). Por ser um trigger, o registro e garantido em QUALQUER
-- caminho de alteracao: pelo procedimento de reajuste, pela aba
-- Produtos da aplicacao Java ou por um UPDATE direto no DBeaver -
-- ninguem precisa "lembrar" de gravar o log.
--
-- Por que AFTER: o log so deve existir se a alteracao realmente
-- aconteceu (passou nas restricoes CHECK da tabela).
-- Por que USER() e nao CURRENT_USER(): dentro de um trigger,
-- CURRENT_USER() devolve o DONO do trigger; USER() devolve quem esta
-- conectado e fez a alteracao.
-- ---------------------------------------------------------
DROP TRIGGER IF EXISTS trg_produto_log_preco;

DELIMITER $$
CREATE TRIGGER trg_produto_log_preco
AFTER UPDATE ON PRODUTO
FOR EACH ROW
BEGIN
    IF NEW.valor_kg_ou_unitario <> OLD.valor_kg_ou_unitario THEN
        INSERT INTO LOG_PRECO_PRODUTO
            (cd_produto, nome_produto, valor_antigo, valor_novo, variacao_pct, usuario)
        VALUES
            (NEW.cd_produto,
             NEW.nome,
             OLD.valor_kg_ou_unitario,
             NEW.valor_kg_ou_unitario,
             ROUND(100 * (NEW.valor_kg_ou_unitario - OLD.valor_kg_ou_unitario)
                       / OLD.valor_kg_ou_unitario, 2),
             USER());
    END IF;
END$$
DELIMITER ;


-- ---------------------------------------------------------
-- TRIGGER 2 - trg_perda_baixa_estoque   (BEFORE INSERT ON PERDA)
--
-- Ao registrar uma perda (produto estragado, amassado, vencido...):
--   1. bloqueia o registro se o lote nao existir ou se a quantidade
--      perdida for MAIOR que a quantidade do lote;
--   2. desconta a quantidade perdida do lote no ESTOQUE.
--
-- JUSTIFICATIVA SEMANTICA: perda e saida de estoque. Sem o trigger, e
-- possivel registrar uma perda e o ESTOQUE continuar mostrando a
-- mercadoria que ja foi para o lixo - o sistema passa a ter "estoque
-- fantasma", que gera vendas que nao podem ser entregues e compras
-- erradas. Tambem nao faz sentido perder mais do que existia no lote.
-- O trigger mantem PERDA e ESTOQUE coerentes automaticamente.
--
-- Por que BEFORE: a verificacao precisa acontecer ANTES de a perda
-- ser gravada, para poder recusa-la (SIGNAL cancela o INSERT inteiro,
-- inclusive a baixa no estoque).
-- ---------------------------------------------------------
DROP TRIGGER IF EXISTS trg_perda_baixa_estoque;

DELIMITER $$
CREATE TRIGGER trg_perda_baixa_estoque
BEFORE INSERT ON PERDA
FOR EACH ROW
BEGIN
    DECLARE v_qtd_lote DECIMAL(10,3);
    DECLARE v_msg      VARCHAR(255);

    SELECT quantidade INTO v_qtd_lote
      FROM ESTOQUE
     WHERE cd_produto = NEW.cd_produto
       AND nr_lote    = NEW.nr_lote
       FOR UPDATE;

    IF v_qtd_lote IS NULL THEN
        SET v_msg = CONCAT('Lote ', NEW.nr_lote, ' do produto ', NEW.cd_produto, ' nao existe no estoque.');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NEW.qt_perdida > v_qtd_lote THEN
        SET v_msg = CONCAT('Perda de ', NEW.qt_perdida, ' maior que o estoque do lote (',
                           v_qtd_lote, ').');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    UPDATE ESTOQUE
       SET quantidade = quantidade - NEW.qt_perdida
     WHERE cd_produto = NEW.cd_produto
       AND nr_lote    = NEW.nr_lote;
END$$
DELIMITER ;


-- =========================================================
-- 6.5 TESTES
-- Tudo dentro de uma transacao encerrada com ROLLBACK: pode ser
-- executado quantas vezes quiser sem alterar os dados.
--
-- Observacao: nos dados de exemplo (inser_to.sql) todos os lotes ja
-- estao vencidos. Para testar a funcao 2 e o FEFO, o teste cria antes
-- tres lotes novos (com validade futura) para o produto 2 (Maca Gala).
-- =========================================================
START TRANSACTION;

-- ---- FUNCAO 1: um exemplo de cada situacao ----
SELECT fn_situacao_validade(CURDATE() - INTERVAL 1 DAY)  AS ontem,      -- VENCIDO
       fn_situacao_validade(CURDATE() + INTERVAL 3 DAY)  AS em_3_dias,  -- CRITICO
       fn_situacao_validade(CURDATE() + INTERVAL 20 DAY) AS em_20_dias, -- ATENCAO
       fn_situacao_validade(CURDATE() + INTERVAL 60 DAY) AS em_60_dias; -- OK

-- ---- Preparacao: 3 lotes novos da Maca Gala (produto 2) ----
INSERT INTO ESTOQUE (cd_produto, nr_lote, quantidade, dt_validade, dt_entrada) VALUES
(2, 2, 10.000, CURDATE() + INTERVAL 5  DAY, CURDATE()),   -- vence primeiro
(2, 3, 20.000, CURDATE() + INTERVAL 15 DAY, CURDATE()),
(2, 4, 30.000, CURDATE() + INTERVAL 40 DAY, CURDATE());   -- vence por ultimo

-- FUNCAO 1 aplicada aos lotes do produto 2
SELECT nr_lote, quantidade, dt_validade, fn_situacao_validade(dt_validade) AS situacao
  FROM ESTOQUE WHERE cd_produto = 2 ORDER BY dt_validade;
-- lote 1: VENCIDO | lote 2: CRITICO | lote 3: ATENCAO | lote 4: OK

-- ---- FUNCAO 2: estoque vendavel (o lote 1, vencido, nao entra) ----
SELECT fn_estoque_disponivel(2) AS disponivel_maca_gala;   -- 60.000

-- ---- PROCEDIMENTO 2 (CURSOR): baixa de 25 kg pela regra FEFO ----
CALL sp_baixar_estoque_fefo(2, 25);
-- esperado: lote 2 (vence primeiro) sai inteiro: 10 -> 0
--           lote 3 cobre o restante:            20 -> 5
--           lote 4 nao e tocado:                30 -> 30

SELECT nr_lote, quantidade, dt_validade
  FROM ESTOQUE WHERE cd_produto = 2 ORDER BY dt_validade;
SELECT fn_estoque_disponivel(2) AS disponivel_apos_baixa;  -- 35.000

-- ---- PROCEDIMENTO 1 + TRIGGER 1: reajuste de 10% na categoria 3 ----
-- (Frutas Vermelhas: Batata Inglesa, Beterraba e Vagem estao nela)
SELECT p.cd_produto, p.nome, p.valor_kg_ou_unitario AS preco_antes
  FROM PRODUTO p JOIN TEM t ON t.cd_produto = p.cd_produto
 WHERE t.cd_categoria = 3;

CALL sp_reajustar_precos_categoria(3, 10);

-- o trigger gravou uma linha de log para cada preco alterado
SELECT cd_produto, nome_produto, valor_antigo, valor_novo, variacao_pct, usuario, dt_alteracao
  FROM LOG_PRECO_PRODUTO
 ORDER BY id_log;

-- ---- TRIGGER 1: alteracao direta (como faz a aba Produtos do app) ----
UPDATE PRODUTO SET valor_kg_ou_unitario = 9.99 WHERE cd_produto = 1;           -- gera log
UPDATE PRODUTO SET descricao = 'Banana Prata da serra' WHERE cd_produto = 1;   -- NAO gera log
SELECT cd_produto, nome_produto, valor_antigo, valor_novo, variacao_pct
  FROM LOG_PRECO_PRODUTO WHERE cd_produto = 1;   -- apenas 1 linha (8.47 -> 9.99)

-- ---- TRIGGER 2: perda desconta o estoque do lote ----
SELECT quantidade AS lote3_antes FROM ESTOQUE WHERE cd_produto = 2 AND nr_lote = 3;   -- 5.000

INSERT INTO PERDA (cd_produto, nr_lote, dt_perda, qt_perdida, motivo_perda, descarte_efetivado)
VALUES (2, 3, CURDATE(), 2.000, 'Produto amassado', FALSE);

SELECT quantidade AS lote3_depois FROM ESTOQUE WHERE cd_produto = 2 AND nr_lote = 3;  -- 3.000

ROLLBACK;

-- ---- Casos de ERRO (descomente um por vez para ver a mensagem) ----
-- Procedimento 1 - percentual fora da faixa:
--   CALL sp_reajustar_precos_categoria(3, 150);
--   -> Percentual invalido (150.00%). Use um valor entre -50 e 100.
-- Procedimento 1 - categoria inexistente:
--   CALL sp_reajustar_precos_categoria(999, 10);
--   -> Categoria 999 nao existe.
-- Procedimento 2 - mais do que o estoque vendavel:
--   CALL sp_baixar_estoque_fefo(2, 1000);
--   -> Estoque insuficiente para o produto 2: disponivel 0.000, solicitado 1000.000.
-- Trigger 2 - perda maior que o lote:
--   INSERT INTO PERDA (cd_produto, nr_lote, dt_perda, qt_perdida, motivo_perda, descarte_efetivado)
--   VALUES (1, 1, CURDATE(), 9999, 'Teste', FALSE);
--   -> Perda de 9999.000 maior que o estoque do lote (205.360).


-- =========================================================
-- 6.6 REMOCAO (somente se quiser desfazer a Parte 6)
-- =========================================================
-- DROP TRIGGER   IF EXISTS trg_produto_log_preco;
-- DROP TRIGGER   IF EXISTS trg_perda_baixa_estoque;
-- DROP PROCEDURE IF EXISTS sp_reajustar_precos_categoria;
-- DROP PROCEDURE IF EXISTS sp_baixar_estoque_fefo;
-- DROP FUNCTION  IF EXISTS fn_situacao_validade;
-- DROP FUNCTION  IF EXISTS fn_estoque_disponivel;
-- DROP TABLE     IF EXISTS LOG_PRECO_PRODUTO;


-- #########################################################
-- PARTE 7 - ENTREGA FINAL: SQL USADO PELA INTERFACE
--
--   7.1 CRUD da 4a tabela: ESTOQUE (aba "Estoque")
--   7.2 Integracao com a Etapa 05 (aba "Rotinas do banco")
--   7.3 Dashboard com filtros (periodo, categoria, forma de pagamento)
--   7.4 Estatistica descritiva (media, mediana, variancia, desvio)
--   7.5 Consultas 9 a 14 com filtros (aba "Consultas")
--
-- Esta parte NAO cria objetos novos no banco: ela documenta os
-- comandos que a aplicacao Java executa. Os "?" sao preenchidos pela
-- aplicacao (PreparedStatement / CallableStatement); aqui aparecem
-- como variaveis @ para que tudo rode direto no DBeaver.
-- Pre-requisito: Partes 5 e 6 executadas (visoes, funcoes,
-- procedimentos e triggers).
-- #########################################################


-- =========================================================
-- 7.1 CRUD DA TABELA ESTOQUE  (dao/EstoqueDAO.java)
--
-- ESTOQUE tem chave composta (cd_produto, nr_lote). Na alteracao a
-- chave nao muda: so quantidade e datas.
-- A listagem usa as duas FUNCOES da Etapa 05 para mostrar a situacao
-- de cada lote e o estoque vendavel do produto.
-- =========================================================

-- LISTAR - EstoqueDAO.listar()
SELECT e.cd_produto, p.nome, e.nr_lote, e.quantidade, e.dt_entrada, e.dt_validade,
       fn_situacao_validade(e.dt_validade)  AS situacao,
       fn_estoque_disponivel(e.cd_produto)  AS disponivel_produto
FROM ESTOQUE e
JOIN PRODUTO p ON p.cd_produto = e.cd_produto
ORDER BY p.nome, e.dt_validade, e.nr_lote;

-- SUGESTAO DO PROXIMO LOTE - EstoqueDAO.proximoLote(?)
--   SELECT COALESCE(MAX(nr_lote), 0) + 1 FROM ESTOQUE WHERE cd_produto = ?;
-- INSERIR - EstoqueDAO.inserir()
--   INSERT INTO ESTOQUE (cd_produto, nr_lote, quantidade, dt_entrada, dt_validade) VALUES (?, ?, ?, ?, ?);
-- ALTERAR - EstoqueDAO.atualizar()
--   UPDATE ESTOQUE SET quantidade=?, dt_entrada=?, dt_validade=? WHERE cd_produto=? AND nr_lote=?;
-- EXCLUIR - EstoqueDAO.excluir()   (bloqueado pela FK se o lote tiver perdas - erro 1451)
--   DELETE FROM ESTOQUE WHERE cd_produto=? AND nr_lote=?;

-- Exemplo executavel (transacao com ROLLBACK)
START TRANSACTION;
SET @prod = 2;
SELECT COALESCE(MAX(nr_lote), 0) + 1 INTO @lote FROM ESTOQUE WHERE cd_produto = @prod;

INSERT INTO ESTOQUE (cd_produto, nr_lote, quantidade, dt_entrada, dt_validade)
VALUES (@prod, @lote, 40.000, CURDATE(), CURDATE() + INTERVAL 10 DAY);

UPDATE ESTOQUE SET quantidade = 35.500
WHERE cd_produto = @prod AND nr_lote = @lote;

SELECT nr_lote, quantidade, fn_situacao_validade(dt_validade) AS situacao,
       fn_estoque_disponivel(cd_produto) AS disponivel
FROM ESTOQUE WHERE cd_produto = @prod AND nr_lote = @lote;   -- 35.500 | ATENCAO | 35.500

DELETE FROM ESTOQUE WHERE cd_produto = @prod AND nr_lote = @lote;
ROLLBACK;


-- =========================================================
-- 7.2 INTEGRACAO COM A ETAPA 05  (dao/RotinasDAO.java)
-- =========================================================

-- Funcao 2 - estoque vendavel do produto escolhido (atualiza ao trocar o produto)
--   SELECT fn_estoque_disponivel(?);

-- Procedimento 1 - reajuste por categoria (CallableStatement, em transacao)
--   {CALL sp_reajustar_precos_categoria(?, ?)}
--   -> cada preco alterado dispara trg_produto_log_preco

-- Procedimento 2 - baixa FEFO com cursor (CallableStatement, em transacao)
--   {CALL sp_baixar_estoque_fefo(?, ?)}
--   -> o result set mostra quanto saiu de cada lote

-- Trigger 2 - registrar perda: le o lote, insere a perda e le de novo
--   SELECT quantidade FROM ESTOQUE WHERE cd_produto=? AND nr_lote=?;      -- antes
--   INSERT INTO PERDA (cd_produto, nr_lote, dt_perda, qt_perdida, motivo_perda, descarte_efetivado)
--   VALUES (?, ?, CURDATE(), ?, ?, ?);                                    -- dispara o trigger
--   SELECT quantidade FROM ESTOQUE WHERE cd_produto=? AND nr_lote=?;      -- depois

-- Lista de lotes do produto (campo "Lote")
--   SELECT nr_lote, CONCAT('Lote ', nr_lote, ' - ', quantidade, ' em estoque - validade ',
--                          DATE_FORMAT(dt_validade, '%d/%m/%Y'))
--   FROM ESTOQUE WHERE cd_produto = ? ORDER BY dt_validade;

-- Categorias que tem produtos (as unicas em que o reajuste tem efeito)
SELECT c.cd_categoria,
       CONCAT(c.nm_categoria, ' (', COUNT(t.cd_produto), ' produto(s))') AS categoria
FROM CATEGORIA c
JOIN TEM t ON t.cd_categoria = c.cd_categoria
GROUP BY c.cd_categoria, c.nm_categoria
ORDER BY c.nm_categoria;

-- Historico de precos gravado pelo trigger 1
SELECT id_log, dt_alteracao, cd_produto, nome_produto,
       valor_antigo, valor_novo, variacao_pct, usuario
FROM LOG_PRECO_PRODUTO
ORDER BY id_log DESC;


-- =========================================================
-- 7.3 DASHBOARD COM FILTROS  (dao/DashboardDAO.java)
--
-- Todos os indicadores e graficos recebem o mesmo filtro:
--   @ini, @fim  -> periodo (data da venda; nas perdas, data da perda)
--   @forma      -> forma de pagamento ('%' = todas)
--   @cat        -> categoria (0 = todas)
-- As medidas de venda sao somadas a partir dos ITENS (POSSUI x
-- PRODUTO), para que o filtro de categoria considere apenas os itens
-- daquela categoria. Sem filtro de categoria o total e igual ao
-- vl_total dos pedidos.
--
-- Condicao comum (FILTRO_ITENS no codigo Java):
--   pe.dt_venda BETWEEN ? AND ?
--   AND pe.forma_pagamento LIKE ?
--   AND (? = 0 OR EXISTS (SELECT 1 FROM TEM t
--                         WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = ?))
-- =========================================================

-- Periodo padrao dos filtros: da primeira a ultima data com movimento
SELECT LEAST(COALESCE((SELECT MIN(dt_venda) FROM PEDIDO), CURDATE()),
             COALESCE((SELECT MIN(dt_perda) FROM PERDA),  CURDATE())) AS inicio,
       GREATEST(COALESCE((SELECT MAX(dt_venda) FROM PEDIDO), CURDATE()),
                COALESCE((SELECT MAX(dt_perda) FROM PERDA),  CURDATE())) AS fim;

-- Valores de exemplo do filtro (altere para testar)
SET @ini = '2026-03-07', @fim = '2026-09-01', @forma = '%', @cat = 0;

-- Indicadores: pedidos, clientes ativos, faturamento (ticket = faturamento / pedidos)
SELECT COUNT(DISTINCT pe.nr_venda_mes)                          AS pedidos,
       COUNT(DISTINCT pe.nr_cliente)                            AS clientes_ativos,
       COALESCE(SUM(ps.quantidade * p.valor_kg_ou_unitario), 0) AS faturamento,
       (SELECT COUNT(*) FROM CLIENTE)                           AS total_clientes
FROM PEDIDO pe
JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
WHERE pe.dt_venda BETWEEN @ini AND @fim
  AND pe.forma_pagamento LIKE @forma
  AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                           WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = @cat));

-- Indicador: pedidos do filtro que tiveram devolucao (% = devolucoes / pedidos)
SELECT COUNT(*) AS pedidos_com_devolucao
FROM DEVOLUCAO d
WHERE d.nr_venda_mes IN (SELECT pe.nr_venda_mes
                         FROM PEDIDO pe
                         JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
                         JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
                         WHERE pe.dt_venda BETWEEN @ini AND @fim
                           AND pe.forma_pagamento LIKE @forma
                           AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                                                    WHERE t.cd_produto = p.cd_produto
                                                      AND t.cd_categoria = @cat)));

-- Indicador: prejuizo com perdas no periodo (% = prejuizo / faturamento)
SELECT COALESCE(SUM(pr.qt_perdida * p.valor_kg_ou_unitario), 0) AS prejuizo_perdas
FROM PERDA pr
JOIN PRODUTO p ON p.cd_produto = pr.cd_produto
WHERE pr.dt_perda BETWEEN @ini AND @fim
  AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                           WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = @cat));

-- Grafico 1: faturamento mensal
SELECT DATE_FORMAT(pe.dt_venda, '%Y-%m') AS mes,
       SUM(ps.quantidade * p.valor_kg_ou_unitario) AS total
FROM PEDIDO pe
JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
WHERE pe.dt_venda BETWEEN @ini AND @fim
  AND pe.forma_pagamento LIKE @forma
  AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                           WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = @cat))
GROUP BY DATE_FORMAT(pe.dt_venda, '%Y-%m')
ORDER BY mes;

-- Grafico 2: faturamento por forma de pagamento
SELECT pe.forma_pagamento, SUM(ps.quantidade * p.valor_kg_ou_unitario) AS total
FROM PEDIDO pe
JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
WHERE pe.dt_venda BETWEEN @ini AND @fim
  AND pe.forma_pagamento LIKE @forma
  AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                           WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = @cat))
GROUP BY pe.forma_pagamento
ORDER BY total DESC;

-- Grafico 3: top 10 produtos por quantidade vendida
SELECT p.nome, SUM(ps.quantidade) AS qtd
FROM PEDIDO pe
JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
WHERE pe.dt_venda BETWEEN @ini AND @fim
  AND pe.forma_pagamento LIKE @forma
  AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                           WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = @cat))
GROUP BY p.cd_produto, p.nome
ORDER BY qtd DESC
LIMIT 10;

-- Grafico 4: faturamento por categoria (8 maiores)
SELECT c.nm_categoria, SUM(ps.quantidade * p.valor_kg_ou_unitario) AS faturamento
FROM PEDIDO pe
JOIN POSSUI    ps ON ps.nr_venda_mes = pe.nr_venda_mes
JOIN PRODUTO   p  ON p.cd_produto    = ps.cd_produto
JOIN TEM       tc ON tc.cd_produto   = p.cd_produto
JOIN CATEGORIA c  ON c.cd_categoria  = tc.cd_categoria
WHERE pe.dt_venda BETWEEN @ini AND @fim
  AND pe.forma_pagamento LIKE @forma
  AND (@cat = 0 OR c.cd_categoria = @cat)
GROUP BY c.cd_categoria, c.nm_categoria
ORDER BY faturamento DESC
LIMIT 8;

-- Grafico 5: quantidade perdida por motivo (periodo pela data da perda)
SELECT COALESCE(pr.motivo_perda, 'Nao informado') AS motivo, SUM(pr.qt_perdida) AS qtd
FROM PERDA pr
JOIN PRODUTO p ON p.cd_produto = pr.cd_produto
WHERE pr.dt_perda BETWEEN @ini AND @fim
  AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                           WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = @cat))
GROUP BY pr.motivo_perda
ORDER BY qtd DESC;

-- Grafico 6: devolucoes dos pedidos do filtro, por forma de resolucao
SELECT COALESCE(d.forma_resolucao, 'Nao informada') AS resolucao, COUNT(*) AS qtd
FROM DEVOLUCAO d
WHERE d.nr_venda_mes IN (SELECT pe.nr_venda_mes
                         FROM PEDIDO pe
                         JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
                         JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
                         WHERE pe.dt_venda BETWEEN @ini AND @fim
                           AND pe.forma_pagamento LIKE @forma
                           AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                                                    WHERE t.cd_produto = p.cd_produto
                                                      AND t.cd_categoria = @cat)))
GROUP BY d.forma_resolucao
ORDER BY qtd DESC;


-- =========================================================
-- 7.4 ESTATISTICA DESCRITIVA  (card do Dashboard)
--
-- A aplicacao busca a AMOSTRA da variavel escolhida (com o mesmo
-- filtro) e calcula em util/Estatistica.java: media, mediana, moda,
-- variancia e desvio padrao AMOSTRAIS (divisor n - 1), coeficiente de
-- variacao, minimo e maximo. Quando nenhum valor se repete (variavel
-- continua), a moda e informada como CLASSE MODAL de uma distribuicao
-- de frequencias com k = 1 + 3,3*log10(n) classes (regra de Sturges).
--
-- Abaixo, a mesma estatistica calculada direto no SQL para conferencia
-- (variavel: valor de cada pedido).
-- =========================================================

-- Amostra: valor de cada pedido (Variavel.VALOR_PEDIDO)
--   SELECT SUM(ps.quantidade * p.valor_kg_ou_unitario) ... FILTRO ... GROUP BY pe.nr_venda_mes
-- Amostra: itens por pedido (Variavel.ITENS_PEDIDO)
--   SELECT COUNT(*) ... FILTRO ... GROUP BY pe.nr_venda_mes
-- Amostra: quantidade vendida por item (Variavel.QTD_ITEM)
--   SELECT ps.quantidade ... FILTRO

WITH valores AS (
    SELECT pe.nr_venda_mes,
           SUM(ps.quantidade * p.valor_kg_ou_unitario) AS valor
    FROM PEDIDO pe
    JOIN POSSUI  ps ON ps.nr_venda_mes = pe.nr_venda_mes
    JOIN PRODUTO p  ON p.cd_produto    = ps.cd_produto
    WHERE pe.dt_venda BETWEEN @ini AND @fim
      AND pe.forma_pagamento LIKE @forma
      AND (@cat = 0 OR EXISTS (SELECT 1 FROM TEM t
                               WHERE t.cd_produto = p.cd_produto AND t.cd_categoria = @cat))
    GROUP BY pe.nr_venda_mes
),
ordenados AS (
    SELECT valor,
           ROW_NUMBER() OVER (ORDER BY valor) AS pos,
           COUNT(*)     OVER ()               AS n
    FROM valores
)
SELECT MAX(n)                                    AS n,
       ROUND(AVG(valor), 2)                      AS media,
       ROUND(AVG(CASE WHEN pos IN (FLOOR((n + 1) / 2), CEIL((n + 1) / 2))
                      THEN valor END), 2)        AS mediana,
       ROUND(VAR_SAMP(valor), 2)                 AS variancia_amostral,
       ROUND(STDDEV_SAMP(valor), 2)              AS desvio_padrao_amostral,
       ROUND(100 * STDDEV_SAMP(valor) / AVG(valor), 1) AS coef_variacao_pct,
       ROUND(MIN(valor), 2)                      AS minimo,
       ROUND(MAX(valor), 2)                      AS maximo
FROM ordenados;
-- com os dados de exemplo e sem filtro: n=40, media=58,28, mediana=44,86,
-- variancia=2148,54, desvio=46,35, CV=79,5%  (iguais ao Dashboard)


-- =========================================================
-- 7.5 CONSULTAS 9 A 14 COM FILTROS  (dao/Consultas.java)
-- Mesmas consultas da Parte 5 com os filtros que a aba "Consultas"
-- oferece. Os valores @ abaixo equivalem a "sem filtro".
-- =========================================================
SET @bairro = '', @tipo = '%', @situacao = '%', @min_ped = 1, @produto = '';

-- Consulta 9 (anti join) + filtros de bairro e tipo de cliente
SELECT cl.nr_cliente, cl.cpf_cnpj,
       CASE WHEN CHAR_LENGTH(cl.cpf_cnpj) > 14
            THEN 'Pessoa Juridica' ELSE 'Pessoa Fisica' END AS tipo_cliente,
       COALESCE(cl.telefone, cl.telefone2, '(sem telefone)') AS contato,
       cl.bairro
FROM CLIENTE cl
LEFT JOIN PEDIDO pe ON pe.nr_cliente = cl.nr_cliente
WHERE pe.nr_venda_mes IS NULL
  AND cl.bairro LIKE CONCAT('%', @bairro, '%')
HAVING tipo_cliente LIKE @tipo
ORDER BY cl.bairro, cl.nr_cliente;

-- Consulta 10 (full outer join): filtro de situacao
--   ... mesma consulta da Parte 5, com  HAVING situacao LIKE ?  antes do ORDER BY
-- Consulta 11 (subconsultas): filtro de minimo de pedidos
--   ... mesma consulta da Parte 5, com  AND g.qtd_pedidos >= ?  no WHERE
-- Consulta 12 (subconsultas): filtro de forma de pagamento
--   ... mesma consulta da Parte 5, com  AND pe.forma_pagamento LIKE ?  no WHERE

-- Consulta 13 - visao vw_vendas_detalhadas com periodo, forma e produto
SELECT *
FROM vw_vendas_detalhadas
WHERE dt_venda BETWEEN '2026-08-01' AND '2026-08-31'
  AND forma_pagamento LIKE 'Pix'
  AND produto LIKE CONCAT('%', @produto, '%')
ORDER BY dt_venda, nr_venda_mes;

-- Consulta 14 - visao vw_situacao_lotes com situacao e produto
SELECT *
FROM vw_situacao_lotes
WHERE situacao LIKE @situacao
  AND produto LIKE CONCAT('%', @produto, '%')
ORDER BY dt_validade;
