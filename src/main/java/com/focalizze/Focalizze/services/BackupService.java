package com.focalizze.Focalizze.services;


import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface BackupService {
    /**
     * Genera un archivo Excel (.xlsx) con los datos actuales del sistema.
     * @return ByteArrayInputStream con el contenido del archivo binario.
     * @throws IOException Si ocurre un error al escribir el archivo.
     */
    ByteArrayInputStream generateExcelBackup() throws IOException;
}
