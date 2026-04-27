package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.SectionRequest;
import tn.esprit.community.dto.response.SectionResponse;

public interface SectionService {
    SectionResponse createSection(Long courseId, SectionRequest sectionRequest);

    SectionResponse updateSection(Long sectionId, SectionRequest sectionRequest);

    List<SectionResponse> listSections(Long courseId);

    SectionResponse getSection(Long sectionId);

    void deleteSection(Long sectionId);
}
