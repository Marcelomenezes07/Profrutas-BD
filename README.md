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
