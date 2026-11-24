# Gestor de Biblioteca

Aplicación de escritorio en JavaFX para administrar libros, préstamos e historial. La aplicación sigue una estructura MVC: modelos en `modelo`, pantallas en `vista`, acciones y reglas en `controlador`, y persistencia MySQL en `database`.

## Requisitos

- Java JDK 21 o posterior.
- IntelliJ IDEA o Maven.
- MySQL 8 y MySQL Workbench para crear las bases.

## Instalación en una computadora

1. Clona este repositorio desde GitHub Desktop y abre la carpeta `GestorBiblioteca` en IntelliJ.
2. En MySQL Workbench, abre y ejecuta `database/instalar_bases.sql`. El script crea `gestor_biblioteca` y `gestor_biblioteca_espejo`, sus tablas y datos ficticios de demostración.
3. Espera a que IntelliJ cargue las dependencias de Maven.
4. Ejecuta `prueba.biblioteca.App` desde IntelliJ o, desde la carpeta del proyecto, ejecuta `mvn javafx:run`.
5. En la ventana **Conexiones**, ingresa el host, puerto, usuario y contraseña de MySQL de esa instalación. La configuración queda en `config.properties`, un archivo local ignorado por Git.

URLs JDBC locales de ejemplo:

```text
jdbc:mysql://localhost:3306/gestor_biblioteca?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
jdbc:mysql://localhost:3306/gestor_biblioteca_espejo?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
```

## Usar una base compartida por varias computadoras

`localhost` siempre significa la computadora donde corre la aplicación. Si cada persona instala MySQL localmente, cada quien tendrá su propia copia de los datos aunque escriba el mismo usuario y contraseña.

Para que todos vean los mismos registros, ambas URLs JDBC deben apuntar a un servidor MySQL común usando su nombre de red o IP, y ese servidor debe permitir las conexiones de las computadoras del equipo. Configura un usuario MySQL exclusivo para la aplicación, con permisos solo sobre las dos bases; no uses `root` ni publiques contraseñas en el repositorio.

## Datos de demostración

Los nombres, correos y préstamos incluidos en `instalar_bases.sql` son ficticios. El script usa `INSERT IGNORE`, así que se puede volver a ejecutar sin sobrescribir registros existentes.

