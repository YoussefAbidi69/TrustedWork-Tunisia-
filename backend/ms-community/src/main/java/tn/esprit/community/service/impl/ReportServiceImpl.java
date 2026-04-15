package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.dto.ReportDTO;
import tn.esprit.community.entity.Report;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.mapper.ReportMapper;
import tn.esprit.community.entity.Enum.ReportStatus;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.ReportRepository;
import tn.esprit.community.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final ReportMapper reportMapper;

    public ReportServiceImpl(
            ReportRepository reportRepository, PostRepository postRepository, ReportMapper reportMapper) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.reportMapper = reportMapper;
    }

    @Override
    public ReportDTO reportPost(Long reportedBy, Long postId, String reason, String description) {
        if (postId == null) {
            throw new PostNotFoundException("Post ID cannot be null");
        }
        Report report = Report.builder()
                .post(postRepository.getReferenceById(postId))
                .reportedBy(reportedBy)
                .reason(reason)
                .description(description)
                .status(ReportStatus.PENDING)
                .build();
        return reportMapper.toDto(reportRepository.save(report));
    }

    @Override
    public ReportDTO adminRestorePost(Long postId) {
        if (postId == null) {
            throw new PostNotFoundException("Post ID cannot be null");
        }
        Report report = reportRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Report not found"));
        report.setStatus(ReportStatus.REVIEWED);
        return reportMapper.toDto(reportRepository.save(report));
    }

    @Override
    public ReportDTO adminRejectPost(Long postId) {
        if (postId == null) {
            throw new PostNotFoundException("Post ID cannot be null");
        }
        Report report = reportRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Report not found"));
        report.setStatus(ReportStatus.REVIEWED);
        return reportMapper.toDto(reportRepository.save(report));
    }
}
