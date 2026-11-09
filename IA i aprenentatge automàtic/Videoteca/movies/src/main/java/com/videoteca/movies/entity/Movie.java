package com.videoteca.movies.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Película del Videoteca.
 *
 * <p>Mapa la tabla {@code pelicula} <b>ya existente</b> en la base de datos
 * (puede contener datos históricos de la TMDB):
 * <pre>
 *   id               smallint unsigned  NOT NULL AUTO_INCREMENT (PK)
 *   titulo           varchar(128)       NOT NULL
 *   sinopsis         text               NULL
 *   anio_estreno     year(4)            NULL
 *   duracion_minutos smallint unsigned  NULL
 *   genero_id        smallint unsigned  NOT NULL  (FK lógico hacia `genero`)
 *   director_id      smallint unsigned  NULL      (FK lógico hacia `director`)
 * </pre>
 *
 * <p><b>Decisión de diseño 1 — claves foráneas lógicas:</b> {@code generoId} y
 * {@code directorId} se tratan como valores simples ({@link Long}) y NO como
 * relaciones JPA: el microservicio no quiere acoplarse a las tablas
 * {@code genero}/{@code director}, que pertenecen a otro contexto.
 *
 * <p><b>Decisión de diseño 2 — tipos y {@code ddl-auto: validate}:</b> Hibernate 7
 * valida el esquema comparando códigos JDBC de forma estricta (la tolerancia
 * entre enteros es solo en un sentido: la entidad puede ser más estrecha que la
 * columna, nunca más amplia). La tabla usa {@code smallint} y {@code year}, y el
 * driver MariaDB los informa como {@code Types.SMALLINT} y {@code Types.YEAR}
 * (91), sin JdbcType asociado. Para que la validación pase manteniendo los tipos
 * Java naturales ({@code Long}/{@code Integer}) se usa:
 * <ul>
 *   <li>{@code @JdbcTypeCode(SqlTypes.SMALLINT)} en las columnas smallint: el
 *       código esperado coincide con el real y el binding runtime es seguro
 *       (LongJavaType/IntegerJavaType convierten a short correctamente; los
 *       valores caben en smallint por construcción de la tabla).</li>
 *   <li>{@code columnDefinition = "year(4)"} en {@code anioEstreno}: la
 *       validación cae en la comparación por nombre de tipo SQL ("year" ==
 *       "year") y el runtime sigue usando Integer (el driver convierte).</li>
 * </ul>
 */
@Entity
@Table(name = "pelicula")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 128)
    private String titulo;

    @Column(name = "sinopsis")
    private String sinopsis;

    @Column(name = "anio_estreno", columnDefinition = "year(4)")
    private Integer anioEstreno;

    @Column(name = "duracion_minutos")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer duracionMinutos;

    @Column(name = "genero_id")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Long generoId;

    @Column(name = "director_id")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Long directorId;

    /** Constructor sin argumentos requerido por JPA. */
    protected Movie() {
    }

    public Movie(String titulo,
                 String sinopsis,
                 Integer anioEstreno,
                 Integer duracionMinutos,
                 Long generoId,
                 Long directorId) {
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.anioEstreno = anioEstreno;
        this.duracionMinutos = duracionMinutos;
        this.generoId = generoId;
        this.directorId = directorId;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public Integer getAnioEstreno() {
        return anioEstreno;
    }

    public void setAnioEstreno(Integer anioEstreno) {
        this.anioEstreno = anioEstreno;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Long getGeneroId() {
        return generoId;
    }

    public void setGeneroId(Long generoId) {
        this.generoId = generoId;
    }

    public Long getDirectorId() {
        return directorId;
    }

    public void setDirectorId(Long directorId) {
        this.directorId = directorId;
    }
}
