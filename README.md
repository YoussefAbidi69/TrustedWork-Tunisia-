Modules Backend (Spring Boot)
1.  Module Gestion des Contrats
Création de Contrat :
Rédaction manuelle : Interface complète pour définir les termes du projet, le budget et les conditions de réalisation.
Brouillon assisté par l'IA : Génération semi-automatique des clauses contractuelles basées sur les besoins du projet.
Gestion des Jalons (Milestones) : Découpage précis du travail avec validation et paiement par étape.
Signature Numérique :
Capture de la signature manuscrite sur le frontend via une interface Canvas.
Incrustation sécurisée des signatures dans les documents officiels via Apache PDFBox.
Piste d'audit : Insertion automatique des métadonnées de signature (Adresse IP, Horodatage précis, Email du signataire) pour une valeur juridique accrue.
Archivage : Stockage centralisé et sécurisé des versions PDF signées des contrats.
2.  Module Litiges & Arbitrage IA
Soumission de Preuves : Système permettant aux deux parties de télécharger des documents, images ou textes pour justifier leur position.
Arbitrage Intelligent : Utilisation de Google Gemini 1.5 Flash pour analyser le contrat original et les preuves fournies afin de suggérer une recommandation d'arbitrage.
Médiation Assistée : Timeline interactive des événements pour faciliter la prise de décision par les administrateurs.
3.  Module Finance & Paiement
Système d'Escrow (Séquestre) : Garantie financière où les fonds sont bloqués de manière sécurisée dès le début d'un jalon et débloqués uniquement après validation.
Intégration Stripe : Gestion des flux monétaires via l'API Stripe (Checkout sécurisé).
Gestion du Wallet : Portefeuille virtuel permettant de suivre les gains, les remboursements et l'historique des transactions.
Facturation Automatique : Génération de reçus PDF téléchargeables après chaque paiement validé.
 Module Notifications
Service SMTP : Notifications automatiques par email pour les signatures de contrats, les rappels de jalons et les résolutions de litiges.
 Module Machine Learning (Python / Flask)
 Système de Recommandation de Talents
Moteur de Matching : Algorithme analysant les compétences requises et le profil des freelancers pour suggérer les meilleures correspondances.
Communication Inter-services : Connexion fluide entre Spring Boot et le module ML via RestTemplate.
 Modules Frontend (Angular)
 Architecture de l'Interface
Core Module : Gestion centralisée de la sécurité (Intercepteurs JWT, Guards de routes).
Activity Module : Interface de gestion des contrats, de suivi des jalons et espace de signature interactive.
Finance Module : Dashboard financier, paiement en ligne et gestion de l'historique transactionnel.
Dispute Module : Centre de résolution de litiges avec vue analytique générée par l'IA.
