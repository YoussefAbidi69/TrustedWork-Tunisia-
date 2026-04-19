package tn.esprit.userservice.entity;

/**
 * Agency-scoped role stored in the agency_members table.
 * OBSERVER is NOT a stored role — a user is implicitly an observer
 * when no AgencyMember row exists for that (user, agency) pair.
 */
public enum MemberRole {
    LEAD,
    MEMBER
}