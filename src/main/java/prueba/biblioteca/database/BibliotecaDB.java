package prueba.biblioteca.database;

import prueba.biblioteca.modelo.Libro;
import prueba.biblioteca.modelo.Prestamo;
import prueba.biblioteca.modelo.Autor;
import prueba.biblioteca.modelo.Categoria;
import prueba.biblioteca.modelo.Usuario;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BibliotecaDB {
    private final Properties p = new Properties();
    private final Path config = Path.of("config.properties");

    public BibliotecaDB() {
        if (Files.exists(config)) {
            try (InputStream in = Files.newInputStream(config)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer config.properties", e);
            }
        }
    }

    public String value(String key, String fallback) { return p.getProperty(key, fallback); }
    public boolean configured() { return !value("primary.url", "").isBlank(); }
    public boolean replicaConfigured() { return !value("replica.url", "").isBlank(); }

    public void saveConfig(String url, String user, String pass, String replicaUrl, String replicaUser, String replicaPass) throws IOException {
        p.setProperty("primary.url", url.trim());
        p.setProperty("primary.user", user.trim());
        p.setProperty("primary.password", pass);
        p.setProperty("replica.url", replicaUrl.trim());
        p.setProperty("replica.user", replicaUser.trim());
        p.setProperty("replica.password", replicaPass);
        try (Writer writer = Files.newBufferedWriter(config)) {
            p.store(writer, "Configuracion local; no compartir este archivo");
        }
    }

    private Connection connect(boolean replica) throws SQLException {
        String prefix = replica ? "replica." : "primary.";
        String url = value(prefix + "url", "");
        if (url.isBlank()) {
            throw new SQLException(replica ? "La conexión espejo no está configurada." : "Configura la conexión principal primero.");
        }
        return DriverManager.getConnection(url, value(prefix + "user", ""), value(prefix + "password", ""));
    }

    public void testConnections() throws SQLException {
        try (Connection ignored = connect(false)) { }
        if (replicaConfigured()) {
            try (Connection ignored = connect(true)) { }
            if (sameEndpointAndCredentials()) {
                String mirrorSchema = schemaFromUrl(value("replica.url", ""));
                try (Connection primary = connect(false); Statement statement = primary.createStatement()) {
                    statement.executeQuery("SELECT 1 FROM " + table(mirrorSchema, "Usuario") + " LIMIT 0").close();
                }
            }
        }
    }

    public List<Autor> autores() throws SQLException {
        List<Autor> result = new ArrayList<>();
        try (Connection c = connect(false); Statement s = c.createStatement();
             ResultSet rows = s.executeQuery("SELECT id_autor,nombre,nacionalidad FROM Autor ORDER BY nombre")) {
            while (rows.next()) result.add(new Autor(rows.getInt(1), rows.getString(2), rows.getString(3)));
        }
        return result;
    }

    public List<Categoria> categorias() throws SQLException {
        List<Categoria> result = new ArrayList<>();
        try (Connection c = connect(false); Statement s = c.createStatement();
             ResultSet rows = s.executeQuery("SELECT id_categoria,nombre,descripcion FROM Categoria ORDER BY nombre")) {
            while (rows.next()) result.add(new Categoria(rows.getInt(1), rows.getString(2), rows.getString(3)));
        }
        return result;
    }

    public List<Usuario> usuarios() throws SQLException {
        List<Usuario> result = new ArrayList<>();
        try (Connection c = connect(false); Statement s = c.createStatement();
             ResultSet rows = s.executeQuery("SELECT id_usuario,nombre,correo_electronico,fecha_registro FROM Usuario ORDER BY nombre")) {
            while (rows.next()) result.add(new Usuario(rows.getInt(1), rows.getString(2), rows.getString(3), rows.getDate(4).toLocalDate()));
        }
        return result;
    }

    public List<Libro> libros() throws SQLException {
        List<Libro> result = new ArrayList<>();
        String sql = "SELECT l.id_libro,l.titulo,l.isbn,l.id_autor,l.id_categoria,a.nombre,c.nombre " +
                "FROM Libro l JOIN Autor a ON a.id_autor=l.id_autor " +
                "JOIN Categoria c ON c.id_categoria=l.id_categoria ORDER BY l.id_libro DESC";
        try (Connection c = connect(false); Statement s = c.createStatement(); ResultSet rows = s.executeQuery(sql)) {
            while (rows.next()) result.add(new Libro(rows.getInt(1), rows.getString(2), rows.getString(3),
                    rows.getInt(4), rows.getInt(5), rows.getString(6), rows.getString(7)));
        }
        return result;
    }

    public List<Prestamo> prestamos(boolean historial) throws SQLException {
        List<Prestamo> result = new ArrayList<>();
        String sql = "SELECT p.id_prestamo,p.fecha_salida,p.estado,p.id_usuario,p.id_libro,u.nombre,l.titulo " +
                "FROM Prestamo p JOIN Usuario u ON u.id_usuario=p.id_usuario " +
                "JOIN Libro l ON l.id_libro=p.id_libro" + (historial ? "" : " WHERE p.estado='Prestado'") +
                " ORDER BY p.id_prestamo DESC";
        try (Connection c = connect(false); Statement s = c.createStatement(); ResultSet rows = s.executeQuery(sql)) {
            while (rows.next()) result.add(new Prestamo(rows.getInt(1), rows.getDate(2).toString(), rows.getString(3),
                    rows.getInt(4), rows.getInt(5), rows.getString(6), rows.getString(7)));
        }
        return result;
    }

    public void guardarLibro(int id, String titulo, String isbn, int autor, int categoria) throws SQLException {
        String cleanTitle = titulo == null ? "" : titulo.trim();
        String cleanIsbn = isbn == null ? "" : isbn.trim();
        if (cleanTitle.isEmpty() || cleanTitle.length() > 150) throw new SQLException("El título es obligatorio y admite hasta 150 caracteres.");
        if (cleanIsbn.isEmpty() || cleanIsbn.length() > 20) throw new SQLException("El ISBN es obligatorio y admite hasta 20 caracteres.");
        final int[] generatedId = {id};
        writeBoth((c, schema, mirror) -> {
            String table = table(schema, "Libro");
            if (id == 0) {
                if (mirror) {
                    try (PreparedStatement s = c.prepareStatement("INSERT INTO " + table + "(id_libro,titulo,isbn,id_autor,id_categoria) VALUES(?,?,?,?,?)")) {
                        s.setInt(1, generatedId[0]); s.setString(2, cleanTitle); s.setString(3, cleanIsbn);
                        s.setInt(4, autor); s.setInt(5, categoria); s.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO " + table + "(titulo,isbn,id_autor,id_categoria) VALUES(?,?,?,?)";
                    try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        s.setString(1, cleanTitle); s.setString(2, cleanIsbn); s.setInt(3, autor); s.setInt(4, categoria); s.executeUpdate();
                        try (ResultSet keys = s.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("MySQL no devolvió el ID del libro nuevo.");
                            generatedId[0] = keys.getInt(1);
                        }
                    }
                }
            } else {
                try (PreparedStatement s = c.prepareStatement("UPDATE " + table + " SET titulo=?,isbn=?,id_autor=?,id_categoria=? WHERE id_libro=?")) {
                    s.setString(1, cleanTitle); s.setString(2, cleanIsbn); s.setInt(3, autor); s.setInt(4, categoria); s.setInt(5, id);
                    s.executeUpdate();
                }
            }
        });
    }

    public void borrarLibro(int id) throws SQLException {
        writeBoth((c, schema, mirror) -> {
            try (PreparedStatement s = c.prepareStatement("DELETE FROM " + table(schema, "Libro") + " WHERE id_libro=?")) {
                s.setInt(1, id);
                try { s.executeUpdate(); } catch (SQLException ex) { throw friendlyConstraint(ex, "No se puede eliminar el libro porque tiene préstamos asociados."); }
            }
        });
    }

    public void guardarPrestamo(int id, String fecha, int estado, int usuario, int libro) throws SQLException {
        LocalDate loanDate;
        try { loanDate = LocalDate.parse(fecha); }
        catch (RuntimeException ex) { throw new SQLException("La fecha del préstamo no es válida.", ex); }
        if (loanDate.isAfter(LocalDate.now())) throw new SQLException("La fecha de salida no puede estar en el futuro.");
        if (usuario <= 0 || libro <= 0) throw new SQLException("Selecciona un usuario y un libro válidos.");
        String loanState = estado == 1 ? "Prestado" : "Devuelto";
        final int[] generatedId = {id};
        writeBoth((c, schema, mirror) -> {
            String loanTable = table(schema, "Prestamo");
            if ("Prestado".equals(loanState)) validarDisponible(c, schema, libro, id);
            if (id == 0) {
                if (mirror) {
                    try (PreparedStatement s = c.prepareStatement("INSERT INTO " + loanTable + "(id_prestamo,fecha_salida,estado,id_usuario,id_libro) VALUES(?,?,?,?,?)")) {
                        s.setInt(1, generatedId[0]); s.setDate(2, java.sql.Date.valueOf(loanDate)); s.setString(3, loanState);
                        s.setInt(4, usuario); s.setInt(5, libro); s.executeUpdate();
                    }
                } else {
                    try (PreparedStatement s = c.prepareStatement("INSERT INTO " + loanTable + "(fecha_salida,estado,id_usuario,id_libro) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        s.setDate(1, java.sql.Date.valueOf(loanDate)); s.setString(2, loanState); s.setInt(3, usuario); s.setInt(4, libro); s.executeUpdate();
                        try (ResultSet keys = s.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("MySQL no devolvió el ID del préstamo nuevo.");
                            generatedId[0] = keys.getInt(1);
                        }
                    }
                }
            } else {
                try (PreparedStatement s = c.prepareStatement("UPDATE " + loanTable + " SET fecha_salida=?,estado=?,id_usuario=?,id_libro=? WHERE id_prestamo=?")) {
                    s.setDate(1, java.sql.Date.valueOf(loanDate)); s.setString(2, loanState); s.setInt(3, usuario); s.setInt(4, libro); s.setInt(5, id);
                    s.executeUpdate();
                }
            }
        });
    }

    private void validarDisponible(Connection c, String schema, int libro, int prestamoActual) throws SQLException {
        String bookTable = table(schema, "Libro");
        try (PreparedStatement lock = c.prepareStatement("SELECT id_libro FROM " + bookTable + " WHERE id_libro=? FOR UPDATE")) {
            lock.setInt(1, libro);
            try (ResultSet row = lock.executeQuery()) {
                if (!row.next()) throw new SQLException("El libro seleccionado ya no existe.");
            }
        }
        String sql = "SELECT id_prestamo FROM " + table(schema, "Prestamo") +
                " WHERE id_libro=? AND estado='Prestado' AND id_prestamo<>? LIMIT 1";
        try (PreparedStatement check = c.prepareStatement(sql)) {
            check.setInt(1, libro); check.setInt(2, prestamoActual);
            try (ResultSet row = check.executeQuery()) {
                if (row.next()) throw new SQLException("Ese libro ya tiene un préstamo activo. Registra su devolución antes de prestarlo otra vez.");
            }
        }
    }

    public void borrarPrestamo(int id) throws SQLException {
        writeBoth((c, schema, mirror) -> {
            try (PreparedStatement s = c.prepareStatement("DELETE FROM " + table(schema, "Prestamo") + " WHERE id_prestamo=?")) {
                s.setInt(1, id); s.executeUpdate();
            }
        });
    }

    public void cambiarEstado(int id, String estado) throws SQLException {
        if (!"Prestado".equals(estado) && !"Devuelto".equals(estado)) throw new SQLException("El estado del préstamo no es válido.");
        writeBoth((c, schema, mirror) -> {
            try (PreparedStatement s = c.prepareStatement("UPDATE " + table(schema, "Prestamo") + " SET estado=? WHERE id_prestamo=?")) {
                s.setString(1, estado); s.setInt(2, id); s.executeUpdate();
            }
        });
    }

    @FunctionalInterface private interface WriteAction {
        void run(Connection connection, String schema, boolean mirror) throws SQLException;
    }

    private void writeBoth(WriteAction action) throws SQLException {
        if (!replicaConfigured()) {
            try (Connection primary = connect(false)) {
                primary.setAutoCommit(false);
                try { action.run(primary, "", false); primary.commit(); }
                catch (SQLException | RuntimeException ex) { rollback(primary); throw ex; }
            }
            return;
        }

        if (sameEndpointAndCredentials()) {
            String mirrorSchema = schemaFromUrl(value("replica.url", ""));
            try (Connection connection = connect(false)) {
                connection.setAutoCommit(false);
                try {
                    action.run(connection, "", false);
                    action.run(connection, mirrorSchema, true);
                    connection.commit();
                } catch (SQLException | RuntimeException ex) {
                    rollback(connection);
                    throw ex;
                }
            }
            return;
        }

        // For separate servers both operations are transactional; a failure before commit rolls back both.
        try (Connection primary = connect(false); Connection mirror = connect(true)) {
            primary.setAutoCommit(false); mirror.setAutoCommit(false);
            try {
                action.run(primary, "", false);
                action.run(mirror, "", true);
                mirror.commit();
                primary.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(primary); rollback(mirror);
                throw ex;
            }
        }
    }

    private boolean sameEndpointAndCredentials() {
        String primaryEndpoint = endpoint(value("primary.url", ""));
        String mirrorEndpoint = endpoint(value("replica.url", ""));
        return !primaryEndpoint.isEmpty() && primaryEndpoint.equalsIgnoreCase(mirrorEndpoint)
                && value("primary.user", "").equals(value("replica.user", ""))
                && value("primary.password", "").equals(value("replica.password", ""));
    }

    private String endpoint(String url) {
        Matcher matcher = Pattern.compile("^jdbc:mysql://([^/?;]+)", Pattern.CASE_INSENSITIVE).matcher(url);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String schemaFromUrl(String url) throws SQLException {
        Matcher matcher = Pattern.compile("^jdbc:mysql://[^/]+/([^?;]+)", Pattern.CASE_INSENSITIVE).matcher(url);
        if (!matcher.find() || !matcher.group(1).matches("[A-Za-z0-9_$]+")) {
            throw new SQLException("La URL de la réplica debe incluir un nombre de base de datos válido.");
        }
        return matcher.group(1);
    }

    private String table(String schema, String name) {
        if (schema == null || schema.isBlank()) return "`" + name + "`";
        if (!schema.matches("[A-Za-z0-9_$]+")) throw new IllegalArgumentException("Nombre de base de datos inválido.");
        return "`" + schema + "`.`" + name + "`";
    }

    private void rollback(Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private SQLException friendlyConstraint(SQLException ex, String foreignKeyMessage) {
        if ("23000".equals(ex.getSQLState())) return new SQLException(foreignKeyMessage, ex.getSQLState(), ex.getErrorCode(), ex);
        return ex;
    }
}
