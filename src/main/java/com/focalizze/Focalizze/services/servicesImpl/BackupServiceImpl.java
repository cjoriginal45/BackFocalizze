package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.BackupService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Implementation of the {@link BackupService} interface.
 * Generates data exports (Backups) in Excel format.
 * Includes memory optimizations for large datasets.
 * <p>
 * Implementación de la interfaz {@link BackupService}.
 * Genera exportaciones de datos (Backups) en formato Excel.
 * Incluye optimizaciones de memoria para grandes conjuntos de datos.
 */
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private static final int BATCH_SIZE = 1000; // Chunk size for DB fetching / Tamaño del lote para obtención de BD

    /**
     * Generates an Excel file containing all users and threads.
     * Uses streaming API (SXSSF) and pagination to prevent OutOfMemoryErrors.
     * <p>
     * Genera un archivo Excel que contiene todos los usuarios e hilos.
     * Utiliza la API de streaming (SXSSF) y paginación para prevenir errores de falta de memoria.
     *
     * @return A {@link ByteArrayInputStream} with the Excel binary data.
     *         Un {@link ByteArrayInputStream} con los datos binarios de Excel.
     * @throws IOException If an I/O error occurs during generation.
     *                     Si ocurre un error de E/S durante la generación.
     */
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateExcelBackup() throws IOException {
        // Use SXSSFWorkbook for memory efficiency (keeps limited rows in memory, writes rest to disk)
        // Usar SXSSFWorkbook para eficiencia de memoria (mantiene filas limitadas en memoria, escribe el resto en disco)
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ==========================================
            // SHEET 1: USERS / HOJA 1: USUARIOS
            // ==========================================
            Sheet userSheet = workbook.createSheet("Usuarios");
            String[] userColumns = {"ID", "Username", "Display Name", "Email", "Rol", "Fecha Registro", "Baneado"};
            createHeader(userSheet, userColumns);

            processUsersInBatches(userSheet);

            // ==========================================
            // SHEET 2: THREADS / HOJA 2: HILOS
            // ==========================================
            Sheet threadSheet = workbook.createSheet("Hilos");
            String[] threadColumns = {"ID", "Autor", "Categoría", "Fecha Publicación", "Likes", "Vistas", "Estado"};
            createHeader(threadSheet, threadColumns);

            processThreadsInBatches(threadSheet);

            // Write to output stream / Escribir en el flujo de salida
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } finally {
            // Dispose of temporary files on disk created by SXSSF
            // Desechar archivos temporales en disco creados por SXSSF
            workbook.dispose();
            workbook.close();
        }
    }

    /**
     * Fetches and writes users page by page to avoid loading all into memory.
     * <p>
     * Obtiene y escribe usuarios página por página para evitar cargar todos en la memoria.
     */
    private void processUsersInBatches(Sheet sheet) {
        int pageNumber = 0;
        int rowIndex = 1;
        Page<User> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            page = userRepository.findAll(pageable);

            for (User user : page.getContent()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getDisplayName());
                row.createCell(3).setCellValue(user.getEmail() != null ? user.getEmail() : "N/A");
                row.createCell(4).setCellValue(user.getRole().toString());
                row.createCell(5).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
                row.createCell(6).setCellValue(user.isBanned() ? "SÍ" : "NO");
            }
            pageNumber++;
        } while (page.hasNext());
    }

    /**
     * Fetches and writes threads page by page.
     * <p>
     * Obtiene y escribe hilos página por página.
     */
    private void processThreadsInBatches(Sheet sheet) {
        int pageNumber = 0;
        int rowIndex = 1;
        Page<ThreadClass> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            page = threadRepository.findAll(pageable);

            for (ThreadClass thread : page.getContent()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(thread.getId());

                String authorName = thread.getUser() != null ? thread.getUser().getUsername() : "Desconocido";
                row.createCell(1).setCellValue(authorName);

                String categoryName = thread.getCategory() != null ? thread.getCategory().getName() : "General";
                row.createCell(2).setCellValue(categoryName);

                row.createCell(3).setCellValue(thread.getPublishedAt() != null ? thread.getPublishedAt().toString() : "No publicado");
                row.createCell(4).setCellValue(thread.getLikeCount());
                row.createCell(5).setCellValue(thread.getViewCount());

                String estado = thread.isDeleted() ? "ELIMINADO" : (thread.isPublished() ? "PUBLICADO" : "BORRADOR");
                row.createCell(6).setCellValue(estado);
            }
            pageNumber++;
        } while (page.hasNext());
    }

    /**
     * Helper to create styled headers.
     * <p>
     * Auxiliar para crear encabezados con estilo.
     */
    private void createHeader(Sheet sheet, String[] columns) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            // Auto-size approx width / Ancho aproximado automático
            sheet.setColumnWidth(i, 4000);
        }
    }
}
