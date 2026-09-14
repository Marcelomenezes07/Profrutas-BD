-- =========================================================
-- Sistema Administrativo para Empresa de Varejo do Ramo Hortifrúti
-- Script de criação do banco de dados (com tipos de dados corrigidos)
-- =========================================================

CREATE DATABASE IF NOT EXISTS hortifruti;
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
    apartamento     VARCHAR(10)
);

-- =========================================================
-- FUNCIONARIO (autorrelacionamento supervisiona/supervisionado)
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
    FOREIGN KEY (nr_funcionario_supervisor)
        REFERENCES FUNCIONARIO (nr_funcionario)
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
    numero           VARCHAR(10)  NOT NULL
);

-- =========================================================
-- PRODUTO
-- =========================================================
CREATE TABLE PRODUTO (
    cd_produto               INT AUTO_INCREMENT PRIMARY KEY,
    nome                     VARCHAR(100) NOT NULL,
    descricao                VARCHAR(255),
    valor_kg_ou_unitario     DECIMAL(10,2) NOT NULL,
    vendido_unitario         BOOLEAN NOT NULL,
    marca                    VARCHAR(60)
);

-- =========================================================
-- CATEGORIA
-- =========================================================
CREATE TABLE CATEGORIA (
    cd_categoria    INT AUTO_INCREMENT PRIMARY KEY,
    nm_categoria    VARCHAR(60) NOT NULL
);

-- =========================================================
-- ESTOQUE (controle por lote)
-- =========================================================
CREATE TABLE ESTOQUE (
    cd_produto    INT NOT NULL,
    nr_lote       INT NOT NULL,
    quantidade    DECIMAL(10,3) NOT NULL,
    dt_validade   DATE NOT NULL,
    dt_entrada    DATE NOT NULL,
    PRIMARY KEY (cd_produto, nr_lote),
    FOREIGN KEY (cd_produto)
        REFERENCES PRODUTO (cd_produto)
);

-- =========================================================
-- PEDIDO
-- =========================================================
CREATE TABLE PEDIDO (
    nr_venda_mes      INT AUTO_INCREMENT PRIMARY KEY,
    nr_cliente        INT NOT NULL,
    dt_venda          DATE NOT NULL,
    forma_pagamento   VARCHAR(30) NOT NULL,
    vl_total          DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (nr_cliente)
        REFERENCES CLIENTE (nr_cliente)
);

-- =========================================================
-- DEVOLUCAO (relação 1:1 opcional com PEDIDO)
-- =========================================================
CREATE TABLE DEVOLUCAO (
    n_devolucao           INT AUTO_INCREMENT PRIMARY KEY,
    nr_venda_mes          INT NOT NULL UNIQUE,
    dt_devolucao          DATE NOT NULL,
    forma_resolucao       VARCHAR(50),
    motivo                VARCHAR(255),
    devolucao_efetivada   BOOLEAN NOT NULL DEFAULT FALSE,
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
    FOREIGN KEY (cd_produto, nr_lote)
        REFERENCES ESTOQUE (cd_produto, nr_lote)
);

-- =========================================================
-- FORNECE (N:N entre FORNECEDOR e PRODUTO)
-- =========================================================
CREATE TABLE FORNECE (
    cd_fornecedor     INT NOT NULL,
    cd_produto        INT NOT NULL,
    modo_transporte   VARCHAR(50),
    PRIMARY KEY (cd_fornecedor, cd_produto),
    FOREIGN KEY (cd_fornecedor)
        REFERENCES FORNECEDOR (cd_fornecedor),
    FOREIGN KEY (cd_produto)
        REFERENCES PRODUTO (cd_produto)
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
);

-- =========================================================
-- POSSUI (N:N entre PEDIDO e PRODUTO - itens do pedido)
-- =========================================================
CREATE TABLE POSSUI (
    cd_produto      INT NOT NULL,
    nr_venda_mes    INT NOT NULL,
    quantidade      DECIMAL(10,3) NOT NULL,
    PRIMARY KEY (cd_produto, nr_venda_mes),
    FOREIGN KEY (cd_produto)
        REFERENCES PRODUTO (cd_produto),
    FOREIGN KEY (nr_venda_mes)
        REFERENCES PEDIDO (nr_venda_mes)
);
