package prueba.biblioteca.vista;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import prueba.biblioteca.controlador.ControladorConexion;

/** Diálogo para configurar y probar MySQL; no accede directamente al archivo de configuración. */
public final class VistaConexiones {
    /** El controlador valida los datos y los delega al modelo para guardarlos y probarlos. */
    private final ControladorConexion controlador;

    /** Recibe el controlador de configuración de MySQL. */
    public VistaConexiones(ControladorConexion controlador) { this.controlador = controlador; }

    /** Muestra un formulario para la conexión principal y la réplica opcional. */
    public void mostrar(Window owner, Runnable alGuardar) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Conexiones MySQL");
        dialog.setHeaderText("Configura la base principal y, si existe, la base espejo.");
        if (owner != null) dialog.initOwner(owner);

        TextField url = new TextField(controlador.valor("primary.url", "jdbc:mysql://localhost:3306/gestor_biblioteca?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true"));
        TextField usuario = new TextField(controlador.valor("primary.user", "root"));
        // Las contraseñas usan PasswordField para ocultar lo que se escribe en pantalla.
        PasswordField contrasena = new PasswordField(); contrasena.setText(controlador.valor("primary.password", ""));
        TextField urlEspejo = new TextField(controlador.valor("replica.url", ""));
        TextField usuarioEspejo = new TextField(controlador.valor("replica.user", ""));
        PasswordField contrasenaEspejo = new PasswordField(); contrasenaEspejo.setText(controlador.valor("replica.password", ""));

        GridPane form = crearFormulario();
        Label principal = new Label("CONEXIÓN PRINCIPAL"); principal.getStyleClass().add("form-section");
        Label espejo = new Label("CONEXIÓN ESPEJO (OPCIONAL)"); espejo.getStyleClass().add("form-section");
        int row = 0;
        form.add(principal, 0, row++, 2, 1);
        agregarFila(form, row++, "URL JDBC", url); agregarFila(form, row++, "Usuario", usuario);
        agregarFila(form, row++, "Contraseña", contrasena);
        form.add(espejo, 0, row++, 2, 1);
        agregarFila(form, row++, "URL JDBC", urlEspejo); agregarFila(form, row++, "Usuario", usuarioEspejo);
        agregarFila(form, row, "Contraseña", contrasenaEspejo);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        // Solo se cierra el diálogo si guardar y probar las conexiones termina correctamente.
        boolean[] guardado = {false};
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                controlador.guardar(url.getText(), usuario.getText(), contrasena.getText(),
                        urlEspejo.getText(), usuarioEspejo.getText(), contrasenaEspejo.getText());
                guardado[0] = true;
            } catch (Exception exception) {
                event.consume(); VistaMensajes.error(owner, exception);
            }
        });
        dialog.showAndWait();
        if (guardado[0]) {
            VistaMensajes.informacion(owner, "Conexiones probadas y configuración guardada.");
            alGuardar.run();
        }
    }

    /** Disposición de dos columnas para las etiquetas y campos de conexión. */
    private static GridPane crearFormulario() {
        GridPane form = new GridPane(); form.setHgap(14); form.setVgap(12); form.setPadding(new Insets(18));
        ColumnConstraints label = new ColumnConstraints(); label.setMinWidth(120);
        ColumnConstraints input = new ColumnConstraints(); input.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(label, input); return form;
    }

    /** Añade una fila de etiqueta y control con el mismo estilo que los demás formularios. */
    private static void agregarFila(GridPane form, int row, String text, javafx.scene.control.Control input) {
        Label label = new Label(text); label.getStyleClass().add("field-label");
        input.setMaxWidth(Double.MAX_VALUE);
        form.add(label, 0, row); form.add(input, 1, row);
    }
}
