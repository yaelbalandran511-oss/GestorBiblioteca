package prueba.biblioteca.modelo;

/** Registro de datos de una categoría del catálogo de libros.
 * @param id clave primaria de Categoria
 * @param nombre etiqueta de categoría mostrada en la aplicación
 * @param descripcion texto opcional sobre la categoría
 */
public record Categoria(int id, String nombre, String descripcion) {
    /** Muestra el nombre en los controles de selección de JavaFX. */
    @Override public String toString() { return nombre; }
}
