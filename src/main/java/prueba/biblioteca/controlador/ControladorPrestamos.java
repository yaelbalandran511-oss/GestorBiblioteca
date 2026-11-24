package prueba.biblioteca.controlador;

import prueba.biblioteca.database.BibliotecaDB;
import prueba.biblioteca.modelo.Libro;
import prueba.biblioteca.modelo.Prestamo;
import prueba.biblioteca.modelo.Usuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Reglas de negocio y operaciones del ciclo de vida de los préstamos. */
public final class ControladorPrestamos {
    /** Modelo que ejecuta consultas y escrituras de préstamos. */
    private final BibliotecaDB modelo;

    /** Recibe el modelo compartido por toda la aplicación. */
    public ControladorPrestamos(BibliotecaDB modelo) {
        this.modelo = modelo;
    }

    /** Devuelve préstamos activos (Prestado y Retraso) o todo el historial. */
    public List<Prestamo> listar(boolean historial) throws SQLException { return modelo.prestamos(historial); }
    /** Datos necesarios para elegir quién recibe un libro. */
    public List<Usuario> usuarios() throws SQLException { return modelo.usuarios(); }
    /** Datos necesarios para elegir el libro prestado. */
    public List<Libro> libros() throws SQLException { return modelo.libros(); }

    /** Valida el usuario nuevo, la fecha, el estado y el libro antes de guardar el préstamo. */
    public void guardar(int id, LocalDate fecha, String estado, String nombreUsuario,
                        String correoUsuario, int idLibro) throws SQLException {
        if (fecha == null) throw new IllegalArgumentException("Selecciona la fecha de salida.");
        if (fecha.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha de salida no puede estar en el futuro.");
        if (!"Prestado".equals(estado) && !"Retraso".equals(estado) && !"Devuelto".equals(estado)) {
            throw new IllegalArgumentException("Selecciona un estado válido.");
        }
        String nombreLimpio = nombreUsuario == null ? "" : nombreUsuario.trim();
        String correoLimpio = correoUsuario == null ? "" : correoUsuario.trim().toLowerCase(Locale.ROOT);
        if (nombreLimpio.isEmpty()) throw new IllegalArgumentException("El nombre del usuario es obligatorio.");
        if (nombreLimpio.length() > 100) throw new IllegalArgumentException("El nombre admite hasta 100 caracteres.");
        if (correoLimpio.isEmpty()) throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        if (correoLimpio.length() > 150 || !correoLimpio.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Escribe un correo válido de hasta 150 caracteres.");
        }
        if (idLibro <= 0) throw new IllegalArgumentException("Selecciona un libro.");
        modelo.guardarPrestamo(id, fecha.toString(), estado, nombreLimpio, correoLimpio, idLibro);
    }

    /** Marca el préstamo como devuelto sin borrar su historial. */
    public void registrarDevolucion(int idPrestamo) throws SQLException { modelo.cambiarEstado(idPrestamo, "Devuelto"); }
    /** Elimina un préstamo del historial. */
    public void eliminar(int idPrestamo) throws SQLException { modelo.borrarPrestamo(idPrestamo); }
}
