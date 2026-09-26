package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fabrica de conexoes JDBC com o MySQL.
 * Os dados de acesso sao lidos do arquivo db.properties (na pasta do projeto)
 * e podem ser alterados pela tela de conexao ao iniciar a aplicacao.
 */
public class ConnectionFactory {
    private static String url = "jdbc:mysql://localhost:3306/hortifruti";
    private static String user = "root";
    private static String pass = "";

    static {
        carregarPropriedades();
    }

    /**
     * Pasta do projeto (onde ficam db.properties e graficos/). Aceita executar tanto
     * a partir da pasta "app" quanto da raiz do repositorio (ex.: IntelliJ aberto na raiz).
     */
    public static Path pastaProjeto() {
        if (Files.exists(Path.of("db.properties"))) return Path.of(".");
        if (Files.exists(Path.of("app", "db.properties"))) return Path.of("app");
        return Path.of(".");
    }

    private static void carregarPropriedades() {
        Path arquivo = pastaProjeto().resolve("db.properties");
        if (!Files.exists(arquivo)) return;
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(arquivo.toFile())) {
            p.load(in);
            url = p.getProperty("db.url", url);
            user = p.getProperty("db.user", user);
            pass = p.getProperty("db.password", pass);
        } catch (IOException e) {
            System.err.println("Nao foi possivel ler db.properties: " + e.getMessage());
        }
    }

    public static void configurar(String novaUrl, String novoUser, String novaSenha) {
        url = novaUrl;
        user = novoUser;
        pass = novaSenha;
    }

    public static String getUrl() { return url; }
    public static String getUser() { return user; }
    public static String getPass() { return pass; }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }
}
