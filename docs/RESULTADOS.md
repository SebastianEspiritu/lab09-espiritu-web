# Resultados comprobados

Fecha: 29 de octubre de 2026, zona America/Lima.

## Entorno real

- JDK Oracle 21.0.12 y Maven 3.9.16, Windows 11, proyecto para IntelliJ IDEA.
- Spring Boot 3.5.16 y JJWT 0.11.5.
- Base existente security_db en localhost:3306. No se ejecutó CREATE DATABASE.
- SELECT VERSION(): **10.4.32-MariaDB**.
- SELECT @@version_comment: **mariadb.org binary distribution**.
- Conexión con MySQL Connector/J; la contraseña vacía y el secreto aleatorio se suministraron al proceso,
  sin guardarlos en el repositorio.

## Ejecuciones

1. Se ejecutó Maven compile al terminar cada etapa de implementación. Todas compilaron.
2. Se ejecutó mvn test: 18 pruebas H2 satisfactorias; 18 externas omitidas por no activar RUN_MYSQL_TESTS.
3. Se ejecutó scripts/Invoke-Lab.ps1 -Action verify contra la base externa.
4. Se corrigió la salida del script para mostrar los logs y se repitió verify.
5. Se añadió identificación explícita del motor y se volvió a verificar.

Última verificación completa registrada: **BUILD SUCCESS**, 36 pruebas, 0 fallos, 0 errores, 0 omitidas.
Finalizó a las 09:07:13 -05:00. Se generó el JAR ejecutable de Spring Boot.
Las repeticiones se realizaron por cambios concretos del script y de la identificación del servidor.

| Suite | Ejecutadas | Fallos | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: |
| AuthenticationIntegrationTest, H2 | 18 | 0 | 0 | 0 |
| MySqlAuthenticationTest, servidor externo MariaDB | 18 | 0 | 0 | 0 |
| Total | 36 | 0 | 0 | 0 |

## Los seis casos obligatorios

Cada caso se ejecutó en ambas suites mediante HTTP real con TestRestTemplate y Tomcat,
después de registrar USER/ADMIN e iniciar sesión para obtener tokens reales.

| Caso | Entrada | HTTP | Cuerpo o comprobación |
| --- | --- | ---: | --- |
| 1 | Ruta /all sin token | 200 | Public Content |
| 2 | Ruta /user sin token | 401 | message: No autorizado |
| 3 | Ruta /user con firma alterada | 401 | Rechazado |
| 4 | Ruta /user con JWT de USER | 200 | User Content |
| 5 | Ruta /admin con JWT de USER | 403 | message: Acceso denegado |
| 6 | Ruta /admin con JWT de ADMIN | 200 | Admin Content |

Las otras 12 pruebas por suite verifican acceso de ADMIN a /user, hash BCrypt y coincidencia de contraseña,
duplicados de username/email, contraseña incorrecta, rol USER predeterminado y formato de respuesta del login,
tokens expirados, sin firma, malformados y de un usuario inexistente, registro inválido y ausencia de cookies de sesión.

## Evidencia local y alcance

Los informes de cada prueba están en target/surefire-reports (XML y texto).
El log de la última ejecución está en target/verification.log.
Estos archivos son artefactos locales excluidos de Git: pueden contener tokens o detalles del entorno
y no se publican. Se documentan aquí los totales y resultados sin copiar secretos.
Las pruebas externas insertan tres usuarios únicos por ejecución; no borran los datos preexistentes.

La colección Postman se validó como JSON y contiene diez solicitudes:
cuatro de preparación y seis casos. No se ejecutó la interfaz de Postman ni Newman.
Los resultados anteriores corresponden a las pruebas Java, no a capturas manuales.

## Limitaciones y pendientes

- El servicio real es MariaDB 10.4.32. Hibernate, a través de MySQL Connector/J, detecta versión 5.5.5
  y emite HHH000511 porque MySQLDialect exige MySQL 8 o superior. Las operaciones probadas funcionaron;
  esto no demuestra compatibilidad completa ni elimina la advertencia.
- Queda pendiente repetir verify sobre un servidor **MySQL 8 real**. No se instaló ni reemplazó ningún servicio.
- Queda pendiente ejecutar la colección en Postman si se requieren capturas para la entrega académica.
- La asignación pública de ADMIN sigue la guía: debe cambiarse antes de un uso de producción.
- No se hicieron pruebas de carga ni despliegue productivo; no son resultados de este laboratorio.
