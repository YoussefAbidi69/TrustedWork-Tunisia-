package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.service.CourseService;
import tn.esprit.community.service.DownloadService;

@Service
public class DownloadServiceImpl implements DownloadService {
    private final CourseService courseService;

    public DownloadServiceImpl(CourseService courseService) {
        this.courseService = courseService;
    }

    @Override
    public CourseDownloadResponse downloadCourse(Long courseId) {
        return courseService.downloadCourse(courseId);
    }
}
