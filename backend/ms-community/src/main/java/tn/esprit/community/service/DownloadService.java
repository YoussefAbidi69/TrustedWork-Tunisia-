package tn.esprit.community.service;

import tn.esprit.community.dto.response.CourseDownloadResponse;

public interface DownloadService {
    CourseDownloadResponse downloadCourse(Long courseId);
}
