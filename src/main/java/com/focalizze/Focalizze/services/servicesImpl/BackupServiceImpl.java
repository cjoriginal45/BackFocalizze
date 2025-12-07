package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.BackupService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateExcelBackup() throws IOException {
        // 1. Crear el libro de Excel (formato .xlsx)
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ==========================================
            // HOJA 1: USUARIOS
            // ==========================================
            Sheet userSheet = workbook.createSheet("Usuarios");
            List<User> users = userRepository.findAll();

            // Cabecera Usuarios
            String[] userColumns = {"ID", "Username", "Display Name", "Email", "Rol", "Fecha Registro", "Baneado"};
            createHeader(userSheet, userColumns);

            // Llenar Datos Usuarios
            int rowIdx = 1;
            for (User user : users) {
                Row row = userSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getDisplayName());
                row.createCell(3).setCellValue(user.getEmail() != null ? user.getEmail() : "N/A");
                row.createCell(4).setCellValue(user.getRole().toString());
                row.createCell(5).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
                row.createCell(6).setCellValue(user.isBanned() ? "SÍ" : "NO");
            }

            // ==========================================
            // HOJA 2: HILOS PUBLICADOS
            // ==========================================
            Sheet threadSheet = workbook.createSheet("Hilos");
            List<ThreadClass> threads = threadRepository.findAll();

            // Cabecera Hilos
            String[] threadColumns = {"ID", "Autor", "Categoría", "Fecha Publicación", "Likes", "Vistas", "Estado"};
            createHeader(threadSheet, threadColumns);

            // Llenar Datos Hilos
            rowIdx = 1;
            for (ThreadClass thread : threads) {
                Row row = threadSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(thread.getId());

                // Manejo seguro de nulos por si se borró el usuario (aunque no debería pasar por FK)
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

            // Escribir el libro en el flujo de salida
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // Método auxiliar para crear cabeceras con estilo (Negrita)
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
            // Ajustar ancho de columna automáticamente (valor aproximado)
            sheet.setColumnWidth(i, 4000);
        }
    }
}
