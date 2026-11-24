package prueba.biblioteca.modelo;

/** Objeto del modelo que reúne datos del libro y los nombres asociados para la interfaz. */
public class Libro {
    // IDs enlazan las claves foráneas; los nombres se usan para presentar filas y formularios.
    private final int id;
    private final String titulo, isbn, autor, categoria;
    private final int idAutor, idCategoria;
    /** Construye una fila de libro con sus IDs y nombres legibles de autor y categoría. */
    public Libro(int id, String titulo, String isbn, int idAutor, int idCategoria, String autor, String categoria) {
        this.id=id; this.titulo=titulo; this.isbn=isbn; this.idAutor=idAutor; this.idCategoria=idCategoria; this.autor=autor; this.categoria=categoria;
    }
    // Getters con los nombres de propiedad que JavaFX TableView puede leer.
    public int getId(){return id;} public String getTitulo(){return titulo;} public String getIsbn(){return isbn;}
    public String getAutor(){return autor;} public String getCategoria(){return categoria;}
    public int getIdAutor(){return idAutor;} public int getIdCategoria(){return idCategoria;}
    /** Muestra el título si JavaFX necesita representar el libro como texto. */
    @Override public String toString(){return titulo;}
}
