# HortifrutiApp — Etapa 03 (primeira versão da interface)

Aplicação desktop em **Java (Swing) + JDBC + MySQL** para o sistema administrativo do hortifrúti.
Todos os comandos SQL são explícitos (`PreparedStatement` / `Statement`) nas classes do pacote `dao`.

## Como executar

1. Crie o banco no MySQL (DBeaver ou terminal), na raiz do repositório:
   ```
   mysql -u root -p < modelo_fisico.sql
   mysql -u root -p hortifruti < inser_to.sql
   ```
2. (Opcional) ajuste usuário/senha em `db.properties`. Eles também podem ser digitados na tela de conexão ao abrir o sistema.
3. Execute:
   - **macOS/Linux:** `./run.sh`
   - **Windows:** `run.bat`
   - **Eclipse:** *File > Import > Existing Projects into Workspace* → pasta `app` → rodar `main.Exec`
   - **IntelliJ:** *File > Open* → pasta `app` → rodar `main.Exec` (working directory = pasta `app`)

Requer JDK 17 ou superior. O driver `mysql-connector-j-8.2.0.jar` já está em `lib/`.

## Funcionalidades

| Aba | O que faz |
|---|---|
| **Dashboard** | 6 indicadores (clientes, pedidos, faturamento, ticket médio, prejuízo com perdas, devoluções) e 6 gráficos gerados do banco |
| **Clientes** | Inserção, alteração, exclusão e busca na tabela `CLIENTE` |
| **Produtos** | Inserção, alteração, exclusão e busca na tabela `PRODUTO` |
| **Funcionarios** | Inserção, alteração, exclusão e busca na tabela `FUNCIONARIO`, com escolha do supervisor (auto-relacionamento) |
| **Consultas** | 8 consultas pré-definidas: mostra o SQL, aceita parâmetro quando houver, exibe o resultado em tabela e exporta CSV |
| **Graficos Estatistica** | Galeria com os gráficos feitos para a disciplina de Estatística (imagens da pasta `graficos/`) |

Erros do banco (violação de FK, CHECK, UNIQUE...) são traduzidos para mensagens claras — por exemplo,
tentar excluir um cliente que tem pedidos mostra que o registro está vinculado a outras tabelas.

## Consultas implementadas

O SQL completo está em [`../consultas.sql`](../consultas.sql) e em `src/dao/Consultas.java`.

| # | Consulta | Recursos SQL |
|---|---|---|
| 1 | Faturamento por categoria | JOIN de 4 tabelas (N:N), SUM/COUNT DISTINCT, função de janela para % do total |
| 2 | Ranking de clientes | JOIN + LEFT JOIN, GROUP BY, HAVING parametrizado, `RANK() OVER`, CASE (PF/PJ) |
| 3 | Hierarquia de funcionários | `WITH RECURSIVE`, auto-JOIN, subconsulta correlacionada |
| 4 | Índice de perdas por lote | JOIN por FK composta (PERDA→ESTOQUE), agregação, SUM condicional |
| 5 | Lotes vencidos / a vencer | LEFT JOIN em cadeia, GROUP_CONCAT, DATEDIFF/DATE_ADD, parâmetro de dias |
| 6 | Conferência de pedidos | JOIN de 4 tabelas + LEFT JOIN, GROUP_CONCAT, valor registrado × recalculado |
| 7 | Vendas mensais × forma de pagamento | Pivot com SUM + CASE, `WITH ROLLUP` |
| 8 | Produtos nunca vendidos | NOT EXISTS (anti-join), subconsultas escalares correlacionadas |

## Estrutura

```
src/
  main/Exec.java              ponto de entrada
  util/ConnectionFactory.java conexão JDBC (lê db.properties)
  model/                      Cliente, Produto, Funcionario, Consulta, ResultadoConsulta
  dao/                        ClienteDAO, ProdutoDAO, FuncionarioDAO (CRUD),
                              DashboardDAO, ConsultaDAO, Consultas (SQL das consultas)
  view/                       telas Swing (MainFrame, CrudPanel e cadastros, Dashboard, Consultas...)
  view/chart/                 gráficos desenhados em Java2D (barras, colunas, rosca)
lib/mysql-connector-j-8.2.0.jar
graficos/                     coloque aqui os PNG/JPG dos gráficos de Estatística
```
