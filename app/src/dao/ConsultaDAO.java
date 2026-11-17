package dao;

import model.Consulta;
import model.ResultadoConsulta;
import util.ConnectionFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Executa as consultas pre-definidas (Consultas.TODAS) e devolve colunas + linhas. */
public class ConsultaDAO {
    /**
     * @param valores um valor (texto) para cada parametro da consulta, na ordem dos "?".
     *                Datas no formato yyyy-MM-dd.
     */
    public ResultadoConsulta executar(Consulta consulta, List<String> valores) throws SQLException {
        long inicio = System.currentTimeMillis();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(consulta.getSql())) {
            List<Consulta.Parametro> parametros = consulta.getParametros();
            for (int i = 0; i < parametros.size(); i++) {
                String valor = valores.get(i);
                switch (parametros.get(i).tipo()) {
                    case INTEIRO -> ps.setInt(i + 1, Integer.parseInt(valor.trim()));
                    case DATA -> ps.setDate(i + 1, Date.valueOf(LocalDate.parse(valor.trim())));
                    case TEXTO -> ps.setString(i + 1, valor);
                    case NENHUM -> { }
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<String> colunas = new ArrayList<>();
                for (int i = 1; i <= n; i++) colunas.add(md.getColumnLabel(i));
                List<Object[]> linhas = new ArrayList<>();
                while (rs.next()) {
                    Object[] linha = new Object[n];
                    for (int i = 1; i <= n; i++) linha[i - 1] = rs.getObject(i);
                    linhas.add(linha);
                }
                return new ResultadoConsulta(colunas, linhas, System.currentTimeMillis() - inicio);
            }
        }
    }
}
