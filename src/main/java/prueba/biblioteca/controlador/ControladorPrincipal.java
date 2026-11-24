package prueba.biblioteca.controlador;

import javafx.stage.Stage;
import prueba.biblioteca.database.BibliotecaDB;
import prueba.biblioteca.vista.VistaConexiones;
import prueba.biblioteca.vista.VistaLibros;
import prueba.biblioteca.vista.VistaPrestamos;
import prueba.biblioteca.vista.VistaPrincipal;

/** Coordina navegación y composición de vistas; no contiene consultas ni controles de formulario. */
public final class ControladorPrincipal {
    private final VistaPrincipal vista;
    private final ControladorLibros libros;
    private final ControladorPrestamos prestamos;
    private final ControladorConexion conexiones;
    private Stage stage;
    private boolean replicaDisponible;

    public ControladorPrincipal(BibliotecaDB modelo) {
        vista = new VistaPrincipal();
        libros = new ControladorLibros(modelo);
        prestamos = new ControladorPrestamos(modelo);
        conexiones = new ControladorConexion(modelo);
        vista.alElegirLibros(this::mostrarLibros);
        vista.alElegirPrestamos(() -> mostrarPrestamos(false));
        vista.alElegirHistorial(() -> mostrarPrestamos(true));
        vista.alElegirConexiones(this::abrirConexiones);
    }

    public void iniciar(Stage stage) {
        this.stage = stage;
        vista.mostrar(stage);
        comprobarConexiones();
        if (!conexiones.configurada()) abrirConexiones();
        else mostrarLibros();
    }

    private void comprobarConexiones() {
        replicaDisponible = false;
        try {
            conexiones.probar();
            replicaDisponible = conexiones.replicaConfigurada();
        } catch (Exception ignored) {
            vista.actualizarEstado("No se pudieron validar todas las conexiones");
        }
    }

    private void mostrarLibros() {
        vista.seleccionarSeccion("libros");
        VistaLibros pagina = new VistaLibros(libros, cantidad ->
                vista.actualizarEstado("Catálogo actualizado • " + cantidad + " libros • " + estadoReplica()));
        pagina.setOwner(stage);
        vista.mostrarContenido(pagina);
        pagina.inicializar();
    }

    private void mostrarPrestamos(boolean historial) {
        vista.seleccionarSeccion(historial ? "historial" : "prestamos");
        VistaPrestamos pagina = new VistaPrestamos(prestamos, historial, cantidad -> {
            String descripcion = historial ? "Historial completo • " + cantidad + " registros"
                    : "Préstamos por devolver • " + cantidad + " activos";
            vista.actualizarEstado(descripcion + " • " + estadoReplica());
        });
        pagina.setOwner(stage);
        vista.mostrarContenido(pagina);
        pagina.inicializar();
    }

    private String estadoReplica() {
        if (!conexiones.replicaConfigurada()) return "sin réplica configurada";
        return replicaDisponible ? "réplica conectada" : "réplica sin conexión";
    }

    private void abrirConexiones() {
        new VistaConexiones(conexiones).mostrar(stage, () -> {
            comprobarConexiones();
            mostrarLibros();
        });
    }
}
