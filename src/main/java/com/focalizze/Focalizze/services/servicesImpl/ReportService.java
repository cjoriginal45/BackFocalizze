package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ReportRequestDto;

public interface ReportService {
    void reportUser(String usernameToReport, ReportRequestDto request);


}
