# --- Etapa 1: Construcción ---
# Usamos una imagen de Maven actualizada basada en Eclipse Temurin (más estable)
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Establecemos un directorio de trabajo para mantener el orden
WORKDIR /app

# Copiamos el código
COPY . .

# Compilamos
RUN mvn clean package -DskipTests

# --- Etapa 2: Ejecución ---
# REEMPLAZO: Usamos eclipse-temurin en lugar de openjdk (que está deprecado)
# 'alpine' es la versión ultra ligera (equivalente a slim)
FROM eclipse-temurin:17-jdk-alpine

# Directorio de trabajo en el contenedor final
WORKDIR /app

# Copiamos el JAR. Notar que agregamos "/app/" porque definimos el WORKDIR arriba
COPY --from=build /app/target/*.jar app.jar

# Creamos el directorio para las imágenes (para que no falle el FileStorageService)
RUN mkdir -p uploads

# Exponemos el puerto
EXPOSE 8080

# Ejecutamos
ENTRYPOINT ["java", "-jar", "app.jar"]