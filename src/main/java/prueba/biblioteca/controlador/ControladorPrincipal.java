package prueba.biblioteca.controlador;

import javafx.stage.Stage;
import prueba.biblioteca.database.BibliotecaDB;
import prueba.biblioteca.vista.VistaConexiones;
import prueba.biblioteca.vista.VistaLibros;
import prueba.biblioteca.vista.VistaPrestamos;
import prueba.biblioteca.vista.VistaPrincipal;

/** Coordina navegación y composición de vistas; no contiene consultas ni controles de formulario. */
public final class ControladorPrincipal {
    /** Vista que contiene la ventana y la navegación principal. */
    private final VistaPrincipal vista;
    /** Controladores de las secciones; comparten un único modelo de base de datos. */
    private final ControladorLibros libros;
    private final ControladorPrestamos prestamos;
    private final ControladorConexion conexiones;
    /** Ventana JavaFX que se usa como propietaria de los diálogos. */
    private Stage stage;
    /** Resultado de la última prueba de la base espejo. */
    private boolean replicaDisponible;

    /** Construye la coordinación MVC y conecta cada botón con su acción. */
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

    /** Muestra la ventana, verifica la conexión y abre el catálogo inicial. */
    public void iniciar(Stage stage) {
        this.stage = stage;
        vista.mostrar(stage);
        comprobarConexiones();
        if (!conexiones.configurada()) abrirConexiones();
        else mostrarLibros();
    }

    /** Actualiza el estado visible de conexión principal y réplica. */
    private void comprobarConexiones() {
        replicaDisponible = false;
        try {
            conexiones.probar();
            replicaDisponible = conexiones.replicaConfigurada();
        } catch (Exception ignored) {
            vista.actualizarEstado("No se pudieron validar todas las conexiones");
        }
    }

    /** Prepara la pantalla del catálogo y carga sus registros. */
    private void mostrarLibros() {
        vista.seleccionarSeccion("libros");
        VistaLibros pagina = new VistaLibros(libros, cantidad ->
                vista.actualizarEstado("Catálogo actualizado • " + cantidad + " libros • " + estadoReplica()));
        pagina.setOwner(stage);
        vista.mostrarContenido(pagina);
        pagina.inicializar();
    }

    /** Prepara la pantalla de préstamos activos o historial completo. */
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

    /** Texto corto que informa si la escritura espejo está configurada y disponible. */
    private String estadoReplica() {
        if (!conexiones.replicaConfigurada()) return "sin réplica configurada";
        return replicaDisponible ? "réplica conectada" : "réplica sin conexión";
    }

    /** Abre el diálogo de conexiones y vuelve al catálogo después de guardar. */
    private void abrirConexiones() {
        new VistaConexiones(conexiones).mostrar(stage, () -> {
            comprobarConexiones();
            mostrarLibros();
        });
    }
}
