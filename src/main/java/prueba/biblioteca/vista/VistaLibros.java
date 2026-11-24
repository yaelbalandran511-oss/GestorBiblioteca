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
    private final ControladorLibros controlador;
    private final IntConsumer alCargar;
    private final ObservableList<Libro> filas = FXCollections.observableArrayList();
    private final TableView<Libro> tabla = new TableView<>();
    private Window owner;

    public VistaLibros(ControladorLibros controlador, IntConsumer alCargar) {
        this.controlador = controlador;
        this.alCargar = alCargar;
        setPadding(new Insets(28, 34, 24, 34)); setSpacing(18);
        construir();
    }

    public void setOwner(Window owner) { this.owner = owner; }
    public void inicializar() { cargar(); }

    private void construir() {
        VBox titleBlock = titulo("Catálogo de libros", "Administra los títulos, autores y categorías.");
        Button agregar = new Button("＋  Agregar libro"); agregar.getStyleClass().add("primary-button");
        HBox titleRow = new HBox(titleBlock, agregar); titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        TextField search = new TextField(); search.setPromptText("Buscar por título, ISBN, autor o categoría...");
        search.getStyleClass().add("search-field");
        tabla.getStyleClass().add("data-table");
        tabla.getColumns().add(col("ID", "id", 70));
        tabla.getColumns().add(col("TÍTULO", "titulo", 240));
        tabla.getColumns().add(col("ISBN", "isbn", 140));
        tabla.getColumns().add(col("AUTOR", "autor", 180));
        tabla.getColumns().add(col("CATEGORÍA", "categoria", 150));
        FilteredList<Libro> filtrados = new FilteredList<>(filas, libro -> true);
        tabla.setItems(filtrados);
        search.textProperty().addListener((observable, anterior, texto) -> filtrados.setPredicate(libro ->
                (libro.getTitulo() + libro.getIsbn() + libro.getAutor() + libro.getCategoria())
                        .toLowerCase().contains(texto.trim().toLowerCase())));

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

    private void cargar() {
        try {
            filas.setAll(controlador.listar());
            alCargar.accept(filas.size());
        } catch (Exception exception) {
            filas.clear(); VistaMensajes.error(owner, exception);
        }
    }

    private void abrirFormulario(Libro seleccionado) {
        try {
            List<Autor> autores = controlador.autores();
            List<Categoria> categorias = controlador.categorias();
            if (autores.isEmpty() || categorias.isEmpty()) {
                VistaMensajes.informacion(owner, "Agrega primero al menos un autor y una categoría en MySQL Workbench.");
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(seleccionado == null ? "Agregar libro" : "Editar libro");
            dialog.setHeaderText(seleccionado == null ? "Datos del libro" : "Actualiza los datos del libro");
            if (owner != null) dialog.initOwner(owner);
            TextField titulo = new TextField(seleccionado == null ? "" : seleccionado.getTitulo());
            TextField isbn = new TextField(seleccionado == null ? "" : seleccionado.getIsbn());
            ComboBox<Autor> autor = new ComboBox<>(FXCollections.observableArrayList(autores));
            ComboBox<Categoria> categoria = new ComboBox<>(FXCollections.observableArrayList(categorias));
            if (seleccionado == null) { autor.getSelectionModel().selectFirst(); categoria.getSelectionModel().selectFirst(); }
            else {
                autores.stream().filter(opcion -> opcion.id() == seleccionado.getIdAutor()).findFirst().ifPresent(autor::setValue);
                categorias.stream().filter(opcion -> opcion.id() == seleccionado.getIdCategoria()).findFirst().ifPresent(categoria::setValue);
            }
            titulo.setPromptText("Título del libro"); isbn.setPromptText("ISBN (único)");
            GridPane form = formulario();
            agregarFila(form, 0, "Título", titulo); agregarFila(form, 1, "ISBN", isbn);
            agregarFila(form, 2, "Autor", autor); agregarFila(form, 3, "Categoría", categoria);
            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            boolean[] guardado = {false};
            dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    if (autor.getValue() == null || categoria.getValue() == null) throw new IllegalArgumentException("Selecciona autor y categoría.");
                    controlador.guardar(seleccionado == null ? 0 : seleccionado.getId(), titulo.getText(), isbn.getText(),
                            autor.getValue().id(), categoria.getValue().id());
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
