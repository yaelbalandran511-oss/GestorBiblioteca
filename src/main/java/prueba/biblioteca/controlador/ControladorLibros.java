package prueba.biblioteca.controlador;

import prueba.biblioteca.database.BibliotecaDB;
import prueba.biblioteca.modelo.Autor;
import prueba.biblioteca.modelo.Categoria;
import prueba.biblioteca.modelo.Libro;

import java.sql.SQLException;
import java.util.List;

/** Reglas de negocio y operaciones del catálogo de libros. */
public final class ControladorLibros {
    private final BibliotecaDB modelo;

    public ControladorLibros(BibliotecaDB modelo) {
        this.modelo = modelo;
    }

    public List<Libro> listar() throws SQLException { return modelo.libros(); }
    public List<Autor> autores() throws SQLException { return modelo.autores(); }
    public List<Categoria> categorias() throws SQLException { return modelo.categorias(); }

    public void guardar(int id, String titulo, String isbn, int idAutor, int idCategoria) throws SQLException {
        String tituloLimpio = titulo == null ? "" : titulo.trim();
        String isbnOriginal = isbn == null ? "" : isbn.trim();
        if (tituloLimpio.isEmpty()) throw new IllegalArgumentException("El título es obligatorio.");
        if (tituloLimpio.length() > 150) throw new IllegalArgumentException("El título admite hasta 150 caracteres.");
        if (isbnOriginal.isEmpty()) throw new IllegalArgumentException("El ISBN es obligatorio.");
        if (isbnOriginal.length() > 20) throw new IllegalArgumentException("El ISBN admite hasta 20 caracteres.");

        String isbnNormalizado = isbnOriginal.replaceAll("[\\s-]", "");
        if (!isbnNormalizado.matches("(?:[0-9]{9}[0-9Xx]|[0-9]{13})")) {
            throw new IllegalArgumentException("Escribe un ISBN de 10 o 13 dígitos; puedes incluir guiones o espacios.");
        }
        if (idAutor <= 0 || idCategoria <= 0) throw new IllegalArgumentException("Selecciona un autor y una categoría.");
        modelo.guardarLibro(id, tituloLimpio, isbnNormalizado, idAutor, idCategoria);
    }

    public void eliminar(int id) throws SQLException { modelo.borrarLibro(id); }
}
