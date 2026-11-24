package prueba.biblioteca.vista;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import prueba.biblioteca.controlador.ControladorPrestamos;
import prueba.biblioteca.modelo.Libro;
import prueba.biblioteca.modelo.Prestamo;
import prueba.biblioteca.modelo.Usuario;

import java.time.LocalDate;
import java.util.List;
import java.util.function.IntConsumer;

/** Vista para préstamos activos e historial; delega las reglas y persistencia al controlador. */
public final class VistaPrestamos extends VBox {
    private final ControladorPrestamos controlador;
    private final boolean historial;
    private final IntConsumer alCargar;
    private final ObservableList<Prestamo> filas = FXCollections.observableArrayList();
    private final TableView<Prestamo> tabla = new TableView<>();
    private Window owner;

    public VistaPrestamos(ControladorPrestamos controlador, boolean historial, IntConsumer alCargar) {
        this.controlador = controlador;
        this.historial = historial;
        this.alCargar = alCargar;
        setPadding(new Insets(28, 34, 24, 34)); setSpacing(18);
        construir();
    }

    public void setOwner(Window owner) { this.owner = owner; }
    public void inicializar() { cargar(); }

    private void construir() {
        String encabezado = historial ? "Historial de préstamos" : "Préstamos activos";
        String subtitulo = historial ? "Consulta y administra todos los movimientos." : "Registra préstamos y controla devoluciones.";
        VBox titleBlock = titulo(encabezado, subtitulo);
        Button agregar = new Button("＋  Nuevo préstamo"); agregar.getStyleClass().add("primary-button");
        HBox titleRow = new HBox(titleBlock, agregar); titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        TextField search = new TextField(); search.setPromptText("Buscar por usuario, libro, fecha o estado...");
        search.getStyleClass().add("search-field");
        tabla.getStyleClass().add("data-table");
        tabla.getColumns().add(col("ID", "id", 65));
        tabla.getColumns().add(col("FECHA", "fecha", 120));
        tabla.getColumns().add(col("USUARIO", "usuario", 175));
        tabla.getColumns().add(col("LIBRO", "libro", 260));
        tabla.getColumns().add(col("ESTADO", "estado", 110));
        FilteredList<Prestamo> filtrados = new FilteredList<>(filas, prestamo -> true);
        tabla.setItems(filtrados);
        search.textProperty().addListener((observable, anterior, texto) -> filtrados.setPredicate(prestamo ->
                (prestamo.getUsuario() + prestamo.getLibro() + prestamo.getFecha() + prestamo.getEstado())
                        .toLowerCase().contains(texto.trim().toLowerCase())));

        Button editar = new Button("Editar"); editar.getStyleClass().add("secondary-button");
        Button devolver = new Button("Registrar devolución"); devolver.getStyleClass().add("secondary-button");
        Button eliminar = new Button("Eliminar"); eliminar.getStyleClass().add("danger-button");
        devolver.setDisable(historial);
        HBox actions = new HBox(8, editar, devolver, eliminar); actions.setAlignment(Pos.CENTER_RIGHT);
        agregar.setOnAction(event -> abrirFormulario(null));
        editar.setOnAction(event -> {
            Prestamo seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) VistaMensajes.informacion(owner, "Selecciona un préstamo para editar.");
            else abrirFormulario(seleccionado);
        });
        eliminar.setOnAction(event -> eliminarSeleccionado());
        devolver.setOnAction(event -> devolverSeleccionado());

        VBox card = new VBox(12, search, tabla, actions); card.getStyleClass().add("card");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(titleRow, card);
        VBox.setVgrow(card, Priority.ALWAYS);
    }

    private void cargar() {
        try {
            filas.setAll(controlador.listar(historial));
            alCargar.accept(filas.size());
        } catch (Exception exception) {
            filas.clear(); VistaMensajes.error(owner, exception);
        }
    }

    private void devolverSeleccionado() {
        Prestamo seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) { VistaMensajes.informacion(owner, "Selecciona un préstamo activo."); return; }
        if (!"Prestado".equals(seleccionado.getEstado())) {
            VistaMensajes.informacion(owner, "Ese préstamo ya aparece como devuelto."); return;
        }
        try {
            controlador.registrarDevolucion(seleccionado.getId());
            cargar();
            VistaMensajes.informacion(owner, "Devolución registrada.");
        } catch (Exception exception) { VistaMensajes.error(owner, exception); }
    }

    private void eliminarSeleccionado() {
        Prestamo seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) { VistaMensajes.informacion(owner, "Selecciona un préstamo para eliminar."); return; }
        if (!VistaMensajes.confirmar(owner, "¿Eliminar el préstamo seleccionado?")) return;
        try {
            controlador.eliminar(seleccionado.getId()); cargar();
            VistaMensajes.informacion(owner, "Préstamo eliminado.");
        } catch (Exception exception) { VistaMensajes.error(owner, exception); }
    }

    private void abrirFormulario(Prestamo seleccionado) {
        try {
            List<Usuario> usuarios = controlador.usuarios();
            List<Libro> libros = controlador.libros();
            if (usuarios.isEmpty() || libros.isEmpty()) {
                VistaMensajes.informacion(owner, "Necesitas al menos un usuario y un libro antes de crear un préstamo.");
                return;
            }
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(seleccionado == null ? "Nuevo préstamo" : "Editar préstamo");
            dialog.setHeaderText("Completa la información del préstamo");
            if (owner != null) dialog.initOwner(owner);
            ComboBox<Usuario> usuario = new ComboBox<>(FXCollections.observableArrayList(usuarios));
            ComboBox<Libro> libro = new ComboBox<>(FXCollections.observableArrayList(libros));
            usuario.getSelectionModel().selectFirst(); libro.getSelectionModel().selectFirst();
            if (seleccionado != null) {
                usuarios.stream().filter(opcion -> opcion.id() == seleccionado.getIdUsuario()).findFirst().ifPresent(usuario::setValue);
                libros.stream().filter(opcion -> opcion.getId() == seleccionado.getIdLibro()).findFirst().ifPresent(libro::setValue);
            }
            DatePicker fecha = new DatePicker(seleccionado == null ? LocalDate.now() : LocalDate.parse(seleccionado.getFecha()));
            ComboBox<String> estado = new ComboBox<>(FXCollections.observableArrayList("Prestado", "Devuelto"));
            estado.setValue(seleccionado == null ? "Prestado" : seleccionado.getEstado());
            estado.setDisable(seleccionado == null);
            GridPane form = formulario();
            agregarFila(form, 0, "Usuario", usuario); agregarFila(form, 1, "Libro", libro);
            agregarFila(form, 2, "Fecha de salida", fecha); agregarFila(form, 3, "Estado", estado);
            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            boolean[] guardado = {false};
            dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    if (usuario.getValue() == null || libro.getValue() == null) {
                        throw new IllegalArgumentException("Selecciona un usuario y un libro.");
                    }
                    controlador.guardar(seleccionado == null ? 0 : seleccionado.getId(), fecha.getValue(), estado.getValue(),
                            usuario.getValue().id(), libro.getValue().getId());
                    guardado[0] = true; cargar();
                } catch (Exception exception) {
                    event.consume(); VistaMensajes.error(owner, exception);
                }
            });
            dialog.showAndWait();
            if (guardado[0]) VistaMensajes.informacion(owner, "Préstamo guardado correctamente.");
        } catch (Exception exception) { VistaMensajes.error(owner, exception); }
    }

    private static VBox titulo(String title, String subtitle) {
        Label heading = new Label(title); heading.getStyleClass().add("page-title");
        Label description = new Label(subtitle); description.getStyleClass().add("page-subtitle");
        return new VBox(4, heading, description);
    }

    private static <T> TableColumn<T, Object> col(String name, String property, double width) {
        TableColumn<T, Object> column = new TableColumn<>(name);
        column.setCellValueFactory(new PropertyValueFactory<>(property)); column.setPrefWidth(width);
        return column;
    }

    private static GridPane formulario() {
        GridPane form = new GridPane(); form.setHgap(14); form.setVgap(12); form.setPadding(new Insets(18));
        ColumnConstraints label = new ColumnConstraints(); label.setMinWidth(120);
        ColumnConstraints input = new ColumnConstraints(); input.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(label, input); return form;
    }

    private static void agregarFila(GridPane form, int row, String text, javafx.scene.Node input) {
        Label label = new Label(text); label.getStyleClass().add("field-label");
        if (input instanceof javafx.scene.control.Control control) control.setMaxWidth(Double.MAX_VALUE);
        form.add(label, 0, row); form.add(input, 1, row);
    }
}
