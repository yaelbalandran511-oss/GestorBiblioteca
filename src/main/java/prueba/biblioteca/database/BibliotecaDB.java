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
    /** Propiedades locales para una o dos conexiones MySQL. */
    private final Properties p = new Properties();
    /** Archivo ignorado por Git donde cada computadora conserva sus credenciales. */
    private final Path config = Path.of("config.properties");

    /** Carga la configuración local si ya se creó desde la ventana de conexiones. */
    public BibliotecaDB() {
        if (Files.exists(config)) {
            try (InputStream in = Files.newInputStream(config)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer config.properties", e);
            }
        }
    }

    /** Lee un valor de configuración o usa el valor alternativo indicado. */
    public String value(String key, String fallback) { return p.getProperty(key, fallback); }
    /** Informa si la URL de la conexión principal está definida. */
    public boolean configured() { return !value("primary.url", "").isBlank(); }
    /** Informa si existe una URL para la conexión espejo. */
    public boolean replicaConfigured() { return !value("replica.url", "").isBlank(); }

    /** Guarda las conexiones localmente sin incluirlas en los archivos del proyecto. */
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

    /** Abre la conexión principal o la espejo usando JDBC. */
    private Connection connect(boolean replica) throws SQLException {
        String prefix = replica ? "replica." : "primary.";
        String url = value(prefix + "url", "");
        if (url.isBlank()) {
            throw new SQLException(replica ? "La conexión espejo no está configurada." : "Configura la conexión principal primero.");
        }
        return DriverManager.getConnection(url, value(prefix + "user", ""), value(prefix + "password", ""));
    }

    /** Prueba conexión principal y, cuando aplica, la conexión y el esquema espejo. */
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

    // Métodos de lectura: consultan tablas y convierten cada fila al objeto del modelo correspondiente.
    public List<Autor> autores() throws SQLException {
        List<Autor> result = new ArrayList<>();
        try (Connection c = connect(false); Statement s = c.createStatement();
             ResultSet rows = s.executeQuery("SELECT id_autor,nombre,nacionalidad FROM Autor ORDER BY nombre")) {
            while (rows.next()) result.add(new Autor(rows.getInt(1), rows.getString(2), rows.getString(3)));
        }
        return result;
    }

    /** Obtiene las categorías para el selector del formulario de libros. */
    public List<Categoria> categorias() throws SQLException {
        List<Categoria> result = new ArrayList<>();
        try (Connection c = connect(false); Statement s = c.createStatement();
             ResultSet rows = s.executeQuery("SELECT id_categoria,nombre,descripcion FROM Categoria ORDER BY nombre")) {
            while (rows.next()) result.add(new Categoria(rows.getInt(1), rows.getString(2), rows.getString(3)));
        }
        return result;
    }

    /** Obtiene usuarios para el formulario de préstamos. */
    public List<Usuario> usuarios() throws SQLException {
        List<Usuario> result = new ArrayList<>();
        try (Connection c = connect(false); Statement s = c.createStatement();
             ResultSet rows = s.executeQuery("SELECT id_usuario,nombre,correo_electronico,fecha_registro FROM Usuario ORDER BY nombre")) {
            while (rows.next()) result.add(new Usuario(rows.getInt(1), rows.getString(2), rows.getString(3), rows.getDate(4).toLocalDate()));
        }
        return result;
    }

    /** Une Libro con Autor y Categoria para mostrar los nombres junto a sus IDs. */
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

    /** Devuelve Prestado y Retraso como activos; con historial=true incluye también Devuelto. */
    public List<Prestamo> prestamos(boolean historial) throws SQLException {
        List<Prestamo> result = new ArrayList<>();
        String sql = "SELECT p.id_prestamo,p.fecha_salida,p.estado,p.id_usuario,p.id_libro,u.nombre,l.titulo " +
                "FROM Prestamo p JOIN Usuario u ON u.id_usuario=p.id_usuario " +
                "JOIN Libro l ON l.id_libro=p.id_libro" + (historial ? "" : " WHERE p.estado IN ('Prestado','Retraso')") +
                " ORDER BY p.id_prestamo DESC";
        try (Connection c = connect(false); Statement s = c.createStatement(); ResultSet rows = s.executeQuery(sql)) {
            while (rows.next()) result.add(new Prestamo(rows.getInt(1), rows.getDate(2).toString(), rows.getString(3),
                    rows.getInt(4), rows.getInt(5), rows.getString(6), rows.getString(7)));
        }
        return result;
    }

    /** Inserta o actualiza un libro; crea autor/nacionalidad si es nuevo y replica los cambios. */
    public void guardarLibro(int id, String titulo, String isbn, String nombreAutor,
                             String nacionalidadAutor, int categoria) throws SQLException {
        String cleanTitle = titulo == null ? "" : titulo.trim();
        String cleanIsbn = isbn == null ? "" : isbn.trim();
        String cleanAuthor = nombreAutor == null ? "" : nombreAutor.trim();
        String cleanNationality = nacionalidadAutor == null ? "" : nacionalidadAutor.trim();
        if (cleanTitle.isEmpty() || cleanTitle.length() > 150) throw new SQLException("El título es obligatorio y admite hasta 150 caracteres.");
        if (cleanIsbn.isEmpty() || cleanIsbn.length() > 20) throw new SQLException("El ISBN es obligatorio y admite hasta 20 caracteres.");
        if (cleanAuthor.isEmpty() || cleanAuthor.length() > 100) throw new SQLException("El nombre del autor es obligatorio y admite hasta 100 caracteres.");
        if (cleanNationality.isEmpty() || cleanNationality.length() > 50) throw new SQLException("La nacionalidad del autor es obligatoria y admite hasta 50 caracteres.");
        final int[] generatedId = {id};
        final int[] authorId = {0};
        writeBoth((c, schema, mirror) -> {
            // El autor se busca por nombre; si es nuevo, se inserta antes que el libro para respetar la FK.
            int autor = obtenerOCrearAutor(c, schema, cleanAuthor, cleanNationality, mirror, authorId[0]);
            if (!mirror) authorId[0] = autor;
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

    /** Busca al autor sin distinguir mayúsculas; crea uno nuevo y mantiene su ID en ambas bases. */
    private int obtenerOCrearAutor(Connection c, String schema, String nombre, String nacionalidad,
                                   boolean mirror, int idEsperado) throws SQLException {
        String authorTable = table(schema, "Autor");
        String lookup = "SELECT id_autor,nacionalidad FROM " + authorTable +
                " WHERE LOWER(TRIM(nombre))=LOWER(?) ORDER BY id_autor LIMIT 1";
        try (PreparedStatement s = c.prepareStatement(lookup)) {
            s.setString(1, nombre);
            try (ResultSet rows = s.executeQuery()) {
                if (rows.next()) {
                    int foundId = rows.getInt(1);
                    // El ID debe coincidir para que el mismo libro apunte al mismo autor en ambas bases.
                    if (mirror && foundId != idEsperado) {
                        throw new SQLException("El autor existe con otro ID en la base espejo. Sincroniza los catálogos antes de guardar.");
                    }
                    // Completa nacionalidades antiguas vacías, pero conserva cualquier valor ya registrado.
                    String currentNationality = rows.getString(2);
                    if (currentNationality == null || currentNationality.isBlank()) {
                        String update = "UPDATE " + authorTable +
                                " SET nacionalidad=? WHERE id_autor=? AND (nacionalidad IS NULL OR TRIM(nacionalidad)='')";
                        try (PreparedStatement updateStatement = c.prepareStatement(update)) {
                            updateStatement.setString(1, nacionalidad);
                            updateStatement.setInt(2, foundId);
                            updateStatement.executeUpdate();
                        }
                    }
                    return foundId;
                }
            }
        }

        // En la base espejo se reutiliza el ID creado/consultado en la conexión principal.
        if (mirror) {
            if (idEsperado <= 0) throw new SQLException("No se pudo sincronizar el autor con la base espejo.");
            String insert = "INSERT INTO " + authorTable + "(id_autor,nombre,nacionalidad) VALUES(?,?,?)";
            try (PreparedStatement s = c.prepareStatement(insert)) {
                s.setInt(1, idEsperado); s.setString(2, nombre); s.setString(3, nacionalidad); s.executeUpdate();
            }
            return idEsperado;
        }

        String insert = "INSERT INTO " + authorTable + "(nombre,nacionalidad) VALUES(?,?)";
        try (PreparedStatement s = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, nombre); s.setString(2, nacionalidad); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el ID del autor nuevo.");
                return keys.getInt(1);
            }
        }
    }

    /** Borra el libro y, si ya no quedan libros de ese autor, elimina también al autor sin uso. */
    public void borrarLibro(int id) throws SQLException {
        final int[] authorId = {0};
        writeBoth((c, schema, mirror) -> {
            String bookTable = table(schema, "Libro");
            String findAuthor = "SELECT id_autor FROM " + bookTable + " WHERE id_libro=?";
            try (PreparedStatement s = c.prepareStatement(findAuthor)) {
                s.setInt(1, id);
                try (ResultSet row = s.executeQuery()) {
                    if (!row.next()) throw new SQLException("El libro ya no existe.");
                    int foundAuthorId = row.getInt(1);
                    // Se evita borrar relaciones incorrectas cuando los dos catálogos están desincronizados.
                    if (mirror && foundAuthorId != authorId[0]) {
                        throw new SQLException("El libro está relacionado con autores distintos entre la base principal y la espejo.");
                    }
                    if (!mirror) authorId[0] = foundAuthorId;
                }
            }

            try (PreparedStatement s = c.prepareStatement("DELETE FROM " + bookTable + " WHERE id_libro=?")) {
                s.setInt(1, id);
                try {
                    if (s.executeUpdate() == 0) throw new SQLException("El libro ya no existe.");
                } catch (SQLException ex) {
                    throw friendlyConstraint(ex, "No se puede eliminar el libro porque tiene préstamos asociados.");
                }
            }

            // Conserva autores compartidos: solo borra el autor cuando ya no tiene libros.
            String deleteUnusedAuthor = "DELETE FROM " + table(schema, "Autor") +
                    " WHERE id_autor=? AND NOT EXISTS (SELECT 1 FROM " + bookTable + " WHERE id_autor=?)";
            try (PreparedStatement s = c.prepareStatement(deleteUnusedAuthor)) {
                s.setInt(1, authorId[0]);
                s.setInt(2, authorId[0]);
                s.executeUpdate();
            }
        });
    }

    /** Inserta o actualiza un préstamo; Prestado y Retraso ocupan el libro hasta su devolución. */
    public void guardarPrestamo(int id, String fecha, String estado, int usuario, int libro) throws SQLException {
        LocalDate loanDate;
        try { loanDate = LocalDate.parse(fecha); }
        catch (RuntimeException ex) { throw new SQLException("La fecha del préstamo no es válida.", ex); }
        if (loanDate.isAfter(LocalDate.now())) throw new SQLException("La fecha de salida no puede estar en el futuro.");
        if (usuario <= 0 || libro <= 0) throw new SQLException("Selecciona un usuario y un libro válidos.");
        if (!"Prestado".equals(estado) && !"Retraso".equals(estado) && !"Devuelto".equals(estado)) {
            throw new SQLException("El estado del préstamo no es válido.");
        }
        String loanState = estado;
        final int[] generatedId = {id};
        writeBoth((c, schema, mirror) -> {
            String loanTable = table(schema, "Prestamo");
            if ("Prestado".equals(loanState) || "Retraso".equals(loanState)) validarDisponible(c, schema, libro, id);
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

    /** Bloquea el libro y evita dos préstamos activos (Prestado o Retraso) al mismo tiempo. */
    private void validarDisponible(Connection c, String schema, int libro, int prestamoActual) throws SQLException {
        String bookTable = table(schema, "Libro");
        try (PreparedStatement lock = c.prepareStatement("SELECT id_libro FROM " + bookTable + " WHERE id_libro=? FOR UPDATE")) {
            lock.setInt(1, libro);
            try (ResultSet row = lock.executeQuery()) {
                if (!row.next()) throw new SQLException("El libro seleccionado ya no existe.");
            }
        }
        String sql = "SELECT id_prestamo FROM " + table(schema, "Prestamo") +
                " WHERE id_libro=? AND estado IN ('Prestado','Retraso') AND id_prestamo<>? LIMIT 1";
        try (PreparedStatement check = c.prepareStatement(sql)) {
            check.setInt(1, libro); check.setInt(2, prestamoActual);
            try (ResultSet row = check.executeQuery()) {
                if (row.next()) throw new SQLException("Ese libro ya tiene un préstamo activo. Registra su devolución antes de prestarlo otra vez.");
            }
        }
    }

    /** Borra el registro de préstamo seleccionado y replica la operación. */
    public void borrarPrestamo(int id) throws SQLException {
        writeBoth((c, schema, mirror) -> {
            try (PreparedStatement s = c.prepareStatement("DELETE FROM " + table(schema, "Prestamo") + " WHERE id_prestamo=?")) {
                s.setInt(1, id); s.executeUpdate();
            }
        });
    }

    /** Cambia el estado permitido y sincroniza ambas bases. */
    public void cambiarEstado(int id, String estado) throws SQLException {
        if (!"Prestado".equals(estado) && !"Retraso".equals(estado) && !"Devuelto".equals(estado)) {
            throw new SQLException("El estado del préstamo no es válido.");
        }
        writeBoth((c, schema, mirror) -> {
            try (PreparedStatement s = c.prepareStatement("UPDATE " + table(schema, "Prestamo") + " SET estado=? WHERE id_prestamo=?")) {
                s.setString(1, estado); s.setInt(2, id); s.executeUpdate();
            }
        });
    }

    /** Operación reutilizable que writeBoth ejecuta en cada conexión/esquema. */
    @FunctionalInterface private interface WriteAction {
        void run(Connection connection, String schema, boolean mirror) throws SQLException;
    }

    /** Ejecuta una escritura transaccional en la base principal y opcionalmente en la espejo. */
    private void writeBoth(WriteAction action) throws SQLException {
        if (!replicaConfigured()) {
            // Sin réplica, la transacción solo afecta a la base principal.
            try (Connection primary = connect(false)) {
                primary.setAutoCommit(false);
                try { action.run(primary, "", false); primary.commit(); }
                catch (SQLException | RuntimeException ex) { rollback(primary); throw ex; }
            }
            return;
        }

        if (sameEndpointAndCredentials()) {
            // Si las dos bases están en el mismo servidor con la misma cuenta, ambas se escriben desde una conexión.
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

        // Para servidores separados se abre una transacción por servidor; si falla alguna escritura, se revierten ambas.
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

    /** Detecta si ambas URLs apuntan al mismo servidor y usan las mismas credenciales. */
    private boolean sameEndpointAndCredentials() {
        String primaryEndpoint = endpoint(value("primary.url", ""));
        String mirrorEndpoint = endpoint(value("replica.url", ""));
        return !primaryEndpoint.isEmpty() && primaryEndpoint.equalsIgnoreCase(mirrorEndpoint)
                && value("primary.user", "").equals(value("replica.user", ""))
                && value("primary.password", "").equals(value("replica.password", ""));
    }

    /** Extrae host y puerto de una URL JDBC para comparar servidores. */
    private String endpoint(String url) {
        Matcher matcher = Pattern.compile("^jdbc:mysql://([^/?;]+)", Pattern.CASE_INSENSITIVE).matcher(url);
        return matcher.find() ? matcher.group(1) : "";
    }

    /** Obtiene el nombre de esquema de una URL JDBC y valida caracteres permitidos. */
    private String schemaFromUrl(String url) throws SQLException {
        Matcher matcher = Pattern.compile("^jdbc:mysql://[^/]+/([^?;]+)", Pattern.CASE_INSENSITIVE).matcher(url);
        if (!matcher.find() || !matcher.group(1).matches("[A-Za-z0-9_$]+")) {
            throw new SQLException("La URL de la réplica debe incluir un nombre de base de datos válido.");
        }
        return matcher.group(1);
    }

    /** Construye una tabla SQL; valida el nombre del esquema antes de insertarlo en la consulta. */
    private String table(String schema, String name) {
        if (schema == null || schema.isBlank()) return "`" + name + "`";
        if (!schema.matches("[A-Za-z0-9_$]+")) throw new IllegalArgumentException("Nombre de base de datos inválido.");
        return "`" + schema + "`.`" + name + "`";
    }

    /** Revierte cambios pendientes; se ignora un error secundario durante rollback. */
    private void rollback(Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    /** Traduce una violación de integridad referencial a un mensaje más claro. */
    private SQLException friendlyConstraint(SQLException ex, String foreignKeyMessage) {
        if ("23000".equals(ex.getSQLState())) return new SQLException(foreignKeyMessage, ex.getSQLState(), ex.getErrorCode(), ex);
        return ex;
    }
}
