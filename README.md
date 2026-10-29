# lab09-espiritu-web

Laboratorio 09 de Tecsup: **Autenticación JWT**, estudiante SebastianEspiritu.
Implementación basada en GLAB-S09-RCOELLO-2026-01-3.docx, revisado completo con sus siete imágenes.

## Requisitos e instalación

- IntelliJ IDEA y JDK 21. Proyecto Maven: abrir `pom.xml` y seleccionar Java 21 como SDK.
- Maven 3.6.3 o superior en PATH (verificado con 3.9.16).
- Servidor MySQL con una base existente llamada `security_db`, accesible por TCP.
- Spring Boot 3.5.16, Spring Security 6, JPA y JJWT 0.11.5. Java 21 está soportado por esta línea de Boot.

```powershell
git clone https://github.com/SebastianEspiritu/lab09-espiritu-web.git
cd lab09-espiritu-web
java -version
mvn -version
```

La base `security_db` ya fue creada por el estudiante y no se vuelve a crear desde la aplicación.
Para una instalación nueva, el administrador debe crear previamente esa base y un usuario con permisos
para consultar y modificar datos, crear tablas e índices y actualizar el esquema.
Hibernate usa `ddl-auto=update` como la guía; crea `users`, `roles` y `user_roles`.
`RoleInitializer` agrega USER, ADMIN y MODERATOR si faltan; no borra datos.

## Configuración por entorno

| Variable | Valor o uso |
| --- | --- |
| DB_URL | Opcional: `jdbc:mysql://localhost:3306/security_db` |
| DB_USERNAME | Obligatoria al ejecutar directamente; el script usa root si falta |
| DB_PASSWORD | Contraseña del servidor; vacía para el entorno local autorizado |
| JWT_SECRET | Obligatoria al ejecutar directamente, mínimo 32 bytes UTF-8 |
| JWT_EXPIRATION | Opcional, duración en milisegundos: 3600000 |
| RUN_MYSQL_TESTS | `true` activa las pruebas sobre la base externa |

No hay credenciales ni claves JWT reales en los archivos versionados. Spring Boot no carga automáticamente
un archivo `.env`. Las variables deben pertenecer al proceso que inicia Java.

En la terminal PowerShell de IntelliJ:

```powershell
.\scripts\Invoke-Lab.ps1 -Action run
```

El script prepara un bloque de entorno para Maven y sus procesos hijos: usuario root si no se indicó otro,
contraseña vacía si no está definida y secreto aleatorio criptográfico de 32 bytes codificado en Base64
si no existe JWT_SECRET. No imprime ni guarda esos valores. Conserva las variables que ya estén definidas.
La aplicación se sirve en `http://localhost:8080`.
Detener con Ctrl+C desde la terminal; verificar que el proceso Java haya finalizado.

Si se configura otro servidor, definir las variables antes de invocar el script; por ejemplo,
`$env:DB_URL = 'jdbc:mysql://localhost:3306/security_db'`.
Introducir la contraseña requerida en el entorno local, sin escribirla en archivos del repositorio.
Al reiniciar con una clave generada automáticamente, los tokens anteriores dejan de ser válidos.

Para ejecutar directamente desde IntelliJ, usar `SecurityJwtApplication.main`, JDK 21,
y definir DB_USERNAME, DB_PASSWORD y JWT_SECRET en **Run > Edit Configurations > Environment variables**.
No versionar esa configuración personal. La alternativa recomendada es el script en la terminal integrada.

## Endpoints de la guía

| Método | Ruta | Acceso | Resultado satisfactorio |
| --- | --- | --- | --- |
| POST | /api/auth/signup | Público | Mensaje de registro |
| POST | /api/auth/signin | Público | JWT y datos del usuario |
| GET | /api/test/all | Público | Public Content |
| GET | /api/test/user | USER o ADMIN | User Content |
| GET | /api/test/admin | ADMIN | Admin Content |

Solicitud de registro del ejemplo del profesor (contraseña de demostración, no una credencial real):

```json
{
  "username": "admin",
  "email": "admin@test.com",
  "password": "123456",
  "roles": ["ADMIN"]
}
```

Respuesta: `{"message":"Usuario registrado correctamente"}`.
Para USER usar otro username y email, y `"roles":["USER"]`. Sin roles se asigna USER.
Si el ejemplo ya fue registrado, se obtiene 400; usar datos distintos.
El login recibe email y password, sin username:

```json
{"email":"admin@test.com","password":"123456"}
```

La respuesta contiene `id`, `username`, `email`, `roles`, `accessToken` y `tokenType`.
Como la guía, `username` en esta respuesta corresponde al email utilizado por Spring Security.
El identificador depende de los datos existentes y el token se genera en cada autenticación.
Enviar `Authorization: Bearer <accessToken>` para las rutas protegidas.

Pruebas manuales desde PowerShell, con la aplicación encendida:

```powershell
Invoke-WebRequest http://localhost:8080/api/test/all
# El login usa los datos de prueba registrados anteriormente.
$body = @{ email = 'admin@test.com'; password = '123456' } | ConvertTo-Json
$login = Invoke-RestMethod http://localhost:8080/api/auth/signin -Method Post -ContentType 'application/json' -Body $body
Invoke-WebRequest http://localhost:8080/api/test/admin -Headers @{ Authorization = "Bearer $($login.accessToken)" }
```

## Pruebas automatizadas y Postman

```powershell
# H2 aislada: no necesita servicio de base de datos.
mvn test

# H2 + base externa configurada por entorno; compila, prueba y empaqueta.
.\scripts\Invoke-Lab.ps1 -Action verify
```

Sin RUN_MYSQL_TESTS, Maven ejecuta 18 pruebas H2 y omite 18 externas deliberadamente.
Con el script verify ejecuta las 36. Las pruebas levantan HTTP real con Tomcat en puertos aleatorios,
registran usuarios, hacen login y usan los JWT generados; no simulan la autenticación.
En la base externa se conservan tres usuarios por ejecución, con sufijos UUID y contraseñas aleatorias
almacenadas exclusivamente como hash BCrypt. No se eliminan registros existentes.
H2 usa una base en memoria y `create-drop` únicamente en el perfil test.

Importar `postman/Lab09-JWT.postman_collection.json` en Postman y ejecutar toda la colección en orden
con Collection Runner. Incluye cuatro solicitudes de preparación y los seis casos, con aserciones HTTP.
Genera usuarios y contraseña de prueba distintos por ejecución y captura los tokens automáticamente.
No exportar luego valores de ejecución que contengan tokens.

| Caso | Solicitud | Resultado comprobado por HTTP automatizado |
| --- | --- | --- |
| 1 | GET /api/test/all, sin token | 200, Public Content |
| 2 | GET /api/test/user, sin token | 401, No autorizado |
| 3 | GET /api/test/user, firma JWT alterada | 401, No autorizado |
| 4 | GET /api/test/user, token USER válido | 200, User Content |
| 5 | GET /api/test/admin, token USER válido | 403, Acceso denegado |
| 6 | GET /api/test/admin, token ADMIN válido | 200, Admin Content |

El JAR se genera en `target/lab09-security-jwt-0.0.1-SNAPSHOT.jar`.
Para usar `java -jar`, preparar previamente las mismas variables de entorno.
Los informes locales están en `target/surefire-reports/`; target está excluido de Git.

## Estructura y decisiones respecto de la guía

- `config`: SecurityConfig y RoleInitializer.
- `controller`: AuthController, TestController y ApiExceptionHandler.
- `dto`: SignupRequest, LoginRequest, JwtResponse y MessageResponse.
- `entity`: User y Role.
- `jwt`: JwtUtils y JwtAuthFilter.
- `repository`: UserRepository y RoleRepository.
- `service`: UserDetailsImpl y UserDetailsServiceImpl.
- `src/test`: contrato HTTP común y ejecuciones H2/base externa.

La captura usa Java 17 y Boot 4.0.6; este proyecto usa Java 21 según lo solicitado y Boot 3.5.16
para conservar las APIs del código de la guía. Se conserva JJWT 0.11.5 explícitamente.
La guía omite el permiso de `/api/test/all`: se añade para cumplir su caso público.
También faltan los registros iniciales de roles: se agregan de forma idempotente.
Se configuran respuestas JSON 401/403 explícitas, validación de entrada, restricciones únicas
y rechazo de JWT sin firma o expiración. Se evita registrar el filtro dos veces.

La guía incluye la regla MODERATOR para `/api/test/mod`, pero no define un método del controller
para esa ruta. Se conserva la regla y el rol, sin inventar un endpoint.
El registro público acepta ADMIN/MODERATOR como indica el ejercicio; cualquier visitante puede solicitar
esos privilegios. Para producción se debe restringir esa asignación a administradores o a aprovisionamiento
controlado. BCrypt realiza hash de contraseñas; no es cifrado reversible.

## Resultados y entrega

Ver [resultados y limitaciones](docs/RESULTADOS.md) y [respuestas, observaciones y conclusiones](docs/ANALISIS.md).
Se comprobó que el servidor local es **MariaDB 10.4.32**, accesible mediante MySQL Connector/J:
no se debe presentar esta ejecución como una validación de MySQL 8.
La colección Postman está preparada, pero su ejecución en la interfaz de Postman queda pendiente.
