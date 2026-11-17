# Sistema Administrativo para Empresa de Varejo do Ramo Hortifrúti

## Descrição do Minimundo

Uma empresa de varejo do ramo de hortifrúti precisa informatizar o controle administrativo de suas operações de cadastro, compras, estoque, vendas e atendimento aos clientes.

Sobre os **clientes** atendidos pela loja, deve-se armazenar um número de cadastro, o CPF ou CNPJ (já que tanto pessoas físicas quanto jurídicas podem comprar), telefones de contato e endereço completo (CEP, rua, bairro, número e, quando houver, apartamento).

Os **funcionários** da empresa são identificados por um número de matrícula, sendo conhecidos nome, CPF, telefone e endereço. Existe uma hierarquia entre eles: um funcionário pode supervisionar vários outros funcionários, mas é supervisionado por, no máximo, um único funcionário superior.

Os **produtos** comercializados (frutas, verduras, legumes etc.) são identificados por um código e possuem nome, descrição, marca e valor, que pode ser por unidade ou por quilo, dependendo de o produto ser vendido de forma unitária ou fracionada — essa informação também é registrada. Cada produto pode ser classificado em uma ou mais **categorias** (por exemplo, frutas, verduras, orgânicos), e cada categoria pode agrupar diversos produtos.

Os produtos são adquiridos de **fornecedores**, dos quais se conhece código, nome, CNPJ, telefones e endereço. Um mesmo produto pode ser fornecido por diversos fornecedores diferentes, e um fornecedor pode fornecer diversos produtos; para cada relação de fornecimento é necessário registrar o modo de transporte utilizado na entrega.

O controle de **estoque** é feito por lote: cada produto pode ter vários lotes, sendo cada lote identificado pelo código do produto somado a um número de lote, com quantidade em estoque, data de entrada e data de validade.

Quando um lote de produto vence, estraga ou é danificado antes da venda, deve ser registrada uma **perda**, identificada por um número, associada ao lote de estoque correspondente, contendo data da perda, quantidade perdida, motivo e indicação de o descarte já ter sido efetivado.

As vendas realizadas pela empresa são registradas como **pedidos**, identificados por um número, vinculados obrigatoriamente a um cliente. De cada pedido são conhecidos a data da venda, a forma de pagamento e o valor total (que corresponde ao somatório dos itens vendidos). Cada pedido é composto por um ou mais produtos, podendo um mesmo produto aparecer em diversos pedidos diferentes, sendo necessário registrar a quantidade vendida de cada produto em cada pedido.

Um pedido pode, eventualmente, ser objeto de uma **devolução** por parte do cliente. Cada devolução está associada a exatamente um pedido, e um pedido pode gerar no máximo uma devolução, sendo registrados número de identificação, data da devolução, motivo, forma de resolução adotada e indicação de a devolução já ter sido efetivada.

---

## 4. Esquema Relacional

Convenção: chave primária (PK) sublinhada representada em **negrito**; chave estrangeira (FK) marcada com asterisco (*).

```
CLIENTE (nr_cliente, cpf_cnpj, telefone, telefone2, cep, rua, bairro, numero, apartamento)
    PK: nr_cliente

FUNCIONARIO (nr_funcionario, nome, cpf, telefone, cep, rua, bairro, numero, apartamento, nr_funcionario_supervisor*)
    PK: nr_funcionario
    FK: nr_funcionario_supervisor -> FUNCIONARIO(nr_funcionario)

FORNECEDOR (cd_fornecedor, nome, cnpj_fornecedor, telefone, telefone2, cep, rua, bairro, numero)
    PK: cd_fornecedor

PRODUTO (cd_produto, nome, descricao, valor_kg_ou_unitario, vendido_unitario, marca)
    PK: cd_produto

CATEGORIA (cd_categoria, nm_categoria)
    PK: cd_categoria

ESTOQUE (cd_produto*, nr_lote, quantidade, dt_validade, dt_entrada)
    PK: cd_produto, nr_lote
    FK: cd_produto -> PRODUTO(cd_produto)

PEDIDO (nr_venda_mes, nr_cliente*, dt_venda, forma_pagamento, vl_total)
    PK: nr_venda_mes
    FK: nr_cliente -> CLIENTE(nr_cliente)

DEVOLUCAO (n_devolucao, nr_venda_mes*, dt_devolucao, forma_resolucao, motivo, devolucao_efetivada)
    PK: n_devolucao
    FK: nr_venda_mes -> PEDIDO(nr_venda_mes)  [UNIQUE - relação 1:1 opcional]

PERDA (nr_perda, cd_produto*, nr_lote*, dt_perda, qt_perdida, motivo_perda, descarte_efetivado)
    PK: nr_perda
    FK: (cd_produto, nr_lote) -> ESTOQUE(cd_produto, nr_lote)

FORNECE (cd_fornecedor*, cd_produto*, modo_transporte)
    PK: cd_fornecedor, cd_produto
    FK: cd_fornecedor -> FORNECEDOR(cd_fornecedor)
    FK: cd_produto -> PRODUTO(cd_produto)

TEM (cd_categoria*, cd_produto*)
    PK: cd_categoria, cd_produto
    FK: cd_categoria -> CATEGORIA(cd_categoria)
    FK: cd_produto -> PRODUTO(cd_produto)

POSSUI (cd_produto*, nr_venda_mes*, quantidade)
    PK: cd_produto, nr_venda_mes
    FK: cd_produto -> PRODUTO(cd_produto)
    FK: nr_venda_mes -> PEDIDO(nr_venda_mes)
```

---

## 5. Dicionário de Dados

### CLIENTE

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| nr_cliente | INT | - | Não | PK | Número de cadastro do cliente |
| cpf_cnpj | VARCHAR | 18 | Não | - | CPF (pessoa física) ou CNPJ (pessoa jurídica) do cliente |
| telefone | VARCHAR | 15 | Sim | - | Telefone principal de contato |
| telefone2 | VARCHAR | 15 | Sim | - | Telefone secundário de contato |
| cep | VARCHAR | 9 | Não | - | CEP do endereço |
| rua | VARCHAR | 100 | Não | - | Logradouro |
| bairro | VARCHAR | 60 | Não | - | Bairro |
| numero | VARCHAR | 10 | Não | - | Número do endereço |
| apartamento | VARCHAR | 10 | Sim | - | Complemento (apartamento/bloco), se houver |

### FUNCIONARIO

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| nr_funcionario | INT | - | Não | PK | Matrícula do funcionário |
| nome | VARCHAR | 100 | Não | - | Nome completo do funcionário |
| cpf | CHAR | 11 | Não | - | CPF do funcionário |
| telefone | VARCHAR | 15 | Sim | - | Telefone de contato |
| cep | VARCHAR | 9 | Não | - | CEP do endereço |
| rua | VARCHAR | 100 | Não | - | Logradouro |
| bairro | VARCHAR | 60 | Não | - | Bairro |
| numero | VARCHAR | 10 | Não | - | Número do endereço |
| apartamento | VARCHAR | 10 | Sim | - | Complemento, se houver |
| nr_funcionario_supervisor | INT | - | Sim | FK | Matrícula do funcionário que o supervisiona (auto-relacionamento) |

### FORNECEDOR

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| cd_fornecedor | INT | - | Não | PK | Código do fornecedor |
| nome | VARCHAR | 100 | Não | - | Razão social/nome do fornecedor |
| cnpj_fornecedor | CHAR | 14 | Não | - | CNPJ do fornecedor |
| telefone | VARCHAR | 15 | Sim | - | Telefone principal |
| telefone2 | VARCHAR | 15 | Sim | - | Telefone secundário |
| cep | VARCHAR | 9 | Não | - | CEP do endereço |
| rua | VARCHAR | 100 | Não | - | Logradouro |
| bairro | VARCHAR | 60 | Não | - | Bairro |
| numero | VARCHAR | 10 | Não | - | Número do endereço |

### PRODUTO

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| cd_produto | INT | - | Não | PK | Código do produto |
| nome | VARCHAR | 100 | Não | - | Nome do produto |
| descricao | VARCHAR | 255 | Sim | - | Descrição do produto |
| valor_kg_ou_unitario | DECIMAL | (10,2) | Não | - | Valor por quilo ou por unidade, conforme o tipo de venda |
| vendido_unitario | BOOLEAN | - | Não | - | Indica se o produto é vendido por unidade (verdadeiro) ou por peso (falso) |
| marca | VARCHAR | 60 | Sim | - | Marca do produto |

### CATEGORIA

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| cd_categoria | INT | - | Não | PK | Código da categoria |
| nm_categoria | VARCHAR | 60 | Não | - | Nome da categoria (ex.: frutas, verduras, orgânicos) |

### ESTOQUE

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| cd_produto | INT | - | Não | PK, FK | Código do produto em estoque |
| nr_lote | INT | - | Não | PK | Número do lote |
| quantidade | DECIMAL | (10,3) | Não | - | Quantidade em estoque (permite fração para produtos vendidos por peso) |
| dt_validade | DATE | - | Não | - | Data de validade do lote |
| dt_entrada | DATE | - | Não | - | Data de entrada do lote no estoque |

### PEDIDO

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| nr_venda_mes | INT | - | Não | PK | Número do pedido/venda |
| nr_cliente | INT | - | Não | FK | Cliente que realizou o pedido |
| dt_venda | DATE | - | Não | - | Data em que a venda foi realizada |
| forma_pagamento | VARCHAR | 30 | Não | - | Forma de pagamento utilizada (ex.: dinheiro, cartão, pix) |
| vl_total | DECIMAL | (10,2) | Não | - | Valor total do pedido (soma dos itens) |

### DEVOLUCAO

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| n_devolucao | INT | - | Não | PK | Número da devolução |
| nr_venda_mes | INT | - | Não | FK (única) | Pedido ao qual a devolução se refere |
| dt_devolucao | DATE | - | Não | - | Data da devolução |
| forma_resolucao | VARCHAR | 50 | Sim | - | Forma de resolução adotada (ex.: troca, estorno) |
| motivo | VARCHAR | 255 | Sim | - | Motivo da devolução |
| devolucao_efetivada | BOOLEAN | - | Não | - | Indica se a devolução já foi efetivada |

### PERDA

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| nr_perda | INT | - | Não | PK | Número da perda |
| cd_produto | INT | - | Não | FK | Produto do lote perdido (referencia ESTOQUE) |
| nr_lote | INT | - | Não | FK | Lote perdido (referencia ESTOQUE) |
| dt_perda | DATE | - | Não | - | Data em que a perda foi registrada |
| qt_perdida | DECIMAL | (10,3) | Não | - | Quantidade perdida |
| motivo_perda | VARCHAR | 255 | Sim | - | Motivo da perda (ex.: vencimento, avaria) |
| descarte_efetivado | BOOLEAN | - | Não | - | Indica se o descarte já foi efetivado |

### FORNECE

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| cd_fornecedor | INT | - | Não | PK, FK | Fornecedor do produto |
| cd_produto | INT | - | Não | PK, FK | Produto fornecido |
| modo_transporte | VARCHAR | 50 | Sim | - | Modo de transporte usado na entrega |

### TEM

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| cd_categoria | INT | - | Não | PK, FK | Categoria associada ao produto |
| cd_produto | INT | - | Não | PK, FK | Produto associado à categoria |

### POSSUI

| Atributo | Tipo | Tamanho | Nulo? | Chave | Descrição |
|---|---|---|---|---|---|
| cd_produto | INT | - | Não | PK, FK | Produto incluído no pedido |
| nr_venda_mes | INT | - | Não | PK, FK | Pedido ao qual o item pertence |
| quantidade | DECIMAL | (10,3) | Não | - | Quantidade do produto vendida nesse pedido |

---

## Etapa 03 — Interface funcional com dashboard

- Aplicação Java (Swing + JDBC): pasta [`app/`](app/) — detalhes em [`app/README.md`](app/README.md)
- SQL das consultas: [`consultas.sql`](consultas.sql)

### Como rodar o projeto

#### Pré-requisitos

- **JDK 17 ou superior** (testado com JDK 21) — verifique com `java -version`
- **MySQL 8 ou superior** em execução (ex.: `brew services start mysql` no macOS)
- O driver JDBC do MySQL já vem no projeto: `app/lib/mysql-connector-j-8.2.0.jar`

#### 1. Criar o banco de dados

Execute os scripts **nesta ordem**, pelo DBeaver (abrir o arquivo e executar com *Alt+X*) ou pelo terminal, na raiz do repositório:

```bash
mysql -u root -p < modelo_fisico.sql
mysql -u root -p hortifruti < inser_to.sql
```

O primeiro cria o banco `hortifruti` e as tabelas; o segundo insere os dados.
Se o banco já existir, apague-o antes (`DROP DATABASE hortifruti;`) para evitar erros de tabela/dado duplicado.

#### 2. Configurar o acesso ao MySQL

Ao abrir, a aplicação mostra a tela **"Conectar ao MySQL"**, onde se informa:

| Campo | Valor |
|---|---|
| URL JDBC | `jdbc:mysql://localhost:3306/hortifruti` (já preenchido) |
| Usuário | `root` (ou outro usuário do seu MySQL) |
| Senha | a **senha do seu MySQL** (a mesma usada no DBeaver) |

Para não digitar a senha toda vez, preencha `db.password` em [`app/db.properties`](app/db.properties)
(não envie esse arquivo com a senha para o GitHub).

#### 3. Executar

**Pelo terminal (macOS/Linux):**

```bash
./app/run.sh
```

**Pelo terminal (Windows):** dar dois cliques em `app\run.bat` ou executar `app\run.bat` no prompt.

**Pelo IntelliJ IDEA:**
1. *File → Open* e selecione a pasta `Profrutas-BD` (ou a pasta `app`).
2. Confirme que o driver está nas dependências: *File → Project Structure → Modules → Dependencies*
   deve listar `mysql-connector-j-8.2.0.jar`. Se não listar, clique em **+ → JARs or Directories**
   e selecione `app/lib/mysql-connector-j-8.2.0.jar`.
3. Abra `app/src/main/Exec.java` e clique em **Run**.

**Pelo Eclipse:**
1. *File → Import → General → Existing Projects into Workspace* e selecione a pasta `app`.
2. Clique com o botão direito em `src/main/Exec.java` → *Run As → Java Application*.

#### Problemas comuns

| Mensagem | Causa / solução |
|---|---|
| `No suitable driver found for jdbc:mysql://...` | O `.jar` do driver não está no classpath — veja o passo 2 do IntelliJ acima. |
| `Usuario ou senha do MySQL incorretos` | Senha errada na tela de conexão ou em `db.properties`. |
| `O banco 'hortifruti' nao existe` | Rode `modelo_fisico.sql` e `inser_to.sql` (passo 1). |
| `Nao foi possivel conectar ao MySQL` | O servidor MySQL não está em execução ou usa outra porta (ajuste a URL). |

#### Gráficos de Estatística

Coloque as imagens (PNG/JPG) dos gráficos feitos para a disciplina de Estatística em `app/graficos/`
ou use o botão **"Adicionar imagem"** na aba *Graficos Estatistica* da aplicação.

---

## Estrutura do projeto

O projeto é dividido em **camadas**, cada uma com uma responsabilidade — o mesmo padrão do projeto-modelo
da disciplina (`util` / `model` / `dao` / `main`), com uma camada a mais, `view`, para a interface gráfica.

### Visão geral

```
Profrutas-BD/
├── modelo_fisico.sql     cria o banco e as tabelas (Etapa 02)
├── inser_to.sql          insere os dados (Etapa 02)
├── consultas.sql         SQL das consultas (entregável 2 da Etapa 03)
├── README.md             documentação do projeto
└── app/                  a aplicação Java (entregável 1 da Etapa 03)
    ├── src/              código-fonte
    ├── lib/              driver JDBC do MySQL
    ├── graficos/         imagens dos gráficos de Estatística
    ├── db.properties     usuário/senha do banco
    ├── run.sh / run.bat  scripts para compilar e rodar
    └── .classpath, .project, .iml, .idea/   configuração do Eclipse e do IntelliJ
```

### Como as camadas conversam

```
 [ view ]  telas que o usuário vê e clica
    │  chama
    ▼
 [ dao ]   onde está o SQL — monta o comando e manda ao banco
    │  usa
    ▼
 [ util ]  abre a conexão com o MySQL (JDBC)
    │
    ▼
  MySQL (banco hortifruti)

 [ model ] objetos simples (Cliente, Produto...) que carregam os dados entre as camadas
```

A tela **nunca** escreve SQL: ela pede ao DAO, o DAO executa o SQL e devolve objetos do `model`.

### Pacote por pacote (`app/src/`)

#### `main/`
- **`Exec.java`** — onde o programa começa (método `main`). Aplica o visual, abre a tela de conexão e, se conectar, abre a janela principal.

#### `util/`
- **`ConnectionFactory.java`** — a "fábrica de conexões". Guarda URL, usuário e senha do banco (lidos do `db.properties` ou da tela de conexão). O método `getConnection()` usa o `DriverManager` do JDBC. Todo DAO pede a conexão aqui.
- **`Estatistica.java`** — cálculos estatísticos do gráfico de dispersão: coeficiente de correlação de **Pearson** (r), **R²**, **reta de regressão linear** por mínimos quadrados (y = a + b·x) e a classificação da força da correlação.

#### `model/` — as entidades
Classes que só guardam dados (atributos + getters/setters). Cada uma espelha uma tabela:
- **`Cliente.java`** — tabela CLIENTE.
- **`Produto.java`** — tabela PRODUTO.
- **`Funcionario.java`** — tabela FUNCIONARIO (tem também `nomeSupervisor`, que vem de um JOIN).
- **`Consulta.java`** — descreve uma consulta pronta: título, descrição, SQL e (se tiver) o parâmetro.
- **`ResultadoConsulta.java`** — resultado de uma consulta: nomes das colunas + linhas.

#### `dao/` — acesso ao banco (**onde está o SQL explícito**)
DAO = *Data Access Object*.
- **`ClienteDAO`, `ProdutoDAO`, `FuncionarioDAO`** — o CRUD de cada tabela: `inserir()`, `listar()`, `atualizar()`, `excluir()`. Cada um tem o SQL escrito (`INSERT INTO ... VALUES (?, ?...)`) e usa `PreparedStatement`, que preenche os `?` com segurança (evita *SQL injection*).
- **`Consultas.java`** — a lista das 8 consultas, com o SQL completo de cada uma (é o mesmo SQL do `consultas.sql`).
- **`ConsultaDAO.java`** — executa qualquer uma das 8 consultas e lê as colunas do resultado com `ResultSetMetaData`, por isso funciona para todas sem código específico.
- **`DashboardDAO.java`** — as consultas que alimentam os indicadores e gráficos do dashboard.
- **`CorrelacaoDAO.java`** — as consultas (com JOIN) que geram os pares (X, Y) do gráfico de dispersão.

#### `view/` — a interface gráfica (Swing)
- **`MainFrame.java`** — a janela principal com as 7 abas. Recarrega os dados sempre que se troca de aba.
- **`ConexaoDialog.java`** — a tela inicial que pede usuário/senha e testa a conexão.
- **`CrudPanel.java`** — **base comum** das telas de cadastro: monta tabela, busca, formulário e os botões Novo/Salvar/Excluir. Cada cadastro só diz *o que* fazer; o `CrudPanel` cuida de *como* mostrar.
- **`ClientePanel`, `ProdutoPanel`, `FuncionarioPanel`** — os cadastros em si. Herdam do `CrudPanel`, validam os campos (CPF com 11 dígitos, valor > 0...) e chamam o DAO correspondente.
- **`DashboardPanel.java`** — 6 cards de indicadores + 6 gráficos. Carrega os dados em segundo plano (`SwingWorker`) para a tela não travar.
- **`ConsultasPanel.java`** — lista das consultas, caixa com o SQL, campo de parâmetro, tabela de resultado e exportação CSV.
- **`CorrelacaoPanel.java`** — aba de correlação: escolha do par de variáveis, gráfico de dispersão, interpretação do resultado e o SQL que gera os pontos.
- **`GraficosEstatisticaPanel.java`** — galeria que mostra as imagens da pasta `graficos/`.
- **`UI.java`** — utilitários visuais: cores, fontes, formatação de moeda (R$) e a **tradução dos erros do MySQL** (ex.: erro 1451, violação de chave estrangeira, vira "não é possível excluir: registro vinculado a outras tabelas").

#### `view/chart/` — gráficos
Desenhados à mão com Java2D, sem nenhuma biblioteca externa:
- **`Grafico.java`** — classe base (título, "sem dados", formato dos números).
- **`GraficoBarras.java`** — barras horizontais (top produtos, categorias, perdas).
- **`GraficoColunas.java`** — colunas verticais com linha de tendência (faturamento mensal).
- **`GraficoRosca.java`** — rosca com legenda e percentuais (formas de pagamento, devoluções).
- **`GraficoDispersao.java`** — **scatter plot** com a reta de regressão, o r de Pearson, o R², a equação da reta e *tooltip* com os valores de cada ponto.

### Exemplo: o que acontece ao salvar um cliente novo

1. Você preenche o formulário e clica em **Salvar** (`CrudPanel`).
2. `ClientePanel.gravar()` valida os campos e monta um objeto `Cliente`.
3. Chama `ClienteDAO.inserir(cliente)`.
4. O DAO pede uma conexão à `ConnectionFactory` e executa `INSERT INTO CLIENTE (...) VALUES (?, ?, ...)`.
5. O MySQL grava ou devolve um erro (ex.: CPF duplicado).
6. Se deu certo, a tabela é recarregada (`listar()`); se deu erro, o `UI` mostra a mensagem traduzida.

### Outros arquivos da pasta `app/`

- **`lib/mysql-connector-j-8.2.0.jar`** — o driver JDBC: é o que permite o Java "falar" com o MySQL. Sem ele aparece o erro *"No suitable driver"*.
- **`db.properties`** — configuração da conexão (para não deixar a senha fixa no código).
- **`run.sh` / `run.bat`** — compilam (`javac`) e executam (`java`) sem precisar de IDE.
- **`graficos/`** — onde ficam as imagens da disciplina de Estatística.
- **`.classpath`, `.project`, `.settings/`** (Eclipse) e **`.iml`, `.idea/`** (IntelliJ) — dizem à IDE onde está o código e o driver.
- **`bin/`** — `.class` compilados; gerada automaticamente, não precisa ser entregue.

---

## Consultas com JOIN

Das 8 consultas, **7 usam JOIN** (o requisito era ao menos 1):

| # | Consulta | JOINs utilizados |
|---|---|---|
| 1 | Faturamento por categoria | CATEGORIA ⋈ TEM ⋈ PRODUTO ⋈ POSSUI |
| 2 | Ranking de clientes | CLIENTE ⋈ PEDIDO ⟕ DEVOLUCAO |
| 3 | Hierarquia de funcionários | FUNCIONARIO ⋈ hierarquia (CTE recursiva) ⟕ FUNCIONARIO (auto-JOIN) |
| 4 | Índice de perdas por lote | PERDA ⋈ ESTOQUE (FK composta) ⋈ PRODUTO |
| 5 | Lotes vencidos / a vencer | ESTOQUE ⋈ PRODUTO ⟕ FORNECE ⟕ FORNECEDOR |
| 6 | Conferência de pedidos | PEDIDO ⋈ CLIENTE ⋈ POSSUI ⋈ PRODUTO ⟕ DEVOLUCAO |
| 7 | Vendas mensais × forma de pagamento | — (agregação condicional / pivot) |
| 8 | Produtos nunca vendidos | PRODUTO ⟕ TEM ⟕ CATEGORIA + NOT EXISTS |

(⋈ = INNER JOIN, ⟕ = LEFT JOIN). As consultas do gráfico de dispersão também usam JOIN.

---

## Gráfico de dispersão e correlação de Pearson

A aba **Correlacao** mostra um *scatter plot* com:

- os **pontos** (X, Y) obtidos do banco por consultas com JOIN (`CorrelacaoDAO`);
- a **reta de regressão linear** (em vermelho), calculada por mínimos quadrados: `y = a + b·x`;
- o **coeficiente de correlação de Pearson (r)**, o **R²** e a interpretação (fraca / moderada / forte, positiva / negativa).

Fórmulas usadas (`util/Estatistica.java`):

```
r = Σ(x − x̄)(y − ȳ) / √( Σ(x − x̄)² · Σ(y − ȳ)² )
b = Σ(x − x̄)(y − ȳ) / Σ(x − x̄)²        a = ȳ − b·x̄        R² = r²
```

Pares de variáveis disponíveis (resultados com os dados do `inser_to.sql`):

| Variáveis (X × Y) | n | r | Interpretação |
|---|---|---|---|
| Pedidos: quantidade de itens × valor total | 40 | 0,670 | moderada positiva |
| Clientes: nº de pedidos × total gasto | 23 | 0,795 | forte positiva |
| Produtos: preço × faturamento | 24 | 0,692 | moderada positiva |
| Produtos: preço × quantidade vendida | 24 | 0,121 | desprezível |
| Lotes: vida útil (dias) × quantidade perdida | 19 | 0,479 | fraca positiva |

Classificação usada para |r|: < 0,3 desprezível · 0,3–0,5 fraca · 0,5–0,7 moderada · 0,7–0,9 forte · ≥ 0,9 muito forte.

O arquivo `consultas.sql` traz também uma versão do cálculo de Pearson e da reta **feita inteiramente em SQL**,
que confere com o valor mostrado na aplicação (r = 0,6696 para o primeiro par).

---

## Entrega 4 — Índices, consultas e visões

Todo o SQL desta entrega está na **Parte 5** do arquivo [`consultas.sql`](consultas.sql) (seção *"PARTE 5 - ENTREGA 4"*).
As 4 consultas novas e as 2 visões também aparecem na aba **Consultas** da aplicação (itens 9 a 14).

> **Como executar:** com o banco já criado (`modelo_fisico.sql` + `inser_to.sql`), execute o `consultas.sql`
> (ou apenas a Parte 5). Os `CREATE INDEX` devem rodar **uma única vez**; para reexecutar a Parte 5,
> rode antes os comandos de remoção da seção 5.4 (`DROP VIEW` / `DROP INDEX`).

### 1. Índices

O MySQL (InnoDB) já cria índices automaticamente para as chaves primárias e para todas as chaves
estrangeiras (ex.: `PEDIDO.nr_cliente`, `PERDA(cd_produto, nr_lote)`). Por isso os índices novos
ficam em colunas que **não são chave**, mas que as consultas filtram — sem eles, essas buscas
percorrem a tabela inteira.

| Índice | Tabela (colunas) | Por que é necessário | Onde é usado |
|---|---|---|---|
| `idx_pedido_pagamento_data` | `PEDIDO (forma_pagamento, dt_venda)` | Relatórios de venda filtram por forma de pagamento **e** período (ex.: "Pix em agosto"). A coluna de igualdade vem primeiro e a de intervalo depois, ordem que permite usar as duas partes do índice. | Consulta 6 (filtro por forma de pagamento), **Consulta 12** (subconsulta correlacionada por forma + mês) e visão `vw_vendas_detalhadas` filtrada por forma/período |
| `idx_estoque_validade` | `ESTOQUE (dt_validade)` | Produtos perecíveis: o controle de validade é diário e sempre é um filtro de intervalo sobre `dt_validade`, que não é chave. | Consulta 5 (lotes vencidos/a vencer) e visão **`vw_situacao_lotes`** filtrada por validade |

O uso foi comprovado com `EXPLAIN` (incluído no script):

```
-> Index range scan on PEDIDO using idx_pedido_pagamento_data
   over (forma_pagamento = 'Pix' AND '2026-08-01' <= dt_venda <= '2026-08-31')
-> Covering index range scan on ESTOQUE using idx_estoque_validade
   over ('2026-09-01' <= dt_validade <= '2026-10-31')
```

### 2. Consultas

| # | Consulta | Tipo exigido | Como foi feito | Resultado* |
|---|---|---|---|---|
| 9 | Clientes que nunca fizeram pedido | **Anti join (esquerda)** | `CLIENTE LEFT JOIN PEDIDO ... WHERE pe.nr_venda_mes IS NULL` | 7 clientes |
| 10 | Vendido × perdido por produto | **Full outer join** | Totais de venda e de perda por produto unidos por `LEFT JOIN` + `UNION ALL` + `RIGHT JOIN ... WHERE v.cd_produto IS NULL` | 28 produtos (9 só venda, 4 só perda, 15 com ambos) |
| 11 | Clientes com gasto acima da média | **Subconsultas** | Tabela derivada (gasto por cliente) + subconsulta escalar no `WHERE` com a média desses gastos | 12 clientes (média = R$ 101,35) |
| 12 | Pedidos acima da média da sua forma de pagamento no mês, com produto que teve perda | **Subconsultas** | Subconsulta **correlacionada** (média por forma de pagamento + mês, usa o índice 1) + `IN` aninhado (pedidos com produto presente em `PERDA`) | 10 pedidos |

\* com os dados do `inser_to.sql`.

**Sobre o FULL OUTER JOIN:** o MySQL não implementa a sintaxe `FULL OUTER JOIN`. O resultado
equivalente é obtido com a forma padrão `A LEFT JOIN B UNION ALL A RIGHT JOIN B WHERE A.chave IS NULL`:
a primeira parte traz todos os produtos vendidos (com ou sem perda) e a segunda acrescenta os
produtos que só tiveram perda. Em PostgreSQL/SQL Server/Oracle seria
`FROM vendas v FULL OUTER JOIN perdas pr ON pr.cd_produto = v.cd_produto`.

Uso no negócio de cada consulta:
- **9** — lista de clientes para campanha de primeira compra.
- **10** — produtos que perdem mais do que vendem (ex.: Pepino, Couve-flor) ou que só geram perda (Melancia, Brócolis, Alface, Cenoura) indicam compra excessiva ou armazenamento inadequado.
- **11** — melhores clientes, candidatos a programa de fidelidade.
- **12** — vendas acima do padrão envolvendo produtos sensíveis a perda, candidatas a conferência de qualidade.

### 3. Visões

#### `vw_vendas_detalhadas` — 4 joins
`PEDIDO ⋈ CLIENTE ⋈ POSSUI ⋈ PRODUTO ⟕ DEVOLUCAO` — uma linha por **item vendido**, com data, mês,
forma de pagamento, cliente (CPF/CNPJ, tipo PF/PJ, bairro), produto, quantidade, preço, subtotal e
situação da devolução.

**Justificativa semântica:** no varejo, a unidade de análise das vendas é o item vendido — qual
produto, quanto, para quem, por quanto e como foi pago. No banco essa informação está espalhada em
5 tabelas, e todo relatório de vendas (por produto, cliente, bairro, mês ou forma de pagamento)
repetiria os mesmos joins. A visão entrega essa base pronta para o gerente consultar, com o
subtotal já calculado.

#### `vw_situacao_lotes` — 4 joins
`ESTOQUE ⋈ PRODUTO ⟕ (perdas agregadas por lote) ⟕ FORNECE ⟕ FORNECEDOR` — uma linha por **lote**,
com validade, dias para vencer, situação (VENCIDO / CRÍTICO / ATENÇÃO / OK), quantidade e valor em
estoque, quantidade perdida, % de perda, descartes pendentes e fornecedores do produto.

**Justificativa semântica:** em um hortifrúti os produtos são perecíveis e o estoque é controlado
por lote. Para decidir o que vender primeiro, o que colocar em promoção e qual fornecedor acionar
quando um lote vence ou estraga, o responsável pelo estoque precisa ver tudo isso numa única linha
por lote — o que hoje exige juntar 5 tabelas. A lista de fornecedores dá a **rastreabilidade** do lote.

O script traz dois exemplos de uso para cada visão (ex.: faturamento por bairro e tipo de cliente;
resumo do estoque por situação de validade; lotes que exigem ação com os fornecedores a acionar).

---

## Etapa 05 — Funções, procedimentos e triggers

Todo o SQL desta etapa está na **Parte 6** do arquivo [`consultas.sql`](consultas.sql)
(seção *"PARTE 6 - ETAPA 05"*), organizada em:

| Seção | Conteúdo |
|---|---|
| 6.1 | Tabela de log `LOG_PRECO_PRODUTO` |
| 6.2 | Funções `fn_situacao_validade` e `fn_estoque_disponivel` |
| 6.3 | Procedimentos `sp_reajustar_precos_categoria` (atualização) e `sp_baixar_estoque_fefo` (cursor) |
| 6.4 | Triggers `trg_produto_log_preco` (log) e `trg_perda_baixa_estoque` |
| 6.5 | Testes de tudo, dentro de uma transação com `ROLLBACK` (não alteram os dados) |
| 6.6 | Comandos de remoção |

> **Como executar:** com o banco criado, execute a Parte 6 no DBeaver como script (**Alt+X**).
> Ela usa `DELIMITER $$`, necessário para criar blocos `BEGIN ... END`. Cada objeto é precedido de
> `DROP ... IF EXISTS`, então a parte pode ser executada mais de uma vez sem erro.

### Visão geral: como as peças se ligam

Os objetos não são soltos: eles trabalham juntos sobre o controle de **preço** e de **estoque**,
os dois pontos mais sensíveis de um hortifrúti.

```
sp_reajustar_precos_categoria ──altera preços──► PRODUTO ──dispara──► trg_produto_log_preco ──grava──► LOG_PRECO_PRODUTO
                                                    ▲
                     aba Produtos do app / UPDATE direto também disparam o log

sp_baixar_estoque_fefo ──consulta──► fn_estoque_disponivel
        └──percorre lotes (CURSOR) e baixa──► ESTOQUE ◄──baixa── trg_perda_baixa_estoque ◄──dispara── INSERT em PERDA

fn_situacao_validade ── regra única de validade (VENCIDO / CRÍTICO / ATENÇÃO / OK)
```

### 1. Funções

#### `fn_situacao_validade(dt_validade)` — com estrutura condicional
Retorna a situação de um lote pela validade usando **`IF / ELSEIF / ELSE`**:

| Retorno | Condição | O que a loja faz |
|---|---|---|
| `VENCIDO` | validade já passou | retira da venda / descarta |
| `CRITICO` | vence em até 7 dias | vende com prioridade |
| `ATENCAO` | vence em até 30 dias | planeja promoção |
| `OK` | mais de 30 dias | venda normal |

**Justificativa:** num hortifrúti quase tudo é perecível, e a classificação do lote pela validade é o
que decide o que vai para a frente da gôndola, o que entra em promoção e o que deve ser descartado.
Essa regra estava escrita **duas vezes** no projeto (Consulta 5 e visão `vw_situacao_lotes`). Se a
loja decidir, por exemplo, que "crítico" passa a ser 5 dias, seria preciso lembrar de alterar todos os
lugares. A função **centraliza a regra de negócio em um único ponto**.

#### `fn_estoque_disponivel(cd_produto)`
Retorna quanto de um produto **pode ser vendido**: a soma dos lotes com validade de hoje em diante.

**Justificativa:** em um hortifrúti, estoque físico e estoque vendável não são a mesma coisa. Um lote
vencido continua na tabela `ESTOQUE` até ser descartado, mas não pode ser vendido. Antes de aceitar
uma venda, o sistema precisa saber quanto está realmente disponível. A função é usada pelo
procedimento de baixa FEFO para recusar uma venda maior que o disponível, e pode ser usada em qualquer
consulta.

> As funções declaram `NO SQL` / `READS SQL DATA` e `NOT DETERMINISTIC` (dependem da data de hoje).
> O MySQL exige essas declarações quando o log binário está ativo, que é o padrão do MySQL 8.

### 2. Procedimentos

#### `sp_reajustar_precos_categoria(categoria, percentual)` — atualização de dados
Reajusta em X% o preço de **todos os produtos de uma categoria**. Exemplo:
`CALL sp_reajustar_precos_categoria(3, 10);` aplica +10% em todos os produtos da categoria 3.

Validações (com `SIGNAL`, que interrompe e devolve uma mensagem clara):
- o percentual precisa estar entre **−50% e +100%**, o que evita erros de digitação (digitar 100
  querendo 10) e preço zerado ou negativo;
- a categoria precisa existir.

**Justificativa:** no hortifrúti os preços acompanham a **sazonalidade** e mudam **por grupo de
produtos**: na entressafra das frutas tropicais todas sobem juntas; na safra, todas baixam. Alterar
produto por produto é lento e sujeito a erro. O procedimento faz o reajuste do grupo inteiro em uma
única operação, com regras de segurança, e cada preço alterado fica registrado automaticamente no log
pelo Trigger 1.

#### `sp_baixar_estoque_fefo(produto, quantidade)` — com CURSOR
Dá baixa de uma quantidade vendida seguindo a regra **FEFO** (*First Expired, First Out*): consome
primeiro o lote que vence primeiro e só passa para o próximo lote quando o anterior acaba.

1. Valida a quantidade (> 0).
2. Usa `fn_estoque_disponivel`: se não houver estoque vendável suficiente, **recusa e não altera nada**.
3. O **cursor** percorre os lotes válidos do produto em ordem de validade. Para cada lote:
   `retirar = MIN(quanto ainda falta, quanto tem no lote)`, atualiza o lote, registra a retirada e
   desconta do que falta. **Para** assim que o pedido é atendido.
4. Devolve uma tabela mostrando quanto saiu de cada lote.

Exemplo do teste (3 lotes de Maçã Gala com 10, 20 e 30 kg; baixa de 25 kg):

| Lote | Vence em | Antes | Retirado | Depois |
|---|---|---|---|---|
| 2 | 5 dias | 10 | **10** | 0 |
| 3 | 15 dias | 20 | **15** | 5 |
| 4 | 40 dias | 30 | — (não é tocado) | 30 |

**Por que precisa de cursor (não dá para fazer com um `UPDATE`):**
- **A quantidade retirada de cada lote depende do resultado do lote anterior.** O saldo que "falta" é
  um estado que passa de uma linha para a próxima. Um `UPDATE` avalia a mesma expressão para cada linha
  de forma independente e não carrega esse saldo entre as linhas.
- **A ordem importa e o processamento para no meio:** os lotes são consumidos por validade crescente, e
  o processo termina quando o pedido é atendido. Um `UPDATE` não tem "pare quando atingir o total".
- **Para cada lote são feitas duas ações** com valores calculados naquele passo: atualizar o lote e
  registrar a retirada.

**Justificativa:** produtos perecíveis precisam sair na ordem de validade. Se a baixa for feita no
lote errado (o mais novo), o lote mais antigo vence na prateleira e vira **perda**. A regra FEFO é a
prática padrão do varejo de alimentos para reduzir desperdício.

> O procedimento não abre nem fecha transação: quem chama decide entre `COMMIT` e `ROLLBACK`. A
> validação do passo 2 garante que a baixa nunca fica pela metade.

### 3. Triggers

#### `trg_produto_log_preco` — `AFTER UPDATE` em `PRODUTO` (atualiza a tabela de log)
Sempre que o **preço** de um produto muda, grava uma linha em `LOG_PRECO_PRODUTO` com preço antigo,
preço novo, variação %, usuário e data/hora. Mudanças que não alteram o preço (nome, descrição, marca)
**não** geram log.

**Justificativa:** o preço é o dado mais sensível do varejo. A loja precisa responder *"quem mudou o
preço do tomate, quando e de quanto?"* para conferir reajustes, investigar erros de etiqueta e atender
reclamações de clientes, já que o preço cobrado deve ser o anunciado. Por ser um trigger, **o registro
é garantido em qualquer caminho de alteração**: pelo procedimento de reajuste, pela aba *Produtos* da
aplicação Java ou por um `UPDATE` direto no DBeaver. Ninguém precisa "lembrar" de gravar o log.

Decisões técnicas:
- **`AFTER`**: o log só deve existir se a alteração realmente aconteceu, ou seja, se passou nas
  restrições `CHECK` da tabela.
- **`USER()` e não `CURRENT_USER()`**: dentro de um trigger, `CURRENT_USER()` devolve o dono do
  trigger, enquanto `USER()` devolve quem está conectado e fez a alteração.
- **A tabela de log não tem chave estrangeira para `PRODUTO`**: um log de auditoria precisa sobreviver
  mesmo que o produto seja excluído depois. Por isso ela guarda também uma cópia do nome do produto.

#### `trg_perda_baixa_estoque` — `BEFORE INSERT` em `PERDA`
Ao registrar uma perda (produto estragado, amassado, vencido...):
1. **bloqueia** o registro se o lote não existir ou se a quantidade perdida for **maior que a
   quantidade do lote**;
2. **desconta** a quantidade perdida do lote no `ESTOQUE`.

**Justificativa:** perda é saída de estoque. Sem o trigger, é possível registrar uma perda e o
`ESTOQUE` continuar mostrando a mercadoria que já foi para o lixo. O sistema passa a ter
**"estoque fantasma"**, que gera vendas que não podem ser entregues e compras planejadas errado.
Também não faz sentido perder mais do que existia no lote. O trigger mantém `PERDA` e `ESTOQUE`
sempre coerentes, automaticamente.

Decisão técnica: **`BEFORE`** porque a verificação precisa acontecer antes de a perda ser gravada,
para poder recusá-la. O `SIGNAL` cancela o `INSERT` inteiro, inclusive a baixa no estoque.

### 4. Testes

A seção 6.5 do script testa tudo dentro de `START TRANSACTION ... ROLLBACK`, então nada fica gravado.
Como todos os lotes dos dados de exemplo já estão vencidos, o teste primeiro cria três lotes novos
(com validade futura) para a Maçã Gala. Resultados verificados:

| Objeto | Teste | Resultado |
|---|---|---|
| `fn_situacao_validade` | datas de ontem, +3, +20 e +60 dias | VENCIDO, CRITICO, ATENCAO, OK |
| `fn_estoque_disponivel` | Maçã Gala com 1 lote vencido + 3 válidos (10+20+30) | 60 (o vencido não conta) |
| `sp_baixar_estoque_fefo` | baixa de 25 kg | lote 2: 10→0, lote 3: 20→5, lote 4 intacto; disponível passa a 35 |
| `sp_reajustar_precos_categoria` | +10% na categoria 3 | 3 produtos reajustados |
| `trg_produto_log_preco` | reajuste + `UPDATE` direto de preço + `UPDATE` só da descrição | 4 linhas de log; a mudança de descrição não gera log |
| `trg_perda_baixa_estoque` | perda de 2 kg no lote 3 | estoque do lote: 5→3 |

Casos de erro (comentados no script, com a mensagem esperada):

| Comando | Mensagem |
|---|---|
| `CALL sp_reajustar_precos_categoria(3, 150)` | Percentual invalido (150.00%). Use um valor entre -50 e 100. |
| `CALL sp_reajustar_precos_categoria(999, 10)` | Categoria 999 nao existe. |
| `CALL sp_baixar_estoque_fefo(2, 1000)` | Estoque insuficiente para o produto 2: disponivel 0.000, solicitado 1000.000. |
| perda de 9999 no lote 1 do produto 1 | Perda de 9999.000 maior que o estoque do lote (205.360). |

Na aplicação Java, essas mensagens de regra de negócio (`SIGNAL`, código 1644) aparecem exatamente como
escritas acima. Depois que a Parte 6 é executada no banco, qualquer alteração de preço feita pela aba
**Produtos** passa a ser registrada no log automaticamente.

---

## Entrega final

Versão definitiva da aplicação, integrada ao banco, consolidando as funcionalidades de todas as etapas.
O SQL novo que a interface usa está documentado na **Parte 7** do [`consultas.sql`](consultas.sql)
(seção *"PARTE 7 - ENTREGA FINAL"*).

### O que rodar no banco

| Ordem | Script | Quando |
|---|---|---|
| 1 | `modelo_fisico.sql` | só se o banco `hortifruti` ainda não existir |
| 2 | `inser_to.sql` | só se o banco acabou de ser criado |
| 3 | `consultas.sql` **Parte 5** (Entrega 4) | se as visões e os índices ainda não existirem |
| 4 | `consultas.sql` **Parte 6** (Etapa 05) | se as funções, procedimentos e triggers ainda não existirem |
| — | `consultas.sql` **Parte 7** | opcional: não cria nada, só documenta e confere o SQL usado pela interface |

> Atalho: num banco recém-criado, basta executar o `consultas.sql` inteiro (Partes 1 a 7) no DBeaver
> com **Alt+X**. Todos os testes que alteram dados rodam dentro de transação com `ROLLBACK`.

### Atendimento aos requisitos

| Requisito | Como foi atendido |
|---|---|
| **CRUD em ao menos 4 tabelas** | **CLIENTE, PRODUTO, FUNCIONARIO e ESTOQUE** — inserir, alterar, excluir e buscar, cada um na sua aba |
| **Interface executa/visualiza funções, procedimentos e triggers** | Aba **Rotinas do banco** executa os 2 procedimentos e dispara os 2 triggers, mostrando o efeito; a aba **Estoque** exibe o resultado das 2 funções em cada lote |
| **Consultas e views acessíveis, com filtros** | Aba **Consultas**: as 14 consultas/visões das Etapas 03 e 04, com o SQL exibido e **filtros** (período, forma de pagamento, situação, tipo de cliente, produto, bairro, mínimo de pedidos) |
| **Apresentação visual clara de cada consulta** | Cada resultado vem com **faixa de indicadores** (linhas, soma, média, mínimo, máximo, grupos distintos) e **gráfico de barras** configurável (agrupar por / valor) |
| **Dashboard com indicadores** | 6 cards: pedidos, faturamento, ticket médio, clientes ativos (**% da base**), **% de pedidos com devolução**, prejuízo com perdas (**% do faturamento**) |
| **Estatística (média, mediana, moda, variância, desvio padrão)** | Card **Estatística descritiva** com n, média, mediana, moda, variância, desvio padrão, coeficiente de variação, mínimo e máximo, para 3 variáveis à escolha |
| **Ao menos 6 gráficos dinâmicos** | 6 gráficos no Dashboard (colunas com linha de tendência, 2 roscas, 3 barras) + gráfico de dispersão com Pearson (aba Correlação) + gráfico de cada consulta |
| **Visualizações interativas com filtros** | Barra de filtros do Dashboard (**período, categoria e forma de pagamento**) aplicada a **todos** os cards, à estatística e aos 6 gráficos |
| **Tudo sobre os dados do banco** | Todos os números vêm de consultas SQL sobre o banco `hortifruti` (a aba de imagens de Estatística é complementar) |

### 1. CRUD da 4ª tabela: ESTOQUE (aba "Estoque")

- **Inserir lote:** produto, nº do lote (o sistema sugere o próximo número livre), quantidade, data de
  entrada e validade (a validade precisa ser posterior à entrada).
- **Alterar lote:** quantidade e datas. Produto e lote formam a **chave primária composta** e por isso
  ficam bloqueados na edição.
- **Excluir lote:** o banco impede excluir um lote que tem perdas registradas (chave estrangeira de
  `PERDA`), e a tela mostra uma mensagem clara.
- **Integração com a Etapa 05:** a listagem mostra, para cada lote, a **situação** calculada por
  `fn_situacao_validade` (VENCIDO / CRITICO / ATENCAO / OK) e o **estoque disponível para venda** do
  produto calculado por `fn_estoque_disponivel`.

**Por que ESTOQUE:** é a tabela central da operação de um hortifrúti (produtos perecíveis controlados
por lote) e a que as rotinas da Etapa 05 manipulam. Ter o CRUD dela permite, por exemplo, cadastrar
um lote novo e em seguida dar baixa FEFO ou registrar uma perda nele.

Arquivos: `model/Estoque.java`, `dao/EstoqueDAO.java`, `view/EstoquePanel.java`. Para suportar a chave
composta, o `CrudPanel` ganhou dois pontos de extensão (`chaveDaLinha` e `rotuloRegistro`).

### 2. Integração com funções, procedimentos e triggers (aba "Rotinas do banco")

| Card | O que executa | O que mostra |
|---|---|---|
| **Reajuste de preços por categoria** | `{CALL sp_reajustar_precos_categoria(?, ?)}` | quantos produtos foram reajustados e, na aba *Histórico de preços*, as linhas gravadas em `LOG_PRECO_PRODUTO` pelo **trigger** `trg_produto_log_preco` |
| **Baixa de estoque (FEFO)** | `{CALL sp_baixar_estoque_fefo(?, ?)}` — procedimento com **cursor** | o estoque vendável do produto (**função** `fn_estoque_disponivel`) e uma tabela com quanto saiu de cada lote, do que vence primeiro ao último |
| **Registrar perda** | `INSERT INTO PERDA ...` | o estoque do lote **antes e depois**, mostrando o desconto feito pelo **trigger** `trg_perda_baixa_estoque` |

- Os procedimentos são chamados com `CallableStatement` dentro de uma **transação** (tudo ou nada).
- As mensagens de regra de negócio (`SIGNAL`), como *"Estoque insuficiente..."* ou *"Perda maior que o
  estoque do lote"*, aparecem na tela exatamente como escritas no banco.
- Toda alteração pede confirmação, porque é gravada no banco.
- O trigger de log também registra as mudanças de preço feitas pela aba **Produtos**.

Arquivos: `dao/RotinasDAO.java`, `view/RotinasPanel.java`.

### 3. Consultas e visões com filtros e apresentação visual (aba "Consultas")

- Os filtros são montados automaticamente a partir da definição de cada consulta: **listas de seleção**
  para valores conhecidos, **campos de data** e **campos de texto**. Cada filtro preenche um `?` do SQL
  via `PreparedStatement`.

| Consulta | Filtros |
|---|---|
| 2. Ranking de clientes | mínimo de pedidos |
| 5. Lotes vencidos / a vencer | janela de dias |
| 6. Conferência de pedidos | forma de pagamento |
| 9. Clientes que nunca compraram (anti join) | bairro, tipo de cliente (PF/PJ) |
| 10. Vendido × perdido (full outer join) | situação (só venda, só perda, perde mais do que vende...) |
| 11. Clientes acima da média (subconsultas) | mínimo de pedidos |
| 12. Pedidos acima da média (subconsultas) | forma de pagamento |
| 13. Visão `vw_vendas_detalhadas` | período (de/até), forma de pagamento, produto |
| 14. Visão `vw_situacao_lotes` | situação do lote, produto |

- **Indicadores do resultado:** número de linhas e, para a coluna numérica escolhida, soma, média,
  mínimo, máximo e quantidade de grupos distintos.
- **Gráfico do resultado:** barras somando a coluna "Valor" por grupo da coluna "Agrupar por" (até os
  15 maiores grupos). O sistema escolhe colunas sensatas por padrão, priorizando valores em R$, e o
  usuário pode trocar. Exemplo: na visão de vendas, *subtotal por mês* ou *subtotal por produto*.

Arquivos: `model/Consulta.java` (agora com vários parâmetros), `dao/ConsultaDAO.java`,
`dao/Consultas.java`, `view/ConsultasPanel.java`.

### 4. Dashboard estatístico com filtros (aba "Dashboard")

**Filtros** (barra no topo): **período** (de/até; o padrão vai da primeira à última data com movimento
no banco), **categoria** e **forma de pagamento**. Clicar em **Aplicar filtros** recalcula tudo; o
**Limpar** volta ao padrão. Uma linha abaixo resume o filtro em uso.

> As medidas de venda são calculadas a partir dos **itens vendidos** (`POSSUI × PRODUTO`). Assim, ao
> filtrar uma categoria, entram só os itens daquela categoria. Sem filtro, o total é igual à soma do
> `vl_total` dos pedidos. As **perdas** usam a data da perda e não dependem da forma de pagamento.

**Indicadores (6 cards):**

| Card | Cálculo |
|---|---|
| Pedidos | pedidos distintos no filtro |
| Faturamento | soma de quantidade × preço dos itens |
| Ticket médio | faturamento ÷ pedidos |
| Clientes ativos | clientes com pedido no filtro, e o **%** sobre o total de clientes |
| Pedidos c/ devolução | **%** dos pedidos do filtro que têm devolução |
| Prejuízo c/ perdas | quantidade perdida × preço, e o **%** sobre o faturamento |

**Estatística descritiva** (inspirada na disciplina de Estatística), com escolha da variável:
*valor do pedido (R$)*, *itens por pedido* ou *quantidade vendida por item*.

| Medida | Como é calculada |
|---|---|
| Média | soma ÷ n |
| Mediana | valor central da amostra ordenada (média dos dois centrais se n for par) |
| Moda | valor mais frequente; se nenhum valor se repete (variável contínua, como R$), a **classe modal** de uma distribuição de frequências com *k = 1 + 3,3·log₁₀(n)* classes (**regra de Sturges**) |
| Variância | **amostral**: Σ(x − x̄)² ÷ (n − 1) |
| Desvio padrão | raiz quadrada da variância |
| Coef. de variação | desvio ÷ média × 100 (dispersão relativa) |
| Mínimo / máximo | extremos da amostra |

Os valores foram conferidos com o cálculo feito direto no SQL (`AVG`, `VAR_SAMP`, `STDDEV_SAMP` e
mediana com `ROW_NUMBER()`), na seção 7.4 do `consultas.sql`. Sem filtro: n = 40, média = R$ 58,28,
mediana = R$ 44,86, variância = 2.148,54, desvio = R$ 46,35, CV = 79,5%.

**6 gráficos dinâmicos**, todos respeitando os filtros:
1. Faturamento mensal (colunas + linha de tendência)
2. Faturamento por forma de pagamento (rosca, com %)
3. Top 10 produtos por quantidade vendida (barras)
4. Faturamento por categoria (barras)
5. Quantidade perdida por motivo (barras)
6. Devoluções por forma de resolução (rosca, com %)

A aba **Correlação** complementa o dashboard com o gráfico de dispersão, o coeficiente de Pearson e a
reta de regressão.

Arquivos: `dao/DashboardDAO.java` (reescrito com `Filtro`), `dao/ReferenciaDAO.java` (listas e período
padrão), `util/Estatistica.java` (estatística descritiva), `view/DashboardPanel.java`.

### 5. Testes realizados

Tudo foi testado num banco MySQL temporário (`modelo_fisico.sql` + `inser_to.sql` + `consultas.sql`
completo), com log binário ativo, como no MySQL padrão:

- `consultas.sql` inteiro (Partes 1 a 7) executa sem erro e não deixa dados alterados.
- As 14 consultas executam pela aplicação com os filtros padrão e com filtros específicos.
  Exemplos: visão de vendas em agosto/Pix → 1 item; consulta 10 "só perda" → 4 produtos.
- Dashboard: os totais sem filtro batem com as consultas anteriores (40 pedidos, R$ 2.331,05). Com
  filtro de categoria e de período/forma de pagamento, todos os cards e gráficos são recalculados.
- CRUD de Estoque: inserir, alterar e excluir lotes; exclusão de lote com perda é bloqueada (erro 1451).
- Rotinas: baixa FEFO de 50 sobre lotes de 40 e 35 → 40 saem do primeiro e 10 do segundo; baixa acima
  do disponível é recusada; reajuste de +5% gera 3 linhas de log; perda de 1,5 desconta o lote
  (25 → 23,5); perda maior que o lote é recusada.

### Estrutura atualizada (arquivos novos desta entrega)

```
app/src/
  model/Estoque.java            lote de estoque (chave composta)
  model/ItemLista.java          item de lista de seleção (código + nome)
  dao/EstoqueDAO.java           CRUD da tabela ESTOQUE (usa as 2 funções na listagem)
  dao/RotinasDAO.java           CALL dos procedimentos, perda (trigger), log de preços
  dao/ReferenciaDAO.java        listas de produtos, categorias, lotes e período dos dados
  view/EstoquePanel.java        aba Estoque
  view/RotinasPanel.java        aba Rotinas do banco
  (alterados) DashboardDAO, DashboardPanel, Consulta, ConsultaDAO, Consultas, ConsultasPanel,
              CrudPanel, Estatistica, UI, MainFrame, chart/Grafico
```
