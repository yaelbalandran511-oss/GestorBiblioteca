package prueba.biblioteca.controlador;

import prueba.biblioteca.database.BibliotecaDB;

import java.io.IOException;
import java.sql.SQLException;

/** Controla la configuración y comprobación de las conexiones MySQL. */
public final class ControladorConexion {
    /** Modelo que lee/guarda las propiedades y abre las conexiones MySQL. */
    private final BibliotecaDB modelo;

    /** Recibe el modelo para no crear conexiones directamente desde la vista. */
    public ControladorConexion(BibliotecaDB modelo) { this.modelo = modelo; }

    /** Indica si hay una URL de base principal guardada. */
    public boolean configurada() { return modelo.configured(); }
    /** Indica si el usuario configuró una segunda base espejo. */
    public boolean replicaConfigurada() { return modelo.replicaConfigured(); }
    /** Lee una opción local y devuelve un valor alternativo si no existe. */
    public String valor(String propiedad, String predeterminado) { return modelo.value(propiedad, predeterminado); }
    /** Comprueba que las conexiones configuradas realmente respondan. */
    public void probar() throws SQLException { modelo.testConnections(); }

    /** Valida los campos, guarda la configuración local y prueba las bases. */
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
