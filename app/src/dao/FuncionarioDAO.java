package dao;

import model.Funcionario;
import util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FuncionarioDAO {
    public void inserir(Funcionario f) throws SQLException {
        String sql = "INSERT INTO FUNCIONARIO (nome, cpf, telefone, cep, rua, bairro, numero, apartamento, "
                   + "nr_funcionario_supervisor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencher(ps, f);
            ps.executeUpdate();
        }
    }

    /** Lista com o nome do supervisor obtido por auto-relacionamento (LEFT JOIN). */
    public List<Funcionario> listar() throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT f.nr_funcionario, f.nome, f.cpf, f.telefone, f.cep, f.rua, f.bairro, f.numero, "
                   + "f.apartamento, f.nr_funcionario_supervisor, s.nome AS nome_supervisor "
                   + "FROM FUNCIONARIO f "
                   + "LEFT JOIN FUNCIONARIO s ON s.nr_funcionario = f.nr_funcionario_supervisor "
                   + "ORDER BY f.nr_funcionario";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Funcionario f = new Funcionario();
                f.setNrFuncionario(rs.getInt("nr_funcionario"));
                f.setNome(rs.getString("nome"));
                f.setCpf(rs.getString("cpf"));
                f.setTelefone(rs.getString("telefone"));
                f.setCep(rs.getString("cep"));
                f.setRua(rs.getString("rua"));
                f.setBairro(rs.getString("bairro"));
                f.setNumero(rs.getString("numero"));
                f.setApartamento(rs.getString("apartamento"));
                int sup = rs.getInt("nr_funcionario_supervisor");
                f.setNrSupervisor(rs.wasNull() ? null : sup);
                f.setNomeSupervisor(rs.getString("nome_supervisor"));
                lista.add(f);
            }
        }
        return lista;
    }

    public void atualizar(Funcionario f) throws SQLException {
        String sql = "UPDATE FUNCIONARIO SET nome=?, cpf=?, telefone=?, cep=?, rua=?, bairro=?, numero=?, "
                   + "apartamento=?, nr_funcionario_supervisor=? WHERE nr_funcionario=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencher(ps, f);
            ps.setInt(10, f.getNrFuncionario());
            ps.executeUpdate();
        }
    }

    /** Subordinados ficam sem supervisor automaticamente (FK com ON DELETE SET NULL). */
    public void excluir(int nrFuncionario) throws SQLException {
        String sql = "DELETE FROM FUNCIONARIO WHERE nr_funcionario=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nrFuncionario);
            ps.executeUpdate();
        }
    }

    private void preencher(PreparedStatement ps, Funcionario f) throws SQLException {
        ps.setString(1, f.getNome());
        ps.setString(2, f.getCpf());
        ps.setString(3, f.getTelefone());
        ps.setString(4, f.getCep());
        ps.setString(5, f.getRua());
        ps.setString(6, f.getBairro());
        ps.setString(7, f.getNumero());
        ps.setString(8, f.getApartamento());
        if (f.getNrSupervisor() == null) {
            ps.setNull(9, Types.INTEGER);
        } else {
            ps.setInt(9, f.getNrSupervisor());
        }
    }
}
