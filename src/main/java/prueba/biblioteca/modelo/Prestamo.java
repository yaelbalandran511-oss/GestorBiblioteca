package prueba.biblioteca.modelo;

/** Modelo de una fila de préstamo, con claves relacionadas y nombres para mostrar. */
public class Prestamo {
    // Los IDs conservan las relaciones con Usuario y Libro; el texto muestra sus nombres.
    private final int id, idUsuario, idLibro;
    private final String fecha, estado, usuario, libro;
    /** Crea el registro que se presenta en la tabla de préstamos o historial. */
    public Prestamo(int id, String fecha, String estado, int idUsuario, int idLibro, String usuario, String libro) {
        this.id=id; this.fecha=fecha; this.estado=estado; this.idUsuario=idUsuario; this.idLibro=idLibro; this.usuario=usuario; this.libro=libro;
    }
    // Getters usados por los controles de tabla de JavaFX.
    public int getId(){return id;} public String getFecha(){return fecha;} public String getEstado(){return estado;}
    public String getUsuario(){return usuario;} public String getLibro(){return libro;}
    public int getIdUsuario(){return idUsuario;} public int getIdLibro(){return idLibro;}
}
