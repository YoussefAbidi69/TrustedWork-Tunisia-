package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.CourseRequest;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.dto.response.CourseResponse;

public interface CourseService {
    CourseResponse createCourse(CourseRequest courseRequest);

    CourseResponse getCourse(Long id);

    List<CourseResponse> listCourses(Long communityId, Boolean publishedOnly);

    CourseResponse updateCourse(Long id, CourseRequest courseRequest);

    void deleteCourse(Long id);

    CourseDownloadResponse downloadCourse(Long courseId);
}
