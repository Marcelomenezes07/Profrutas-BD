CREATE DATABASE IF NOT EXISTS hortifruti;
USE hortifruti;

CREATE TABLE `CLIENTE` (
    `nr_cliente` INT PRIMARY KEY,
    `telefone2` INT,
    `cpf/cnpj` INT,
    `telefone` INT,
    `cep` INT,
    `rua` INT,
    `bairro` INT,
    `numero` INT,
    `apartamento` INT
);

CREATE TABLE `Funcionario` (
    `nr_funcionario` INT PRIMARY KEY,
    `nome` INT,
    `cpf` INT,
    `telefone` INT,
    `cep` INT,
    `rua` INT,
    `bairro` INT,
    `numero` INT,
    `apartamento` INT,
    `nr_funcionario_supervisor` INT,
    FOREIGN KEY (`nr_funcionario_supervisor`)
        REFERENCES `Funcionario` (`nr_funcionario`)
);

CREATE TABLE `FORNECEDOR` (
    `cd_fornecedor` INT PRIMARY KEY,
    `nome` INT,
    `cnpj_fornecedor` INT,
    `telefone2` INT,
    `telefone` INT,
    `cep` INT,
    `rua` INT,
    `bairro` INT,
    `numero` INT
);

CREATE TABLE `PRODUTO` (
    `cd_produto` INT PRIMARY KEY,
    `nome` INT,
    `descrição` INT,
    `valor/kg ou valor unitario` INT,
    `Vendido unitario(bool)` INT,
    `marca` INT
);

CREATE TABLE `categoria` (
    `cd_categoria` INT PRIMARY KEY,
    `nm_categoria` INT
);

CREATE TABLE `Estoque` (
    `cd_produto` INT,
    `nr_lote` INT,
    `quantidade` INT,
    `dt_validade` INT,
    `dt_entrada` INT,
    PRIMARY KEY (`cd_produto`, `nr_lote`),
    FOREIGN KEY (`cd_produto`)
        REFERENCES `PRODUTO` (`cd_produto`)
);

CREATE TABLE `pedido` (
    `nr_venda_mes` INT PRIMARY KEY,
    `nr_cliente` INT NOT NULL,
    `dt_venda` INT,
    `forma_pagamento` INT,
    `vl_total` INT,
    FOREIGN KEY (`nr_cliente`)
        REFERENCES `CLIENTE` (`nr_cliente`)
);

CREATE TABLE `Devolucao` (
    `n_devolucao` INT PRIMARY KEY,
    `nr_venda_mes` INT NOT NULL UNIQUE,
    `dt_devolucao` INT,
    `forma_resolucao` INT,
    `motivo` INT,
    `devolucao_efeticada` INT,
    FOREIGN KEY (`nr_venda_mes`)
        REFERENCES `pedido` (`nr_venda_mes`)
);

CREATE TABLE `Perda` (
    `nr_perda` INT PRIMARY KEY,
    `cd_produto` INT NOT NULL,
    `nr_lote` INT NOT NULL,
    `dt_perda` INT,
    `qt_perdida` INT,
    `motivo_perda` INT,
    `descarte_efetivado` INT,
    FOREIGN KEY (`cd_produto`, `nr_lote`)
        REFERENCES `Estoque` (`cd_produto`, `nr_lote`)
);

CREATE TABLE `fornece` (
    `cd_fornecedor` INT,
    `cd_produto` INT,
    `Modo de transporte` INT,
    PRIMARY KEY (`cd_fornecedor`, `cd_produto`),
    FOREIGN KEY (`cd_fornecedor`)
        REFERENCES `FORNECEDOR` (`cd_fornecedor`),
    FOREIGN KEY (`cd_produto`)
        REFERENCES `PRODUTO` (`cd_produto`)
);

CREATE TABLE `tem` (
    `cd_categoria` INT,
    `cd_produto` INT,
    PRIMARY KEY (`cd_categoria`, `cd_produto`),
    FOREIGN KEY (`cd_categoria`)
        REFERENCES `categoria` (`cd_categoria`),
    FOREIGN KEY (`cd_produto`)
        REFERENCES `PRODUTO` (`cd_produto`)
);

CREATE TABLE `possui` (
    `cd_produto` INT,
    `nr_venda_mes` INT,
    `quantidade` INT,
    PRIMARY KEY (`cd_produto`, `nr_venda_mes`),
    FOREIGN KEY (`cd_produto`)
        REFERENCES `PRODUTO` (`cd_produto`),
    FOREIGN KEY (`nr_venda_mes`)
        REFERENCES `pedido` (`nr_venda_mes`)
);
