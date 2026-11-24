package prueba.biblioteca.modelo;

public class Prestamo {
    private final int id, idUsuario, idLibro;
    private final String fecha, estado, usuario, libro;
    public Prestamo(int id, String fecha, String estado, int idUsuario, int idLibro, String usuario, String libro) {
        this.id=id; this.fecha=fecha; this.estado=estado; this.idUsuario=idUsuario; this.idLibro=idLibro; this.usuario=usuario; this.libro=libro;
    }
    public int getId(){return id;} public String getFecha(){return fecha;} public String getEstado(){return estado;}
    public String getUsuario(){return usuario;} public String getLibro(){return libro;}
    public int getIdUsuario(){return idUsuario;} public int getIdLibro(){return idLibro;}
}
