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
