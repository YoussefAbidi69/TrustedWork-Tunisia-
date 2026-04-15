package tn.esprit.community.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.CommentDTO;
import tn.esprit.community.entity.Comment;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.mapper.CommentMapper;
import tn.esprit.community.repository.CommentRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.service.CommentService;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;

    public CommentServiceImpl(
            CommentRepository commentRepository, PostRepository postRepository, CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentDTO addComment(Long postId, CommentDTO commentDTO) {
        if (postId == null) {
            throw new PostNotFoundException("Post ID cannot be null");
        }
        Comment comment = commentMapper.toEntity(commentDTO);
        comment.setPost(postRepository.getReferenceById(postId));
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    public List<CommentDTO> listComments(Long postId) {
        return commentRepository.findByPost_IdOrderByIdAsc(postId).stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }
}
