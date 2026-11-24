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
import prueba.biblioteca.controlador.ControladorLibros;
import prueba.biblioteca.modelo.Autor;
import prueba.biblioteca.modelo.Categoria;
import prueba.biblioteca.modelo.Libro;

import java.util.List;
import java.util.function.IntConsumer;

/** Vista del catálogo. Solo presenta controles y envía las acciones al controlador. */
public final class VistaLibros extends VBox {
    /** El controlador recibe las acciones y aplica las reglas del catálogo. */
    private final ControladorLibros controlador;
    /** Notifica al panel principal cuántos libros quedaron cargados. */
    private final IntConsumer alCargar;
    /** Lista observable que mantiene sincronizadas la tabla y el resultado de la búsqueda. */
    private final ObservableList<Libro> filas = FXCollections.observableArrayList();
    private final TableView<Libro> tabla = new TableView<>();
    private Window owner;

    /** Configura dependencias y construye los controles visuales. */
    public VistaLibros(ControladorLibros controlador, IntConsumer alCargar) {
        this.controlador = controlador;
        this.alCargar = alCargar;
        setPadding(new Insets(28, 34, 24, 34)); setSpacing(18);
        construir();
    }

    /** Ventana padre para que los diálogos se abran asociados a esta pantalla. */
    public void setOwner(Window owner) { this.owner = owner; }
    /** Carga los libros después de mostrar la vista. */
    public void inicializar() { cargar(); }

    /** Construye el encabezado, buscador, tabla y botones de edición del catálogo. */
    private void construir() {
        VBox titleBlock = titulo("Catálogo de libros", "Administra los títulos, autores y categorías.");
        Button agregar = new Button("＋  Agregar libro"); agregar.getStyleClass().add("primary-button");
        HBox titleRow = new HBox(titleBlock, agregar); titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        TextField search = new TextField(); search.setPromptText("Buscar por título, ISBN, autor o categoría...");
        search.getStyleClass().add("search-field");
        tabla.getStyleClass().add("data-table");
        // Cada columna lee una propiedad del modelo Libro.
        tabla.getColumns().add(col("ID", "id", 70));
        tabla.getColumns().add(col("TÍTULO", "titulo", 240));
        tabla.getColumns().add(col("ISBN", "isbn", 140));
        tabla.getColumns().add(col("AUTOR", "autor", 180));
        tabla.getColumns().add(col("CATEGORÍA", "categoria", 150));
        // Filtra localmente las filas mientras el usuario escribe en el buscador.
        FilteredList<Libro> filtrados = new FilteredList<>(filas, libro -> true);
        tabla.setItems(filtrados);
        search.textProperty().addListener((observable, anterior, texto) -> filtrados.setPredicate(libro ->
                (libro.getTitulo() + libro.getIsbn() + libro.getAutor() + libro.getCategoria())
                        .toLowerCase().contains(texto.trim().toLowerCase())));

        // Los botones trabajan con la fila seleccionada y delegan guardado/eliminación.
        Button editar = new Button("Editar"); editar.getStyleClass().add("secondary-button");
        Button eliminar = new Button("Eliminar"); eliminar.getStyleClass().add("danger-button");
        HBox actions = new HBox(8, editar, eliminar); actions.setAlignment(Pos.CENTER_RIGHT);
        agregar.setOnAction(event -> abrirFormulario(null));
        editar.setOnAction(event -> {
            Libro seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) VistaMensajes.informacion(owner, "Selecciona un libro para editar.");
            else abrirFormulario(seleccionado);
        });
        eliminar.setOnAction(event -> eliminarSeleccionado());

        VBox card = new VBox(12, search, tabla, actions); card.getStyleClass().add("card");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(titleRow, card);
        VBox.setVgrow(card, Priority.ALWAYS);
    }

    /** Consulta el catálogo y actualiza el indicador de registros en la ventana principal. */
    private void cargar() {
        try {
            filas.setAll(controlador.listar());
            alCargar.accept(filas.size());
        } catch (Exception exception) {
            filas.clear(); VistaMensajes.error(owner, exception);
        }
    }

    /** Comparte formulario para agregar un libro nuevo o editar uno existente. */
    private void abrirFormulario(Libro seleccionado) {
        try {
            List<Autor> autores = controlador.autores();
            List<Categoria> categorias = controlador.categorias();
            if (categorias.isEmpty()) {
                VistaMensajes.informacion(owner, "Agrega primero al menos una categoría en MySQL Workbench.");
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(seleccionado == null ? "Agregar libro" : "Editar libro");
            dialog.setHeaderText(seleccionado == null ? "Datos del libro" : "Actualiza los datos del libro");
            if (owner != null) dialog.initOwner(owner);
            TextField titulo = new TextField(seleccionado == null ? "" : seleccionado.getTitulo());
            TextField isbn = new TextField(seleccionado == null ? "" : seleccionado.getIsbn());
            // El campo de autor acepta nombres nuevos; el modelo lo crea si aún no existe.
            TextField autor = new TextField(seleccionado == null ? "" : seleccionado.getAutor());
            TextField nacionalidad = new TextField();
            ComboBox<Categoria> categoria = new ComboBox<>(FXCollections.observableArrayList(categorias));
            // Si el formulario es de edición, conserva la categoría que ya tenía el libro.
            if (seleccionado == null) { categoria.getSelectionModel().selectFirst(); }
            else {
                categorias.stream().filter(opcion -> opcion.id() == seleccionado.getIdCategoria()).findFirst().ifPresent(categoria::setValue);
                autores.stream().filter(opcion -> opcion.id() == seleccionado.getIdAutor()).findFirst()
                        .ifPresent(opcion -> nacionalidad.setText(opcion.nacionalidad() == null ? "" : opcion.nacionalidad()));
            }
            titulo.setPromptText("Título del libro"); isbn.setPromptText("ISBN (único)");
            autor.setPromptText("Escribe cualquier nombre de autor");
            nacionalidad.setPromptText("Escribe la nacionalidad (máximo 50 caracteres)");
            // Al escribir un autor ya conocido se completa su nacionalidad; para un autor nuevo queda libre.
            autor.textProperty().addListener((observable, anterior, nombre) -> {
                Autor existente = autores.stream().filter(opcion -> opcion.nombre().equalsIgnoreCase(nombre.trim()))
                        .findFirst().orElse(null);
                if (existente == null || existente.nacionalidad() == null || existente.nacionalidad().isBlank()) {
                    nacionalidad.setEditable(true);
                    nacionalidad.clear();
                } else {
                    nacionalidad.setText(existente.nacionalidad());
                    nacionalidad.setEditable(false);
                }
            });
            // Si se edita un libro existente, evita sobrescribir la nacionalidad ya registrada del autor.
            if (!nacionalidad.getText().isBlank()) nacionalidad.setEditable(false);
            GridPane form = formulario();
            agregarFila(form, 0, "Título", titulo); agregarFila(form, 1, "ISBN", isbn);
            agregarFila(form, 2, "Autor", autor); agregarFila(form, 3, "Nacionalidad", nacionalidad);
            agregarFila(form, 4, "Categoría", categoria);
            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            boolean[] guardado = {false};
            // Se intercepta Aceptar para validar y guardar; ante un error el diálogo permanece abierto.
            dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    if (categoria.getValue() == null) throw new IllegalArgumentException("Selecciona una categoría.");
                    controlador.guardar(seleccionado == null ? 0 : seleccionado.getId(), titulo.getText(), isbn.getText(),
                            autor.getText(), nacionalidad.getText(), categoria.getValue().id());
                    guardado[0] = true; cargar();
                } catch (Exception exception) {
                    event.consume(); VistaMensajes.error(owner, exception);
                }
            });
            dialog.showAndWait();
            if (guardado[0]) VistaMensajes.informacion(owner, "Libro guardado correctamente.");
        } catch (Exception exception) {
            VistaMensajes.error(owner, exception);
        }
    }

    /** Pide confirmación y solicita al controlador eliminar la fila seleccionada. */
    private void eliminarSeleccionado() {
        Libro seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) { VistaMensajes.informacion(owner, "Selecciona un libro para eliminar."); return; }
        if (!VistaMensajes.confirmar(owner, "¿Eliminar “" + seleccionado.getTitulo() + "” del catálogo?")) return;
        try {
            controlador.eliminar(seleccionado.getId()); cargar();
            VistaMensajes.informacion(owner, "Libro eliminado.");
        } catch (Exception exception) { VistaMensajes.error(owner, exception); }
    }

    private static VBox titulo(String title, String subtitle) {
        Label heading = new Label(title); heading.getStyleClass().add("page-title");
        Label description = new Label(subtitle); description.getStyleClass().add("page-subtitle");
        return new VBox(4, heading, description);
    }

    /** Crea una columna vinculada a una propiedad del objeto y con ancho definido. */
    private static <T> TableColumn<T, Object> col(String name, String property, double width) {
        TableColumn<T, Object> column = new TableColumn<>(name);
        column.setCellValueFactory(new PropertyValueFactory<>(property)); column.setPrefWidth(width);
        return column;
    }

    /** Disposición común de dos columnas para etiquetas e inputs del formulario. */
    private static GridPane formulario() {
        GridPane form = new GridPane(); form.setHgap(14); form.setVgap(12); form.setPadding(new Insets(18));
        ColumnConstraints label = new ColumnConstraints(); label.setMinWidth(120);
        ColumnConstraints input = new ColumnConstraints(); input.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(label, input); return form;
    }

    /** Añade una etiqueta y su control a una fila del formulario. */
    private static void agregarFila(GridPane form, int row, String text, javafx.scene.Node input) {
        Label label = new Label(text); label.getStyleClass().add("field-label");
        if (input instanceof javafx.scene.control.Control control) control.setMaxWidth(Double.MAX_VALUE);
        form.add(label, 0, row); form.add(input, 1, row);
    }
}
