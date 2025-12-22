package com.focalizze.Focalizze.services;


import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface BackupService {
    ByteArrayInputStream generateExcelBackup() throws IOException;
}
