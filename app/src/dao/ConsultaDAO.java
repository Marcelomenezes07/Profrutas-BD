package dao;

import model.Consulta;
import model.ResultadoConsulta;
import util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Executa as consultas pre-definidas (Consultas.TODAS) e devolve colunas + linhas. */
public class ConsultaDAO {
    public ResultadoConsulta executar(Consulta consulta, String parametro) throws SQLException {
        long inicio = System.currentTimeMillis();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(consulta.getSql())) {
            switch (consulta.getTipoParametro()) {
                case INTEIRO -> ps.setInt(1, Integer.parseInt(parametro.trim()));
                case TEXTO -> ps.setString(1, parametro);
                case NENHUM -> { }
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
