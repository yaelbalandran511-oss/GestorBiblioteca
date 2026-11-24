package prueba.biblioteca.modelo;

/** Registro de datos de un autor; se usa para mostrar la información leída de MySQL.
 * @param id clave primaria de Autor
 * @param nombre nombre que se muestra junto al libro
 * @param nacionalidad dato opcional del autor
 */
public record Autor(int id, String nombre, String nacionalidad) {
    /** Muestra el nombre cuando JavaFX usa el autor en un control de selección. */
    @Override public String toString() { return nombre; }
}
