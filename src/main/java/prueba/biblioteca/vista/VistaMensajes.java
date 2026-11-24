package prueba.biblioteca.vista;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.sql.SQLException;

final class VistaMensajes {
    private VistaMensajes() { }

    static void informacion(Window owner, String texto) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, texto, ButtonType.OK);
        alert.setTitle("Biblioteca"); alert.setHeaderText(null);
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
    }

    static void error(Window owner, Exception exception) {
        String message = exception.getMessage() == null ? exception.toString() : exception.getMessage();
        if (exception instanceof SQLException sql && "23000".equals(sql.getSQLState())) {
            String detail = message.toLowerCase();
            if (detail.contains("duplicate entry")) message = "Ya existe un registro con ese ISBN, correo o nombre de categoría.";
            else if (detail.contains("foreign key") || detail.contains("constraint fails")) {
                message = "No se puede eliminar este registro porque tiene datos relacionados.";
            }
        } else if (exception instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) {
            message = "No se pudo conectar con una de las bases de datos. Revisa las conexiones e inténtalo de nuevo.";
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("No se pudo completar"); alert.setHeaderText("Revisa los datos o la conexión");
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
    }

    static boolean confirmar(Window owner, String texto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar acción"); alert.setHeaderText(null);
        if (owner != null) alert.initOwner(owner);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
