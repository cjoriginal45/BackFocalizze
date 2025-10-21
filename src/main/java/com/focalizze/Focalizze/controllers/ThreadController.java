package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.services.ThreadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thread")
public class ThreadController {
    private final ThreadService threadService;

    public ThreadController(ThreadService threadService) {
        this.threadService = threadService;
    }

    @PostMapping("/create")
    public ResponseEntity<ThreadResponseDto> createThread(@Valid @RequestBody ThreadRequestDto requestDto) {
        ThreadResponseDto responseDto = threadService.createThread(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
}
