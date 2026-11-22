Projecte Catan - Spring

Integrants: Victoria Alcazar i Adria Cabrera

Requisits: Java 21, Maven i MySQL.

Crear una base de dades anomenada catan i configurar l'usuari i la contrasenya al fitxer src/main/resources/application.properties.

Per executar el projecte:
./mvnw spring-boot:run

El servidor funciona al port 8081. Per provar els endpoints, podem fer servir
Swagger UI a http://localhost:8081/swagger-ui.html o Postman.

Endpoints: consultar el tauler, crear una partida, tirar els daus, consultar els recursos, construir i passar el torn.
