package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.comment.mapper.CommentMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class CommentAdminService {

    private final CommentRepository commentRepository;
    private final CommentMapper mapper;

    @Transactional
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с id=" + commentId + " не найден");
        }
        commentRepository.deleteById(commentId);
    }

    public List<CommentDto> getAllComments(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return commentRepository.findAll(pageable)
                .stream()
                .map(mapper::toCommentDto)
                .collect(Collectors.toList());
    }

    public CommentDto getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
        return mapper.toCommentDto(comment);
    }
}