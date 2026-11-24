package prueba.biblioteca.vista;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Ventana principal: estructura visual y navegación, sin consultas SQL ni reglas de negocio. */
public final class VistaPrincipal {
    private final BorderPane root = new BorderPane();
    private final Label status = new Label("Preparando biblioteca…");
    private final Button navBooks = nav("▤   Libros");
    private final Button navLoans = nav("↗   Préstamos");
    private final Button navHistory = nav("◷   Historial");
    private Runnable alElegirLibros = () -> { };
    private Runnable alElegirPrestamos = () -> { };
    private Runnable alElegirHistorial = () -> { };
    private Runnable alElegirConexiones = () -> { };

    public VistaPrincipal() {
        root.setStyle("-fx-background-color:#f3f6fb; -fx-font-family:'Segoe UI';");
        root.setTop(crearEncabezado());
        root.setLeft(crearBarraLateral());
        status.setStyle("-fx-text-fill:#64748b; -fx-padding:12 20;");
        root.setBottom(status);
        navBooks.setOnAction(event -> alElegirLibros.run());
        navLoans.setOnAction(event -> alElegirPrestamos.run());
        navHistory.setOnAction(event -> alElegirHistorial.run());
    }

    public void mostrar(Stage stage) {
        Scene scene = new Scene(root, 1120, 740);
        var stylesheet = getClass().getResource("/prueba/biblioteca/estilos.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
        stage.setTitle("Biblioteca | Panel de gestión");
        stage.setMinWidth(950);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public void alElegirLibros(Runnable accion) { alElegirLibros = accion; }
    public void alElegirPrestamos(Runnable accion) { alElegirPrestamos = accion; }
    public void alElegirHistorial(Runnable accion) { alElegirHistorial = accion; }
    public void alElegirConexiones(Runnable accion) { alElegirConexiones = accion; }
    public void mostrarContenido(Node contenido) { root.setCenter(contenido); }
    public void actualizarEstado(String texto) { status.setText(texto); }

    public void seleccionarSeccion(String seccion) {
        navBooks.getStyleClass().remove("nav-selected");
        navLoans.getStyleClass().remove("nav-selected");
        navHistory.getStyleClass().remove("nav-selected");
        switch (seccion) {
            case "libros" -> navBooks.getStyleClass().add("nav-selected");
            case "prestamos" -> navLoans.getStyleClass().add("nav-selected");
            case "historial" -> navHistory.getStyleClass().add("nav-selected");
            default -> { }
        }
    }

    private Node crearEncabezado() {
        Label logo = new Label("B"); logo.getStyleClass().add("brand-mark");
        VBox brand = new VBox(2, new Label("BIBLIOTECA"), new Label("Panel de gestión"));
        brand.getStyleClass().add("brand-copy");
        HBox left = new HBox(12, logo, brand); left.setAlignment(Pos.CENTER_LEFT);
        Button config = new Button("⚙  Conexiones"); config.getStyleClass().add("light-button");
        config.setOnAction(event -> alElegirConexiones.run());
        HBox bar = new HBox(left, config); bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(15, 24, 15, 24)); bar.getStyleClass().add("topbar");
        HBox.setHgrow(left, Priority.ALWAYS);
        return bar;
    }

    private Node crearBarraLateral() {
        Label caption = new Label("MENÚ PRINCIPAL"); caption.getStyleClass().add("side-caption");
        VBox box = new VBox(9, caption, navBooks, navLoans, navHistory);
        box.setPadding(new Insets(28, 14, 20, 14)); box.setPrefWidth(220); box.getStyleClass().add("sidebar");
        return box;
    }

    private static Button nav(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        return button;
    }
}
