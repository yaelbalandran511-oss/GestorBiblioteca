-- Crea la base principal y una base espejo para duplicar los cambios del programa.
CREATE DATABASE IF NOT EXISTS gestor_biblioteca
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS gestor_biblioteca_espejo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Estructura de la base principal.
USE gestor_biblioteca;
-- Personas que pueden recibir libros prestados.
CREATE TABLE IF NOT EXISTS Usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo_electronico VARCHAR(150) NOT NULL UNIQUE,
    fecha_registro DATE NOT NULL
) ENGINE=InnoDB;
-- Autores del catálogo. Un libro nuevo puede crear aquí un autor escrito desde la aplicación.
CREATE TABLE IF NOT EXISTS Autor (
    id_autor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    nacionalidad VARCHAR(50)
) ENGINE=InnoDB;
-- Categorías disponibles para clasificar los libros.
CREATE TABLE IF NOT EXISTS Categoria (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;
-- Catálogo de libros; las claves foráneas enlazan con Autor y Categoria.
CREATE TABLE IF NOT EXISTS Libro (
    id_libro INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    id_autor INT NOT NULL,
    id_categoria INT NOT NULL,
    CONSTRAINT fk_libro_autor FOREIGN KEY (id_autor) REFERENCES Autor(id_autor) ON DELETE RESTRICT,
    CONSTRAINT fk_libro_categoria FOREIGN KEY (id_categoria) REFERENCES Categoria(id_categoria) ON DELETE RESTRICT
) ENGINE=InnoDB;
-- Movimientos de préstamo; RESTRICT evita borrar personas/libros que tengan relaciones activas.
CREATE TABLE IF NOT EXISTS Prestamo (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    fecha_salida DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Prestado',
    id_usuario INT NOT NULL,
    id_libro INT NOT NULL,
    CONSTRAINT fk_prestamo_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_prestamo_libro FOREIGN KEY (id_libro) REFERENCES Libro(id_libro) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Se repite la misma estructura en la base espejo para mantener IDs y relaciones compatibles.
USE gestor_biblioteca_espejo;
-- Copia de la tabla de usuarios de la base principal.
CREATE TABLE IF NOT EXISTS Usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo_electronico VARCHAR(150) NOT NULL UNIQUE,
    fecha_registro DATE NOT NULL
) ENGINE=InnoDB;
-- Copia de la tabla de autores.
CREATE TABLE IF NOT EXISTS Autor (
    id_autor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    nacionalidad VARCHAR(50)
) ENGINE=InnoDB;
-- Copia de la tabla de categorías.
CREATE TABLE IF NOT EXISTS Categoria (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;
-- Copia del catálogo y sus relaciones con autor y categoría.
CREATE TABLE IF NOT EXISTS Libro (
    id_libro INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    id_autor INT NOT NULL,
    id_categoria INT NOT NULL,
    CONSTRAINT fk_libro_autor FOREIGN KEY (id_autor) REFERENCES Autor(id_autor) ON DELETE RESTRICT,
    CONSTRAINT fk_libro_categoria FOREIGN KEY (id_categoria) REFERENCES Categoria(id_categoria) ON DELETE RESTRICT
) ENGINE=InnoDB;
-- Copia de los préstamos y sus relaciones con usuario y libro.
CREATE TABLE IF NOT EXISTS Prestamo (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    fecha_salida DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Prestado',
    id_usuario INT NOT NULL,
    id_libro INT NOT NULL,
    CONSTRAINT fk_prestamo_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_prestamo_libro FOREIGN KEY (id_libro) REFERENCES Libro(id_libro) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Datos de ejemplo para probar búsquedas y relaciones entre autores y libros.
INSERT IGNORE INTO gestor_biblioteca.Autor(id_autor,nombre,nacionalidad) VALUES
 (1,'Gabriel García Márquez','Colombiana'),(2,'Isabel Allende','Chilena'),
 (3,'Ray Bradbury','Estadounidense'),(4,'Elena Poniatowska','Mexicana'),
 (5,'Juan Rulfo','Mexicana'),(6,'Julio Verne','Francesa'),
 (7,'Jane Austen','Británica'),(8,'Antoine de Saint-Exupéry','Francesa');
-- Los mismos IDs en el espejo permiten que cada libro conserve sus relaciones al duplicarse.
INSERT IGNORE INTO gestor_biblioteca_espejo.Autor(id_autor,nombre,nacionalidad) VALUES
 (1,'Gabriel García Márquez','Colombiana'),(2,'Isabel Allende','Chilena'),
 (3,'Ray Bradbury','Estadounidense'),(4,'Elena Poniatowska','Mexicana'),
 (5,'Juan Rulfo','Mexicana'),(6,'Julio Verne','Francesa'),
 (7,'Jane Austen','Británica'),(8,'Antoine de Saint-Exupéry','Francesa');

-- Categorías de ejemplo; IGNORE permite ejecutar el script otra vez sin duplicar claves únicas.
INSERT IGNORE INTO gestor_biblioteca.Categoria(id_categoria,nombre,descripcion) VALUES
 (1,'Novela','Narrativa de ficción'),(2,'Ciencia ficción','Historias especulativas'),
 (3,'Literatura latinoamericana','Obras de autores latinoamericanos'),
 (4,'Cuento','Relatos breves'),(5,'Aventura','Narraciones de exploración y viajes'),
 (6,'Clásicos','Obras reconocidas de la literatura');
INSERT IGNORE INTO gestor_biblioteca_espejo.Categoria(id_categoria,nombre,descripcion) VALUES
 (1,'Novela','Narrativa de ficción'),(2,'Ciencia ficción','Historias especulativas'),
 (3,'Literatura latinoamericana','Obras de autores latinoamericanos'),
 (4,'Cuento','Relatos breves'),(5,'Aventura','Narraciones de exploración y viajes'),
 (6,'Clásicos','Obras reconocidas de la literatura');

-- Usuarios ficticios para ensayar el alta de préstamos.
INSERT IGNORE INTO gestor_biblioteca.Usuario(id_usuario,nombre,correo_electronico,fecha_registro) VALUES
 (1,'Ana Sofía Martínez López','ana.martinez@example.com',CURRENT_DATE),
 (2,'Carlos Eduardo Ramírez Torres','carlos.ramirez@example.com',CURRENT_DATE),
 (3,'Valeria Hernández Soto','valeria.hernandez@example.com',CURRENT_DATE),
 (4,'Diego Alejandro Flores Ruiz','diego.flores@example.com',CURRENT_DATE),
 (5,'Mariana Isabel Torres Vega','mariana.torres@example.com',CURRENT_DATE),
 (6,'José Manuel Castillo Cruz','jose.castillo@example.com',CURRENT_DATE),
 (7,'Fernanda López Medina','fernanda.lopez@example.com',CURRENT_DATE),
 (8,'Andrés Felipe Navarro Díaz','andres.navarro@example.com',CURRENT_DATE);
INSERT IGNORE INTO gestor_biblioteca_espejo.Usuario(id_usuario,nombre,correo_electronico,fecha_registro) VALUES
 (1,'Ana Sofía Martínez López','ana.martinez@example.com',CURRENT_DATE),
 (2,'Carlos Eduardo Ramírez Torres','carlos.ramirez@example.com',CURRENT_DATE),
 (3,'Valeria Hernández Soto','valeria.hernandez@example.com',CURRENT_DATE),
 (4,'Diego Alejandro Flores Ruiz','diego.flores@example.com',CURRENT_DATE),
 (5,'Mariana Isabel Torres Vega','mariana.torres@example.com',CURRENT_DATE),
 (6,'José Manuel Castillo Cruz','jose.castillo@example.com',CURRENT_DATE),
 (7,'Fernanda López Medina','fernanda.lopez@example.com',CURRENT_DATE),
 (8,'Andrés Felipe Navarro Díaz','andres.navarro@example.com',CURRENT_DATE);

-- Libros de muestra; sus IDs de autor y categoría deben coincidir con las tablas anteriores.
INSERT IGNORE INTO gestor_biblioteca.Libro(id_libro,titulo,isbn,id_autor,id_categoria) VALUES
 (1,'Cien años de soledad','9780307474728',1,1),
 (2,'La casa de los espíritus','9788401352836',2,1),
 (3,'Fahrenheit 451','9781451673319',3,2),
 (4,'La noche de Tlatelolco','9786074450893',4,3),
 (5,'El llano en llamas','9786073121054',5,4),
 (6,'La vuelta al mundo en 80 días','9788420674259',6,5),
 (7,'Orgullo y prejuicio','9780141439518',7,6),
 (8,'El principito','9780156012195',8,6);
INSERT IGNORE INTO gestor_biblioteca_espejo.Libro(id_libro,titulo,isbn,id_autor,id_categoria) VALUES
 (1,'Cien años de soledad','9780307474728',1,1),
 (2,'La casa de los espíritus','9788401352836',2,1),
 (3,'Fahrenheit 451','9781451673319',3,2),
 (4,'La noche de Tlatelolco','9786074450893',4,3),
 (5,'El llano en llamas','9786073121054',5,4),
 (6,'La vuelta al mundo en 80 días','9788420674259',6,5),
 (7,'Orgullo y prejuicio','9780141439518',7,6),
 (8,'El principito','9780156012195',8,6);

-- Préstamos de ejemplo con fechas relativas al día en que se ejecuta este script.
INSERT IGNORE INTO gestor_biblioteca.Prestamo(id_prestamo,fecha_salida,estado,id_usuario,id_libro) VALUES
 (1,CURRENT_DATE - INTERVAL 2 DAY,'Prestado',1,1),
 (2,CURRENT_DATE - INTERVAL 10 DAY,'Devuelto',2,2),
 (3,CURRENT_DATE,'Prestado',3,3),
 (4,CURRENT_DATE - INTERVAL 1 DAY,'Prestado',4,4),
 (5,CURRENT_DATE - INTERVAL 20 DAY,'Devuelto',5,5),
 (6,CURRENT_DATE - INTERVAL 3 DAY,'Prestado',6,6),
 (7,CURRENT_DATE - INTERVAL 30 DAY,'Devuelto',7,7),
 (8,CURRENT_DATE,'Prestado',8,8);
INSERT IGNORE INTO gestor_biblioteca_espejo.Prestamo(id_prestamo,fecha_salida,estado,id_usuario,id_libro) VALUES
 (1,CURRENT_DATE - INTERVAL 2 DAY,'Prestado',1,1),
 (2,CURRENT_DATE - INTERVAL 10 DAY,'Devuelto',2,2),
 (3,CURRENT_DATE,'Prestado',3,3),
 (4,CURRENT_DATE - INTERVAL 1 DAY,'Prestado',4,4),
 (5,CURRENT_DATE - INTERVAL 20 DAY,'Devuelto',5,5),
 (6,CURRENT_DATE - INTERVAL 3 DAY,'Prestado',6,6),
 (7,CURRENT_DATE - INTERVAL 30 DAY,'Devuelto',7,7),
 (8,CURRENT_DATE,'Prestado',8,8);
