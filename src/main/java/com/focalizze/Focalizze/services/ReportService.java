package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.ReportRequestDto;

public interface ReportService {
    void reportUser(String usernameToReport, ReportRequestDto request);

    void reportThread(Long threadId, ReportRequestDto request);
}
