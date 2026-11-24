package prueba.biblioteca.controlador;

import prueba.biblioteca.database.BibliotecaDB;

import java.io.IOException;
import java.sql.SQLException;

/** Controla la configuración y comprobación de las conexiones MySQL. */
public final class ControladorConexion {
    private final BibliotecaDB modelo;

    public ControladorConexion(BibliotecaDB modelo) { this.modelo = modelo; }

    public boolean configurada() { return modelo.configured(); }
    public boolean replicaConfigurada() { return modelo.replicaConfigured(); }
    public String valor(String propiedad, String predeterminado) { return modelo.value(propiedad, predeterminado); }
    public void probar() throws SQLException { modelo.testConnections(); }

    public void guardar(String url, String usuario, String contrasena,
                        String urlEspejo, String usuarioEspejo, String contrasenaEspejo)
            throws IOException, SQLException {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("La URL de la conexión principal es obligatoria.");
        if (usuario == null || usuario.isBlank()) throw new IllegalArgumentException("El usuario de MySQL es obligatorio.");
        if (urlEspejo != null && !urlEspejo.isBlank() && (usuarioEspejo == null || usuarioEspejo.isBlank())) {
            throw new IllegalArgumentException("Escribe el usuario de MySQL para la réplica.");
        }
        modelo.saveConfig(url, usuario, contrasena, urlEspejo == null ? "" : urlEspejo,
                usuarioEspejo == null ? "" : usuarioEspejo, contrasenaEspejo == null ? "" : contrasenaEspejo);
        modelo.testConnections();
    }
}
