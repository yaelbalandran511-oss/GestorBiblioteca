package prueba.biblioteca.modelo;

import java.time.LocalDate;

/** Usuario de biblioteca. */
public record Usuario(int id, String nombre, String correoElectronico, LocalDate fechaRegistro) {
    @Override public String toString() { return nombre; }
}
