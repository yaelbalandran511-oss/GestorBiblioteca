# Gestor de Biblioteca

Aplicación de escritorio en JavaFX para administrar el catálogo de libros y los préstamos. Está organizada con el patrón Modelo Vista Controlador (MVC):

- `modelo`: clases que representan libros, autores, usuarios, categorías y préstamos.
- `vista`: pantallas y formularios JavaFX.
- `controlador`: navegación, validaciones y coordinación de acciones.
- `database/BibliotecaDB.java`: consultas y escrituras JDBC en MySQL.

## Requisitos

- Java JDK 21 o posterior.
- IntelliJ IDEA o Maven.
- MySQL 8 y MySQL Workbench.

## Instalación

1. Abre el proyecto en IntelliJ IDEA.
2. En MySQL Workbench, ejecuta `database/instalar_bases.sql`. El script crea `gestor_biblioteca` y `gestor_biblioteca_espejo`, las tablas y datos ficticios de demostración.
3. Espera a que Maven descargue las dependencias.
4. Ejecuta `prueba.biblioteca.App` desde IntelliJ o `mvn javafx:run` desde la carpeta del proyecto.
5. En **Conexiones**, configura las URLs, usuario y contraseña de MySQL. La información se guarda en `config.properties`, archivo local excluido de Git.

URLs JDBC para MySQL local:

```text
jdbc:mysql://localhost:3306/gestor_biblioteca?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
jdbc:mysql://localhost:3306/gestor_biblioteca_espejo?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
```

## Comportamiento principal

- Para agregar un libro se escribe título, ISBN, autor, nacionalidad y categoría. El autor se crea si no existe; la nacionalidad es obligatoria y admite hasta 50 caracteres.
- Al eliminar un libro, también se elimina su autor solo si ya no tiene otros libros asociados. Un préstamo relacionado impide borrar el libro.
- Los estados `Prestado` y `Retraso` cuentan como activos, ocupan el libro y aparecen en la vista de préstamos activos. `Devuelto` queda en el historial.
- Las escrituras se envían a la base espejo cuando está configurada.

## Compartir datos entre computadoras

`localhost` significa la computadora donde se ejecuta la aplicación. Para que varias computadoras vean los mismos registros, sus conexiones deben apuntar a un servidor MySQL común accesible por red. Configura un usuario exclusivo con permisos limitados a estas bases; no uses `root` ni publiques credenciales.

Los nombres, correos y préstamos de demostración son ficticios. El script usa `INSERT IGNORE` para no duplicar los registros de ejemplo cuando se ejecuta nuevamente.
