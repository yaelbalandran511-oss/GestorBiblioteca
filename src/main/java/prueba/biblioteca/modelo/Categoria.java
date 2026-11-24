package prueba.biblioteca.modelo;

/** Categoría que organiza los libros. */
public record Categoria(int id, String nombre, String descripcion) {
    @Override public String toString() { return nombre; }
}
