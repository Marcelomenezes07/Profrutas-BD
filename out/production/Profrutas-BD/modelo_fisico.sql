-- =========================================================
-- Sistema Administrativo para Empresa de Varejo do Ramo Hortifruti
-- Etapa 02 - Criacao de Tabelas (com constraints) e Insercao de Dados
-- =========================================================

CREATE DATABASE hortifruti;
USE hortifruti;


-- =========================================================
-- CLIENTE
-- =========================================================
CREATE TABLE CLIENTE (
    nr_cliente      INT AUTO_INCREMENT PRIMARY KEY,
    cpf_cnpj        VARCHAR(18) NOT NULL,
    telefone        VARCHAR(15),
    telefone2       VARCHAR(15),
    cep             VARCHAR(9)  NOT NULL,
    rua             VARCHAR(100) NOT NULL,
    bairro          VARCHAR(60)  NOT NULL,
    numero          VARCHAR(10)  NOT NULL,
    apartamento     VARCHAR(10),
    CONSTRAINT uq_cliente_cpf_cnpj UNIQUE (cpf_cnpj),
    CONSTRAINT ck_cliente_cpf_cnpj_tam CHECK (CHAR_LENGTH(cpf_cnpj) BETWEEN 11 AND 18)
);

-- =========================================================
-- FUNCIONARIO (autorrelacionamento supervisiona/supervisionado)
-- Demonstra ON DELETE SET NULL: se o supervisor for excluido,
-- o funcionario subordinado apenas fica sem supervisor.
-- =========================================================
CREATE TABLE FUNCIONARIO (
    nr_funcionario              INT AUTO_INCREMENT PRIMARY KEY,
    nome                        VARCHAR(100) NOT NULL,
    cpf                         CHAR(11) NOT NULL,
    telefone                    VARCHAR(15),
    cep                         VARCHAR(9)   NOT NULL,
    rua                         VARCHAR(100) NOT NULL,
    bairro                      VARCHAR(60)  NOT NULL,
    numero                      VARCHAR(10)  NOT NULL,
    apartamento                 VARCHAR(10),
    nr_funcionario_supervisor   INT,
    CONSTRAINT uq_funcionario_cpf UNIQUE (cpf),
    CONSTRAINT ck_funcionario_cpf_tam CHECK (CHAR_LENGTH(cpf) = 11),
    FOREIGN KEY (nr_funcionario_supervisor)
        REFERENCES FUNCIONARIO (nr_funcionario)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- =========================================================
-- FORNECEDOR
-- =========================================================
CREATE TABLE FORNECEDOR (
    cd_fornecedor    INT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(100) NOT NULL,
    cnpj_fornecedor  CHAR(14) NOT NULL,
    telefone         VARCHAR(15),
    telefone2        VARCHAR(15),
    cep              VARCHAR(9)   NOT NULL,
    rua              VARCHAR(100) NOT NULL,
    bairro           VARCHAR(60)  NOT NULL,
    numero           VARCHAR(10)  NOT NULL,
    CONSTRAINT uq_fornecedor_cnpj UNIQUE (cnpj_fornecedor),
    CONSTRAINT ck_fornecedor_cnpj_tam CHECK (CHAR_LENGTH(cnpj_fornecedor) = 14)
);

-- =========================================================
-- PRODUTO
-- =========================================================
CREATE TABLE PRODUTO (
    cd_produto               INT AUTO_INCREMENT PRIMARY KEY,
    nome                     VARCHAR(100) NOT NULL,
    descricao                VARCHAR(255),
    valor_kg_ou_unitario     DECIMAL(10,2) NOT NULL,
    vendido_unitario         BOOLEAN NOT NULL DEFAULT FALSE,
    marca                    VARCHAR(60),
    CONSTRAINT ck_produto_valor_positivo CHECK (valor_kg_ou_unitario > 0)
);

-- =========================================================
-- CATEGORIA
-- =========================================================
CREATE TABLE CATEGORIA (
    cd_categoria    INT AUTO_INCREMENT PRIMARY KEY,
    nm_categoria    VARCHAR(60) NOT NULL,
    CONSTRAINT uq_categoria_nome UNIQUE (nm_categoria)
);

-- =========================================================
-- ESTOQUE (controle por lote)
-- Demonstra ON UPDATE CASCADE: se cd_produto de PRODUTO mudar,
-- o lote em ESTOQUE acompanha automaticamente.
-- =========================================================
CREATE TABLE ESTOQUE (
    cd_produto    INT NOT NULL,
    nr_lote       INT NOT NULL,
    quantidade    DECIMAL(10,3) NOT NULL,
    dt_validade   DATE NOT NULL,
    dt_entrada    DATE NOT NULL DEFAULT (CURRENT_DATE),
    PRIMARY KEY (cd_produto, nr_lote),
    CONSTRAINT ck_estoque_quantidade CHECK (quantidade >= 0),
    CONSTRAINT ck_estoque_datas CHECK (dt_validade > dt_entrada),
    FOREIGN KEY (cd_produto)
        REFERENCES PRODUTO (cd_produto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- PEDIDO
-- =========================================================
CREATE TABLE PEDIDO (
    nr_venda_mes      INT AUTO_INCREMENT PRIMARY KEY,
    nr_cliente        INT NOT NULL,
    dt_venda          DATE NOT NULL,
    forma_pagamento   VARCHAR(30) NOT NULL DEFAULT 'Dinheiro',
    vl_total          DECIMAL(10,2) NOT NULL,
    CONSTRAINT ck_pedido_forma_pagamento CHECK (forma_pagamento IN
        ('Dinheiro','Cartao de Credito','Cartao de Debito','Pix','Boleto')),
    CONSTRAINT ck_pedido_valor_total CHECK (vl_total >= 0),
    FOREIGN KEY (nr_cliente)
        REFERENCES CLIENTE (nr_cliente)
);

-- =========================================================
-- DEVOLUCAO (relacao 1:1 opcional com PEDIDO)
-- =========================================================
CREATE TABLE DEVOLUCAO (
    n_devolucao           INT AUTO_INCREMENT PRIMARY KEY,
    nr_venda_mes          INT NOT NULL UNIQUE,
    dt_devolucao          DATE NOT NULL,
    forma_resolucao       VARCHAR(50),
    motivo                VARCHAR(255),
    devolucao_efetivada   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_devolucao_resolucao CHECK (forma_resolucao IN
        ('Troca','Reembolso','Credito','Reparo') OR forma_resolucao IS NULL),
    FOREIGN KEY (nr_venda_mes)
        REFERENCES PEDIDO (nr_venda_mes)
);

-- =========================================================
-- PERDA (referencia o lote em ESTOQUE)
-- =========================================================
CREATE TABLE PERDA (
    nr_perda             INT AUTO_INCREMENT PRIMARY KEY,
    cd_produto           INT NOT NULL,
    nr_lote              INT NOT NULL,
    dt_perda             DATE NOT NULL,
    qt_perdida           DECIMAL(10,3) NOT NULL,
    motivo_perda         VARCHAR(255),
    descarte_efetivado   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_perda_quantidade CHECK (qt_perdida > 0),
    FOREIGN KEY (cd_produto, nr_lote)
        REFERENCES ESTOQUE (cd_produto, nr_lote)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- FORNECE (N:N entre FORNECEDOR e PRODUTO)
-- =========================================================
CREATE TABLE FORNECE (
    cd_fornecedor     INT NOT NULL,
    cd_produto        INT NOT NULL,
    modo_transporte   VARCHAR(50) DEFAULT 'Rodoviario',
    PRIMARY KEY (cd_fornecedor, cd_produto),
    CONSTRAINT ck_fornece_modo CHECK (modo_transporte IN
        ('Rodoviario','Ferroviario','Aereo','Maritimo') OR modo_transporte IS NULL),
    FOREIGN KEY (cd_fornecedor)
        REFERENCES FORNECEDOR (cd_fornecedor),
    FOREIGN KEY (cd_produto)
        REFERENCES PRODUTO (cd_produto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- TEM (N:N entre CATEGORIA e PRODUTO)
-- =========================================================
CREATE TABLE TEM (
    cd_categoria    INT NOT NULL,
    cd_produto      INT NOT NULL,
    PRIMARY KEY (cd_categoria, cd_produto),
    FOREIGN KEY (cd_categoria)
        REFERENCES CATEGORIA (cd_categoria),
    FOREIGN KEY (cd_produto)
        REFERENCES PRODUTO (cd_produto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =========================================================
-- POSSUI (N:N entre PEDIDO e PRODUTO - itens do pedido)
-- =========================================================
CREATE TABLE POSSUI (
    cd_produto      INT NOT NULL,
    nr_venda_mes    INT NOT NULL,
    quantidade      DECIMAL(10,3) NOT NULL,
    PRIMARY KEY (cd_produto, nr_venda_mes),
    CONSTRAINT ck_possui_quantidade CHECK (quantidade > 0),
    FOREIGN KEY (cd_produto)
        REFERENCES PRODUTO (cd_produto)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    FOREIGN KEY (nr_venda_mes)
        REFERENCES PEDIDO (nr_venda_mes)
);

