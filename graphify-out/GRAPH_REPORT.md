# Graph Report - Profrutas-BD  (2026-10-21)

## Corpus Check
- Corpus is ~21,494 words - fits in a single context window. You may not need a graph.

## Summary
- 447 nodes · 926 edges · 22 communities (14 shown, 8 thin omitted)
- Extraction: 82% EXTRACTED · 18% INFERRED · 0% AMBIGUOUS · INFERRED: 165 edges (avg confidence: 0.85)
- Token cost: 99,579 input · 0 output

## Community Hubs (Navigation)
- Graficos Java2D Base
- Funcionario CRUD
- Consultas Pre-definidas
- Paineis Swing e CrudPanel
- Correlacao e Estatistica
- Produto CRUD
- Conexao JDBC e DAOs
- Cliente Modelo e DAO
- Inicializacao e Login
- Galeria Graficos Estatistica
- Banco e Esquema (docs)
- Cadastros e Erros (docs)
- Dashboard Consultas
- Correlacao Pearson (docs)
- Tabelas Produto e Estoque
- Consultas SQL (docs)
- Dashboard e Abas (docs)
- Modelo de Tabela Swing
- Busca nas Tabelas
- Carregamento Assincrono Dashboard
- Arquitetura em Camadas
- Script de Execucao

## God Nodes (most connected - your core abstractions)
1. `Funcionario` - 33 edges
2. `Cliente` - 27 edges
3. `CrudPanel` - 25 edges
4. `Produto` - 21 edges
5. `UI` - 21 edges
6. `GraficoDispersao` - 20 edges
7. `Consulta` - 18 edges
8. `ConnectionFactory` - 16 edges
9. `ConsultasPanel` - 16 edges
10. `FuncionarioPanel` - 13 edges

## Surprising Connections (you probably didn't know these)
- `Abas Clientes/Produtos/Funcionarios (CRUD)` --semantically_similar_to--> `CrudPanel (base dos cadastros)`  [INFERRED] [semantically similar]
  app/README.md → README.md
- `Aba Dashboard (6 indicadores, 6 graficos)` --semantically_similar_to--> `DashboardPanel`  [INFERRED] [semantically similar]
  app/README.md → README.md
- `Traducao de erros do banco (FK, CHECK, UNIQUE)` --semantically_similar_to--> `UI - traducao dos erros do MySQL`  [INFERRED] [semantically similar]
  app/README.md → README.md
- `Consultas implementadas (recursos SQL)` --semantically_similar_to--> `Consultas com JOIN (8 consultas)`  [INFERRED] [semantically similar]
  app/README.md → README.md
- `Aba Correlacao (scatter, Pearson, R2)` --semantically_similar_to--> `Correlacao de Pearson e regressao linear`  [INFERRED] [semantically similar]
  app/README.md → README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Fluxo em camadas view -> dao -> util -> MySQL** — readme_camada_view, readme_camada_dao, readme_camada_util, readme_camada_model, readme_banco_hortifruti [EXTRACTED 1.00]
- **Pipeline do grafico de dispersao e correlacao** — readme_correlacaodao, readme_estatistica, readme_graficodispersao, readme_correlacaopanel, readme_correlacao_pearson [INFERRED 0.85]
- **Tabelas associativas N:N de PRODUTO** — readme_tabela_produto, readme_tabela_fornece, readme_tabela_tem, readme_tabela_possui [EXTRACTED 1.00]

## Communities (22 total, 8 thin omitted)

### Community 0 - "Graficos Java2D Base"
Cohesion: 0.05
Nodes (7): Grafico, GraficoBarras, GraficoColunas, GraficoRosca, DashboardPanel, MainFrame, UI

### Community 1 - "Funcionario CRUD"
Cohesion: 0.11
Nodes (3): FuncionarioDAO, Funcionario, FuncionarioPanel

### Community 2 - "Consultas Pre-definidas"
Cohesion: 0.07
Nodes (10): ConsultaDAO, Consultas, Consulta, TipoParametro, INTEIRO, NENHUM, TEXTO, ResultadoConsulta (+2 more)

### Community 3 - "Paineis Swing e CrudPanel"
Cohesion: 0.08
Nodes (3): SwingWorker<List<Ponto>, Void>, CrudPanel, GraficosEstatisticaPanel

### Community 4 - "Correlacao e Estatistica"
Cohesion: 0.11
Nodes (7): CorrelacaoDAO, Par, Ponto, Correlacao, Estatistica, GraficoDispersao, CorrelacaoPanel

### Community 5 - "Produto CRUD"
Cohesion: 0.12
Nodes (3): ProdutoDAO, Produto, ProdutoPanel

### Community 6 - "Conexao JDBC e DAOs"
Cohesion: 0.12
Nodes (3): ClienteDAO, ConnectionFactory, ClientePanel

### Community 10 - "Banco e Esquema (docs)"
Cohesion: 0.18
Nodes (13): HortifrutiApp - Etapa 03, GitHub Repository Marcelomenezes07/Profrutas-BD, Banco MySQL hortifruti, ConnectionFactory, app/db.properties, Dicionario de Dados, Esquema Relacional, Etapa 03 - Interface funcional com dashboard (+5 more)

### Community 11 - "Cadastros e Erros (docs)"
Cohesion: 0.22
Nodes (9): Abas Clientes/Produtos/Funcionarios (CRUD), Traducao de erros do banco (FK, CHECK, UNIQUE), ClienteDAO / ProdutoDAO / FuncionarioDAO, Fluxo: salvar cliente novo, Tabela CLIENTE, Tabela DEVOLUCAO, Tabela FUNCIONARIO, Tabela PEDIDO (+1 more)

### Community 13 - "Correlacao Pearson (docs)"
Cohesion: 0.25
Nodes (7): Aba Correlacao (scatter, Pearson, R2), Camada util, CorrelacaoDAO, CorrelacaoPanel, Estatistica (Pearson, R2, regressao), GraficoDispersao (scatter plot), Graficos Java2D (view/chart)

### Community 14 - "Tabelas Produto e Estoque"
Cohesion: 0.29
Nodes (7): Tabela CATEGORIA, Tabela ESTOQUE, Tabela FORNECE, Tabela FORNECEDOR, Tabela PERDA, Tabela PRODUTO, Tabela TEM

### Community 15 - "Consultas SQL (docs)"
Cohesion: 0.40
Nodes (6): Aba Consultas (8 pre-definidas, exporta CSV), Consultas implementadas (recursos SQL), ConsultaDAO / Consultas, Consultas com JOIN (8 consultas), consultas.sql, Tabela POSSUI

### Community 16 - "Dashboard e Abas (docs)"
Cohesion: 0.33
Nodes (6): Aba Dashboard (6 indicadores, 6 graficos), Aba Graficos Estatistica, Funcionalidades (7 abas), DashboardDAO, DashboardPanel, Graficos de Estatistica (app/graficos)

### Community 20 - "Arquitetura em Camadas"
Cohesion: 0.33
Nodes (5): Camada dao (Data Access Object), Camada main (Exec.java), Camada model (entidades), Camada view (Swing), run.sh / run.bat

## Knowledge Gaps
- **18 isolated node(s):** `NENHUM`, `INTEIRO`, `TEXTO`, `GitHub Repository Marcelomenezes07/Profrutas-BD`, `Dicionario de Dados` (+13 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 108 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CrudPanel` connect `Paineis Swing e CrudPanel` to `Graficos Java2D Base`, `Funcionario CRUD`, `Produto CRUD`, `Conexao JDBC e DAOs`, `Cliente Modelo e DAO`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **What connects `NENHUM`, `INTEIRO`, `TEXTO` to the rest of the system?**
  _18 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Graficos Java2D Base` be split into smaller, more focused modules?**
  _Cohesion score 0.05407925407925408 - nodes in this community are weakly interconnected._
- **Why does `MainFrame` connect `Graficos Java2D Base` to `Funcionario CRUD`, `Consultas Pre-definidas`, `Paineis Swing e CrudPanel`, `Correlacao e Estatistica`, `Produto CRUD`, `Conexao JDBC e DAOs`?**
  _High betweenness centrality (0.101) - this node is a cross-community bridge._
- **Should `Funcionario CRUD` be split into smaller, more focused modules?**
  _Cohesion score 0.10606060606060606 - nodes in this community are weakly interconnected._
- **Why does `UI` connect `Graficos Java2D Base` to `Consultas Pre-definidas`, `Paineis Swing e CrudPanel`, `Correlacao e Estatistica`, `Inicializacao e Login`, `Modelo de Tabela Swing`, `Carregamento Assincrono Dashboard`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Should `Consultas Pre-definidas` be split into smaller, more focused modules?**
  _Cohesion score 0.07293868921775898 - nodes in this community are weakly interconnected._