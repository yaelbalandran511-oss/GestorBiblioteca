package prueba.biblioteca.controlador;

import prueba.biblioteca.database.BibliotecaDB;
import prueba.biblioteca.modelo.Libro;
import prueba.biblioteca.modelo.Prestamo;
import prueba.biblioteca.modelo.Usuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Reglas de negocio y operaciones del ciclo de vida de los préstamos. */
public final class ControladorPrestamos {
    /** Modelo que ejecuta consultas y escrituras de préstamos. */
    private final BibliotecaDB modelo;

    /** Recibe el modelo compartido por toda la aplicación. */
    public ControladorPrestamos(BibliotecaDB modelo) {
        this.modelo = modelo;
    }

    /** Devuelve los préstamos activos o todo el historial, según la pantalla solicitada. */
    public List<Prestamo> listar(boolean historial) throws SQLException { return modelo.prestamos(historial); }
    /** Datos necesarios para elegir quién recibe un libro. */
    public List<Usuario> usuarios() throws SQLException { return modelo.usuarios(); }
    /** Datos necesarios para elegir el libro prestado. */
    public List<Libro> libros() throws SQLException { return modelo.libros(); }

    /** Revisa fecha, estado y relaciones antes de guardar o editar un préstamo. */
    public void guardar(int id, LocalDate fecha, String estado, int idUsuario, int idLibro) throws SQLException {
        if (fecha == null) throw new IllegalArgumentException("Selecciona la fecha de salida.");
        if (fecha.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha de salida no puede estar en el futuro.");
        if (!"Prestado".equals(estado) && !"Devuelto".equals(estado)) throw new IllegalArgumentException("Selecciona un estado válido.");
        if (idUsuario <= 0 || idLibro <= 0) throw new IllegalArgumentException("Selecciona un usuario y un libro.");
        modelo.guardarPrestamo(id, fecha.toString(), "Prestado".equals(estado) ? 1 : 0, idUsuario, idLibro);
    }

    /** Marca el préstamo como devuelto sin borrar su historial. */
    public void registrarDevolucion(int idPrestamo) throws SQLException { modelo.cambiarEstado(idPrestamo, "Devuelto"); }
    /** Elimina un préstamo del historial. */
    public void eliminar(int idPrestamo) throws SQLException { modelo.borrarPrestamo(idPrestamo); }
}
