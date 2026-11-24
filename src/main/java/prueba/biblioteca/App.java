package prueba.biblioteca;

import javafx.application.Application;
import javafx.stage.Stage;
import prueba.biblioteca.controlador.ControladorPrincipal;
import prueba.biblioteca.database.BibliotecaDB;

/** Punto de entrada y composición inicial de la aplicación. */
public final class App extends Application {
    @Override
    public void start(Stage stage) {
        BibliotecaDB modelo = new BibliotecaDB();
        new ControladorPrincipal(modelo).iniciar(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
