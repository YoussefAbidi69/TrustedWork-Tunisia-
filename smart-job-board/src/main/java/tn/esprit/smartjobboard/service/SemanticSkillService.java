package tn.esprit.smartjobboard.service;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Semantic similarity map between related skills; expands sets before Jaccard-style overlap.
 */
@Service
public class SemanticSkillService {

    private final Map<String, Set<String>> neighbors = new HashMap<>();

    @PostConstruct
    void buildGraph() {
        linkBidirectional("react", "next.js");
        linkBidirectional("javascript", "typescript");
        linkBidirectional("sql", "postgresql");
        linkBidirectional("docker", "kubernetes");
        linkBidirectional("aws", "azure");
        linkBidirectional("aws", "gcp");
        linkBidirectional("azure", "gcp");
        linkBidirectional("python", "django");
        linkBidirectional("python", "fastapi");
        linkBidirectional("django", "fastapi");
        linkBidirectional("java", "spring boot");
        linkBidirectional("node.js", "express");
        linkBidirectional("vue", "nuxt");
        linkBidirectional("flutter", "react native");
        linkBidirectional("mongodb", "mongoose");
        linkBidirectional("kafka", "rabbitmq");
    }

    private void linkBidirectional(String a, String b) {
        addNeighbor(a, b);
        addNeighbor(b, a);
    }

    private void addNeighbor(String from, String to) {
        neighbors.computeIfAbsent(norm(from), k -> new HashSet<>()).add(norm(to));
    }

    /**
     * Normalizes a skill label for graph lookup.
     */
    public String norm(String skill) {
        if (skill == null) {
            return "";
        }
        return skill.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Expands a set of skills with semantic neighbors (including originals).
     */
    public Set<String> expand(Collection<String> skills) {
        Set<String> expanded = new HashSet<>();
        for (String s : skills) {
            String n = norm(s);
            if (n.isEmpty()) {
                continue;
            }
            expanded.add(n);
            expanded.addAll(neighbors.getOrDefault(n, Set.of()));
        }
        return expanded;
    }

    /**
     * Jaccard similarity 0–100 between two skill lists after semantic expansion.
     */
    public double skillMatchPercent(Collection<String> required, Collection<String> candidate) {
        Set<String> a = expand(required);
        Set<String> b = expand(candidate);
        if (a.isEmpty() && b.isEmpty()) {
            return 50.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (inter.size() * 100.0) / union.size();
    }
}
