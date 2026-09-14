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
