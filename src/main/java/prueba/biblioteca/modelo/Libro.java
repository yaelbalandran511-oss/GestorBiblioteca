package prueba.biblioteca.modelo;

public class Libro {
    private final int id;
    private final String titulo, isbn, autor, categoria;
    private final int idAutor, idCategoria;
    public Libro(int id, String titulo, String isbn, int idAutor, int idCategoria, String autor, String categoria) {
        this.id=id; this.titulo=titulo; this.isbn=isbn; this.idAutor=idAutor; this.idCategoria=idCategoria; this.autor=autor; this.categoria=categoria;
    }
    public int getId(){return id;} public String getTitulo(){return titulo;} public String getIsbn(){return isbn;}
    public String getAutor(){return autor;} public String getCategoria(){return categoria;}
    public int getIdAutor(){return idAutor;} public int getIdCategoria(){return idCategoria;}
    @Override public String toString(){return titulo;}
}
