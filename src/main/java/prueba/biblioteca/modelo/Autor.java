package prueba.biblioteca.modelo;

/** Autor registrado en el catálogo. */
public record Autor(int id, String nombre, String nacionalidad) {
    @Override public String toString() { return nombre; }
}
