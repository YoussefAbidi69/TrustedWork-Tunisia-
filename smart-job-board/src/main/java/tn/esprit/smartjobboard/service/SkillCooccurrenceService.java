package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.entity.SkillCooccurrence;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.SkillCooccurrenceRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rebuilds pairwise skill co-occurrence statistics from all published offers.
 */
@Service
@RequiredArgsConstructor
public class SkillCooccurrenceService {

    private final JobOfferRepository jobOfferRepository;
    private final SkillCooccurrenceRepository skillCooccurrenceRepository;
    private final MatchingEngineService matchingEngineService;

    @Transactional
    public void rebuildFromPublishedJobs() {
        skillCooccurrenceRepository.deleteAll();
        List<JobOffer> jobs = jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED);
        List<SkillCooccurrence> batch = new ArrayList<>();
        for (JobOffer j : jobs) {
            Set<String> norm = normalizedSkillSet(j);
            List<String> list = new ArrayList<>(new TreeSet<>(norm));
            for (int i = 0; i < list.size(); i++) {
                for (int k = i + 1; k < list.size(); k++) {
                    String a = list.get(i);
                    String b = list.get(k);
                    String p = a.compareTo(b) <= 0 ? a : b;
                    String r = a.compareTo(b) <= 0 ? b : a;
                    SkillCooccurrence sc = new SkillCooccurrence();
                    sc.setSkillPrimary(p);
                    sc.setSkillRelated(r);
                    sc.setCoCount(1);
                    batch.add(sc);
                }
            }
        }
        mergeAndSave(batch);
    }

    private void mergeAndSave(List<SkillCooccurrence> flat) {
        java.util.Map<String, SkillCooccurrence> merged = new java.util.HashMap<>();
        for (SkillCooccurrence s : flat) {
            String key = s.getSkillPrimary() + "\0" + s.getSkillRelated();
            merged.merge(key, s, (a, b) -> {
                a.setCoCount(a.getCoCount() + b.getCoCount());
                return a;
            });
        }
        skillCooccurrenceRepository.saveAll(merged.values());
    }

    private Set<String> normalizedSkillSet(JobOffer j) {
        Set<String> set = new LinkedHashSet<>();
        List<String> merged = matchingEngineService.mergeJobSkills(j);
        for (String s : merged) {
            if (s != null && !s.isBlank()) {
                set.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }
}
