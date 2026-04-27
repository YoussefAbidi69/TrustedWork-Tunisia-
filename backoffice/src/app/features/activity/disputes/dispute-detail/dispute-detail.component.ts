import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DisputeService } from '../../../../core/services/dispute.service';
import { GeminiService } from '../../../../core/services/gemini.service';
import { MilestoneService } from '../../../../core/services/milestone.service';
import { Dispute, DisputeEvidence, DisputeResolveRequest, DisputeAiRecommendation } from '../../../../core/models/dispute.model';
import { AuthService } from '../../../../core/services/auth.service';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-dispute-detail',
  templateUrl: './dispute-detail.component.html',
  styleUrl: './dispute-detail.component.css'
})
export class DisputeDetailComponent implements OnInit {
  dispute: Dispute | null = null;
  evidences: DisputeEvidence[] = [];
  disputeId!: number;
  loading = true;
  error = '';

  // Evidence upload
  selectedFile: File | null = null;
  uploading = false;

  // Respond (defendeur)
  showRespondModal = false;
  responseText = '';
  responding = false;

  // Assign (admin)
  showAssignModal = false;
  arbitreId = '';
  assigning = false;

  // Resolve (admin)
  showResolveModal = false;
  resolving = false;
  resolveForm: DisputeResolveRequest = {
    status: 'RESOLVED_CLIENT',
    decision: '',
    montantRembourse: 0,
    montantLibere: 0
  };

  // ─── AI Analysis ─────────────────────────────────────────
  aiRecommendation: DisputeAiRecommendation | null = null;
  aiLoading = false;
  aiError = '';
  showAiPanel = false;

  constructor(
    private disputeService: DisputeService,
    private geminiService: GeminiService,
    private milestoneService: MilestoneService,
    public authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  get isAdmin(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'ADMIN';
  }

  get isResolvable(): boolean {
    const statusOk = this.dispute?.status === 'OPEN' || this.dispute?.status === 'RESPONDED' || this.dispute?.status === 'UNDER_REVIEW';
    // Dans beaucoup de backends, il faut qu'un arbitre soit assigné pour résoudre
    return !!(statusOk && this.dispute?.arbitreId);
  }

  get canAssign(): boolean {
    return this.isAdmin && !this.isResolved;
  }

  get isResolved(): boolean {
    return this.dispute?.status === 'RESOLVED_CLIENT' ||
           this.dispute?.status === 'RESOLVED_FREELANCER' ||
           this.dispute?.status === 'SPLIT' ||
           this.dispute?.status === 'DISMISSED';
  }

  ngOnInit(): void {
    this.disputeId = +this.route.snapshot.params['disputeId'];
    this.loadDispute();
    this.loadEvidence();
  }

  loadDispute(): void {
    this.loading = true;
    this.disputeService.getById(this.disputeId).subscribe({
      next: (d: any) => { this.dispute = d; this.loading = false; },
      error: (err: any) => { this.error = 'Impossible de charger le litige.'; this.loading = false; console.error(err); }
    });
  }

  loadEvidence(): void {
    this.disputeService.listEvidence(this.disputeId).subscribe({
      next: (ev: any) => this.evidences = ev,
      error: (err: any) => console.error('Evidence load error:', err)
    });
  }

  // ─── EVIDENCE ────────────────────────────────────────

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  uploadEvidence(): void {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.disputeService.uploadEvidence(this.disputeId, this.selectedFile).subscribe({
      next: () => {
        this.uploading = false;
        this.selectedFile = null;
        this.loadEvidence();
      },
      error: (err: any) => {
        this.uploading = false;
        console.error('Upload error:', err);
        alert('Erreur lors de l\'upload : ' + (err.error?.message || 'Serveur indisponible'));
      }
    });
  }

  filenameFrom(contentDisposition: string | null): string | null {
    if (!contentDisposition) return null;
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    return match ? match[1] : null;
  }

  downloadEvidence(evidenceId: number, ev: any = null): void {
    this.disputeService.downloadEvidenceFile(this.disputeId, evidenceId).subscribe({
      next: (res: any) => {
        // Recréer le blob en forçant le format binaire pur pour que le navigateur n'altère pas les octets
        const fileType = res.headers.get('content-type') || ev?.contentType || 'application/octet-stream';
        const finalBlob = new Blob([res.body as BlobPart], { type: fileType });
        
        // --- DEBOGAGE DEMANDÉ PAR LE BACKEND ---
        console.log('--- DEBUG DOWNLOAD ---');
        console.log('1. Evidence ID téléchargé :', evidenceId);
        console.log('2. sizeBytes (API list)   :', ev?.sizeBytes);
        console.log('3. blob.size (reçu)       :', finalBlob.size);
        console.log('4. Header Content-Length  :', res.headers.get('content-length'));
        console.log('5. Header Content-Type    :', res.headers.get('content-type'));
        console.log('6. Header X-File-SHA256   :', res.headers.get('x-file-sha256'));
        console.log('----------------------');

        const a = document.createElement('a');
        a.href = window.URL.createObjectURL(finalBlob);
        
        // Use the header filename first, fallback to ev metadata or default
        const headerFilename = this.filenameFrom(res.headers.get('content-disposition'));
        a.download = headerFilename || ev?.originalFilename || ev?.fileName || `evidence_${evidenceId}.pdf`;
        
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(a.href);
      },
      error: (err: any) => {
        console.error('Download error:', err);
        alert('Erreur lors du téléchargement. Le fichier est peut-être corrompu, expiré ou introuvable (vérifiez console F12).');
      }
    });
  }

  // ─── RESPOND ─────────────────────────────────────────

  openRespondModal(): void { this.showRespondModal = true; this.responseText = ''; }
  closeRespondModal(): void { this.showRespondModal = false; }

  submitResponse(): void {
    if (!this.responseText.trim()) return;
    this.responding = true;
    this.disputeService.respond(this.disputeId, this.responseText).subscribe({
      next: () => { this.responding = false; this.closeRespondModal(); this.loadDispute(); },
      error: (err: any) => { this.responding = false; alert('Erreur: ' + (err.error?.message || 'Serveur indisponible')); }
    });
  }

  // ─── ASSIGN ──────────────────────────────────────────
  
  assignToMe(): void {
    const adminCin = this.authService.getCin();
    if (!adminCin) {
      alert('Erreur: Impossible de récupérer votre identifiant admin.');
      return;
    }
    
    this.assigning = true;
    this.disputeService.assign(this.disputeId, adminCin).subscribe({
      next: () => { 
        this.assigning = false; 
        this.loadDispute(); 
      },
      error: (err: any) => { 
        this.assigning = false; 
        console.error('Auto-assign error:', err);
        alert('Erreur lors de l\'assignation: ' + (err.error?.message || 'Action interdite.')); 
      }
    });
  }

  // ─── RESOLVE ─────────────────────────────────────────

  openResolveModal(): void {
    this.showResolveModal = true;
    this.resolveForm = { status: 'RESOLVED_CLIENT', decision: '', montantRembourse: 0, montantLibere: 0 };
  }
  closeResolveModal(): void { this.showResolveModal = false; }

  submitResolve(): void {
    if (!this.resolveForm.decision.trim()) {
      alert('La décision est obligatoire.');
      return;
    }
    this.resolving = true;
    
    // Forcer la conversion en nombre pour éviter les erreurs de validation backend
    const payload: DisputeResolveRequest = {
      ...this.resolveForm,
      montantRembourse: Number(this.resolveForm.montantRembourse),
      montantLibere: Number(this.resolveForm.montantLibere)
    };

    this.disputeService.resolve(this.disputeId, payload).subscribe({
      next: () => { this.resolving = false; this.closeResolveModal(); this.loadDispute(); },
      error: (err: any) => { 
        this.resolving = false; 
        console.error('Resolve error:', err);
        alert('Erreur: ' + (err.error?.message || 'Action interdite ou erreur serveur (403). Vérifiez que le litige vous est assigné.')); 
      }
    });
  }

  // ─── HELPERS ─────────────────────────────────────────

  goBack(): void {
    if (this.dispute?.contractId) {
      this.router.navigate(['/admin/activity/disputes'], { queryParams: { contractId: this.dispute.contractId } });
    } else {
      this.router.navigate(['/admin/activity/disputes']);
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'open';
      case 'RESPONDED': return 'responded';
      case 'UNDER_REVIEW': return 'review';
      case 'RESOLVED_CLIENT': case 'RESOLVED_FREELANCER': case 'SPLIT': return 'resolved';
      case 'DISMISSED': return 'dismissed';
      default: return 'pending';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'OPEN': return 'Ouvert';
      case 'RESPONDED': return 'Répondu';
      case 'UNDER_REVIEW': return 'En examen';
      case 'RESOLVED_CLIENT': return 'Résolu (Client)';
      case 'RESOLVED_FREELANCER': return 'Résolu (Freelancer)';
      case 'SPLIT': return 'Partagé';
      case 'DISMISSED': return 'Rejeté';
      default: return status;
    }
  }

  // ─── AI ANALYSIS ─────────────────────────────────────────

  runAiAnalysis(): void {
    this.aiLoading = true;
    this.aiError = '';
    this.aiRecommendation = null;
    this.showAiPanel = true;

    if (!this.dispute) {
      this.aiError = 'Litige non chargé.';
      this.aiLoading = false;
      return;
    }

    // Charger le jalon si un milestoneId est présent avant de lancer l'IA
    if (this.dispute.milestoneId) {
      this.milestoneService.getById(this.dispute.milestoneId).subscribe({
        next: (milestone) => {
          this.callGeminiAnalysis(milestone);
        },
        error: (err) => {
          console.warn('Impossible de charger le jalon pour l\'IA, on continue sans le jalon...', err);
          this.callGeminiAnalysis();
        }
      });
    } else {
      this.callGeminiAnalysis();
    }
  }

  private callGeminiAnalysis(milestoneInfo?: any): void {
    // 1. Filtrer les pièces jointes compatibles avec Gemini (Images et PDF)
    // Limité à 3 fichiers max pour éviter les payloads trop lourds (limites HTTP)
    const supportedTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/heic', 'image/heif', 'application/pdf'];
    const filesToAnalyze = this.evidences
      .filter(ev => ev.contentType && supportedTypes.includes(ev.contentType.toLowerCase()))
      .slice(0, 3);

    if (filesToAnalyze.length === 0) {
      this.executeGeminiCall(milestoneInfo, []);
      return;
    }

    // 2. Télécharger les fichiers et les convertir en Base64
    const downloadJobs: Observable<{mimeType: string, base64: string} | null>[] = filesToAnalyze.map(ev => 
      this.disputeService.downloadEvidenceFile(this.disputeId, ev.id!).pipe(
        switchMap(res => {
          const blob = res.body as Blob;
          return this.blobToBase64(blob).pipe(
            map(base64Data => ({
              mimeType: ev.contentType!,
              // Enlever le prefix "data:image/jpeg;base64," pour Gemini
              base64: base64Data.split(',')[1] || base64Data
            }))
          );
        }),
        catchError(err => {
          console.warn(`Failed to process evidence ${ev.id} for AI`, err);
          return of(null);
        })
      )
    );

    forkJoin(downloadJobs).subscribe(results => {
      // Filtrer les éventuels échecs (null)
      const validFiles = results.filter(r => r !== null) as {mimeType: string, base64: string}[];
      this.executeGeminiCall(milestoneInfo, validFiles);
    });
  }

  private blobToBase64(blob: Blob): Observable<string> {
    return new Observable<string>(observer => {
      const reader = new FileReader();
      reader.onloadend = () => {
        observer.next(reader.result as string);
        observer.complete();
      };
      reader.onerror = () => {
        observer.error('Failed to read blob');
      };
      reader.readAsDataURL(blob);
    });
  }

  private executeGeminiCall(milestoneInfo: any, evidenceFiles: {mimeType: string, base64: string}[]): void {
    // Appel Gemini directement depuis le navigateur (contourne le firewall backend)
    // On lui passe le jalon ET les fichiers (multimodalité)
    this.geminiService.analyzeDispute(this.dispute!, undefined, milestoneInfo, evidenceFiles).subscribe({
      next: (rec) => {
        this.aiRecommendation = rec;
        this.aiLoading = false;
      },
      error: (err) => {
        console.warn('[AI] Gemini unreachable or failed, using rule-based fallback:', err);
        // Fallback local si Gemini inaccessible
        this.aiRecommendation = this.geminiService.buildFallback(this.dispute!);
        this.aiLoading = false;
      }
    });
  }

  applyAiSuggestion(): void {
    if (!this.aiRecommendation) return;
    this.resolveForm.status = this.aiRecommendation.suggestedDecision;
    this.resolveForm.montantRembourse = this.aiRecommendation.suggestedMontantRembourse || 0;
    this.resolveForm.montantLibere = this.aiRecommendation.suggestedMontantLibere || 0;
    this.resolveForm.decision = this.aiRecommendation.reasoning;
    this.showResolveModal = true;
  }

  getRiskClass(level: string): string {
    switch (level) {
      case 'LOW': return 'risk-low';
      case 'MEDIUM': return 'risk-medium';
      case 'HIGH': return 'risk-high';
      default: return '';
    }
  }

  getRiskLabel(level: string): string {
    switch (level) {
      case 'LOW': return 'Risque Faible';
      case 'MEDIUM': return 'Risque Modéré';
      case 'HIGH': return 'Risque Élevé';
      default: return level;
    }
  }

  getAiDecisionLabel(decision: string): string {
    switch (decision) {
      case 'RESOLVED_CLIENT': return '⚖️ Faveur Client';
      case 'RESOLVED_FREELANCER': return '⚖️ Faveur Freelancer';
      case 'SPLIT': return '⚖️ Partage';
      case 'DISMISSED': return '⚖️ Rejeté';
      default: return decision;
    }
  }

  getAiDecisionClass(decision: string): string {
    switch (decision) {
      case 'RESOLVED_CLIENT': return 'ai-decision-client';
      case 'RESOLVED_FREELANCER': return 'ai-decision-freelancer';
      case 'SPLIT': return 'ai-decision-split';
      case 'DISMISSED': return 'ai-decision-dismissed';
      default: return '';
    }
  }

  getConfidencePct(): number {
    return Math.round((this.aiRecommendation?.confidenceScore || 0) * 100);
  }
}
