# --- Etapa 1: Construcción ---
# Usamos una imagen de Maven con Java 17 para compilar nuestro proyecto
FROM maven:3.8.5-openjdk-17 AS build

# Copiamos todo el código fuente al contenedor
COPY . .

# Ejecutamos el comando de Maven para compilar y empaquetar la aplicación
# '-DskipTests' acelera la construcción al omitir los tests
RUN mvn clean package -DskipTests

# --- Etapa 2: Ejecución ---
# Usamos una imagen ligera de OpenJDK 17 para ejecutar la aplicación
FROM openjdk:17-jdk-slim

# Copiamos solo el archivo .jar compilado de la etapa anterior
COPY --from=build /target/*.jar app.jar

# Exponemos el puerto en el que corre nuestra aplicación Spring Boot
EXPOSE 8080

# Comando que se ejecutará al iniciar el contenedor
ENTRYPOINT ["java","-jar","/app.jar"]