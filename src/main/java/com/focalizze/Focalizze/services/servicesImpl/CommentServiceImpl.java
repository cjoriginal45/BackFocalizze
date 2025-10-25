package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CommentRequestDto;
import com.focalizze.Focalizze.dto.CommentResponseDto;
import com.focalizze.Focalizze.dto.mappers.CommentMapper;
import com.focalizze.Focalizze.models.CommentClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CommentRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl  implements CommentService {
    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByThread(Long threadId, Pageable pageable) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado"));

        Page<CommentClass> comments = commentRepository.findAllByThreadOrderByCreatedAtAsc(thread, pageable);
        return comments.map(commentMapper::toCommentResponseDto);
    }

    @Override
    @Transactional
    public CommentResponseDto createComment(Long threadId, CommentRequestDto commentRequestDto, User currentUser) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado"));

        CommentClass newComment = CommentClass.builder()
                .content(commentRequestDto.content())
                .user(currentUser)
                .thread(thread)
                .build();

        CommentClass savedComment = commentRepository.save(newComment);

        thread.setCommentCount(thread.getCommentCount() + 1);
        threadRepository.save(thread);

        return commentMapper.toCommentResponseDto(savedComment);
    }
}