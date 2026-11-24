package prueba.biblioteca.controlador;

import prueba.biblioteca.database.BibliotecaDB;
import prueba.biblioteca.modelo.Autor;
import prueba.biblioteca.modelo.Categoria;
import prueba.biblioteca.modelo.Libro;

import java.sql.SQLException;
import java.util.List;

/** Reglas de negocio y operaciones del catálogo de libros. */
public final class ControladorLibros {
    /** Acceso al modelo de datos; concentra consultas y escrituras en MySQL. */
    private final BibliotecaDB modelo;

    /** Comparte el modelo principal con el controlador del catálogo. */
    public ControladorLibros(BibliotecaDB modelo) {
        this.modelo = modelo;
    }

    /** Obtiene todos los libros para llenar la tabla de la vista. */
    public List<Libro> listar() throws SQLException { return modelo.libros(); }
    /** Catálogo de autores disponible (útil para otras vistas o consultas). */
    public List<Autor> autores() throws SQLException { return modelo.autores(); }
    /** Catálogo requerido por el formulario de libros. */
    public List<Categoria> categorias() throws SQLException { return modelo.categorias(); }

    /** Valida libro, autor y nacionalidad; después pide al modelo guardar o actualizar el registro. */
    public void guardar(int id, String titulo, String isbn, String nombreAutor,
                        String nacionalidadAutor, int idCategoria) throws SQLException {
        String tituloLimpio = titulo == null ? "" : titulo.trim();
        String isbnOriginal = isbn == null ? "" : isbn.trim();
        String autorLimpio = nombreAutor == null ? "" : nombreAutor.trim();
        String nacionalidadLimpia = nacionalidadAutor == null ? "" : nacionalidadAutor.trim();
        if (tituloLimpio.isEmpty()) throw new IllegalArgumentException("El título es obligatorio.");
        if (tituloLimpio.length() > 150) throw new IllegalArgumentException("El título admite hasta 150 caracteres.");
        if (isbnOriginal.isEmpty()) throw new IllegalArgumentException("El ISBN es obligatorio.");
        if (isbnOriginal.length() > 20) throw new IllegalArgumentException("El ISBN admite hasta 20 caracteres.");
        if (autorLimpio.isEmpty()) throw new IllegalArgumentException("El nombre del autor es obligatorio.");
        if (autorLimpio.length() > 100) throw new IllegalArgumentException("El nombre del autor admite hasta 100 caracteres.");
        if (nacionalidadLimpia.isEmpty()) throw new IllegalArgumentException("La nacionalidad del autor es obligatoria.");
        if (nacionalidadLimpia.length() > 50) throw new IllegalArgumentException("La nacionalidad admite hasta 50 caracteres.");

        // Se permiten guiones y espacios al capturar, pero se guardan fuera del ISBN.
        String isbnNormalizado = isbnOriginal.replaceAll("[\\s-]", "");
        if (!isbnNormalizado.matches("(?:[0-9]{9}[0-9Xx]|[0-9]{13})")) {
            throw new IllegalArgumentException("Escribe un ISBN de 10 o 13 dígitos; puedes incluir guiones o espacios.");
        }
        if (idCategoria <= 0) throw new IllegalArgumentException("Selecciona una categoría.");
        modelo.guardarLibro(id, tituloLimpio, isbnNormalizado, autorLimpio, nacionalidadLimpia, idCategoria);
    }

    /** Elimina el libro seleccionado; la base impide borrarlo si tiene préstamos relacionados. */
    public void eliminar(int id) throws SQLException { modelo.borrarLibro(id); }
}
