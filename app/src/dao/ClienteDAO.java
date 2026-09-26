package dao;

import model.Cliente;
import util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    public void inserir(Cliente c) throws SQLException {
        String sql = "INSERT INTO CLIENTE (cpf_cnpj, telefone, telefone2, cep, rua, bairro, numero, apartamento) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCpfCnpj());
            ps.setString(2, c.getTelefone());
            ps.setString(3, c.getTelefone2());
            ps.setString(4, c.getCep());
            ps.setString(5, c.getRua());
            ps.setString(6, c.getBairro());
            ps.setString(7, c.getNumero());
            ps.setString(8, c.getApartamento());
            ps.executeUpdate();
        }
    }

    public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT nr_cliente, cpf_cnpj, telefone, telefone2, cep, rua, bairro, numero, apartamento "
                   + "FROM CLIENTE ORDER BY nr_cliente";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Cliente(
                    rs.getInt("nr_cliente"),
                    rs.getString("cpf_cnpj"),
                    rs.getString("telefone"),
                    rs.getString("telefone2"),
                    rs.getString("cep"),
                    rs.getString("rua"),
                    rs.getString("bairro"),
                    rs.getString("numero"),
                    rs.getString("apartamento")
                ));
            }
        }
        return lista;
    }

    public void atualizar(Cliente c) throws SQLException {
        String sql = "UPDATE CLIENTE SET cpf_cnpj=?, telefone=?, telefone2=?, cep=?, rua=?, bairro=?, "
                   + "numero=?, apartamento=? WHERE nr_cliente=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCpfCnpj());
            ps.setString(2, c.getTelefone());
            ps.setString(3, c.getTelefone2());
            ps.setString(4, c.getCep());
            ps.setString(5, c.getRua());
            ps.setString(6, c.getBairro());
            ps.setString(7, c.getNumero());
            ps.setString(8, c.getApartamento());
            ps.setInt(9, c.getNrCliente());
            ps.executeUpdate();
        }
    }

    public void excluir(int nrCliente) throws SQLException {
        String sql = "DELETE FROM CLIENTE WHERE nr_cliente=?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nrCliente);
            ps.executeUpdate();
        }
    }
}
