import random

skills_pool = ["Angular", "React", "Vue.js", "Spring Boot", "Node.js", "Python", "Django", "MySQL", "PostgreSQL", "MongoDB", "Docker", "Kubernetes", "AWS", "DevOps", "Machine Learning", "Data Science", "UI/UX", "Figma", "PHP", "Laravel", "Flutter", "Swift", "Kotlin", "Cybersecurity", "Blockchain"]
locations_pool = ["Tunis", "Sfax", "Sousse", "Monastir", "Ariana", "Ben Arous", "Manouba", "Bizerte", "Nabeul", "Mahdia"]
availabilities = ["FULL_TIME", "PART_TIME", "WEEKENDS", "UNAVAILABLE"]

fnames = ["Amine", "Ahmed", "Mohamed", "Ali", "Sami", "Youssef", "Karim", "Omar", "Hassen", "Khaled", "Faten", "Asma", "Sarra", "Mouna", "Nour", "Rym", "Salma", "Imen", "Hela", "Fatma"]
lnames = ["Trabelsi", "Ben Ali", "Gharbi", "Jelassi", "Ayari", "Ben Salem", "Bouazizi", "Mansouri", "Khemiri", "Mezni", "Touati", "Driss", "Cherif", "Zribi", "Fourati", "Karray", "Hachicha", "Feki", "Masmoudi", "Gargouri"]

agencies = [1, 13, 14, 15, 20]

sql = []

# Generate 200 users
users = []
for i in range(1, 201):
    fname = random.choice(fnames)
    lname = random.choice(lnames)
    email = f"{fname.lower()}.{lname.lower()}.{i}@trustedwork.tn"
    cin = random.randint(10000000, 99999999)
    trust_level = random.choices([1, 2, 3, 4, 5], weights=[0.30, 0.25, 0.20, 0.15, 0.10])[0]
    
    num_skills = random.randint(2, 6)
    u_skills = ",".join(random.sample(skills_pool, num_skills))
    avail = random.choice(availabilities)
    loc = random.choice(locations_pool)
    exp_years = random.randint(0, 10)
    exp = f"{exp_years} years - Developer"
    
    kyc = "APPROVED" if random.random() < 0.6 else "PENDING"
    liveness = 1 if kyc == "APPROVED" else 0
    
    # ID starting from 1000 to avoid conflict with existing users
    uid = 1000 + i
    users.append(uid)
    
    sql.append(f"INSERT INTO users (id, first_name, last_name, email, password, cin, role, trust_level, skills, availability, experience, location, kyc_status, liveness_passed, account_status, account_non_locked, enabled, created_at, updated_at) VALUES ({uid}, '{fname}', '{lname}', '{email}', 'dummy_hash', {cin}, 'FREELANCER', {trust_level}, '{u_skills}', '{avail}', '{exp}', '{loc}', '{kyc}', {liveness}, 'ACTIVE', 1, 1, NOW(), NOW());")

# Generate 500 scores
for i in range(1, 501):
    agency = random.choice(agencies)
    freelancer = random.choice(users)
    
    f1 = round(random.uniform(0, 1), 4)
    f2 = round(random.uniform(0, 1), 4)
    f3 = round(random.uniform(0, 1), 4)
    f4 = round(random.uniform(0, 1), 4)
    f5 = round(random.uniform(0, 1), 4)
    f6 = round(random.uniform(0, 1), 4)
    
    tot = round((f1*0.35) + (f2*0.25) + (f3*0.20) + (f4*0.10) + (f5*0.05) + (f6*0.05), 4)
    
    expl = f"Skill match: {int(f1*100)}% | Trust: Medium | Availability: FULL_TIME | Experience: 3 years | Similar to team: {int(f5*100)}% | Location: Tunis (match)"
    
    sql.append(f"INSERT IGNORE INTO freelancer_recommendation_scores (agency_id, freelancer_id, recommendation_score, skill_match_score, trust_score, availability_score, experience_score, similarity_score, location_score, explanation, computed_at) VALUES ({agency}, {freelancer}, {tot}, {f1}, {f2}, {f3}, {f4}, {f5}, {f6}, '{expl}', NOW());")

with open("seed_ml_dataset.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql))
