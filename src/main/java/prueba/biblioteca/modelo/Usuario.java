package prueba.biblioteca.modelo;

import java.time.LocalDate;

/** Datos de un usuario de biblioteca obtenidos de la base de datos.
 * @param id clave primaria de Usuario
 * @param nombre nombre que aparece en los préstamos
 * @param correoElectronico correo único del usuario
 * @param fechaRegistro día en que se registró en la biblioteca
 */
public record Usuario(int id, String nombre, String correoElectronico, LocalDate fechaRegistro) {
    /** Muestra el nombre cuando el usuario aparece en una lista desplegable. */
    @Override public String toString() { return nombre; }
}
