import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DisputeService } from '../../../../core/services/dispute.service';
import { Dispute, DisputeEvidence, DisputeResolveRequest } from '../../../../core/models/dispute.model';
import { AuthService } from '../../../../core/services/auth.service';
import { forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

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
  respondProof = '';
  responding = false;
  respondFiles: File[] = [];
  respondFileError = '';

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

  constructor(
    private disputeService: DisputeService,
    public authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  get isAdmin(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'ADMIN';
  }

  get isResolvable(): boolean {
    return this.dispute?.status === 'OPEN' || this.dispute?.status === 'RESPONDED' || this.dispute?.status === 'UNDER_REVIEW';
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
      next: (d) => { this.dispute = d; this.loading = false; },
      error: (err: any) => { this.error = 'Impossible de charger le litige.'; this.loading = false; console.error(err); }
    });
  }

  loadEvidence(): void {
    this.disputeService.listEvidence(this.disputeId).subscribe({
      next: (ev) => this.evidences = ev,
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
      next: (res) => {
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

  openRespondModal(): void { 
    this.showRespondModal = true; 
    this.responseText = ''; 
    this.respondProof = '';
    this.respondFiles = [];
    this.respondFileError = '';
  }
  closeRespondModal(): void { this.showRespondModal = false; }

  onRespondFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.respondFileError = '';
    if (input.files) {
      const filesArr = Array.from(input.files);
      if (this.respondFiles.length + filesArr.length > 2) {
        this.respondFileError = 'Vous ne pouvez uploader que 2 fichiers maximum.';
        return;
      }
      for (const file of filesArr) {
        if (file.size > 5 * 1024 * 1024) { // Limit 5MB
          this.respondFileError = `Le fichier ${file.name} dépasse 5 Mo.`;
          return;
        }
      }
      this.respondFiles = [...this.respondFiles, ...filesArr].slice(0, 2);
    }
  }

  removeRespondFile(index: number): void {
    this.respondFiles.splice(index, 1);
  }

  submitResponse(): void {
    if (!this.responseText.trim()) return;
    this.responding = true;
    
    // Combine réponse et preuves texte
    let finalDefenseText = `Réponse : \n${this.responseText.trim()}`;
    if (this.respondProof?.trim()) {
      finalDefenseText += `\n\nPreuves : \n${this.respondProof.trim()}`;
    }

    this.disputeService.respond(this.disputeId, finalDefenseText).pipe(
      switchMap(disputeResp => {
        if (this.respondFiles.length === 0) {
          return of([]);
        }
        const uploadObs = this.respondFiles.map(file => this.disputeService.uploadEvidence(this.disputeId, file));
        return forkJoin(uploadObs);
      })
    ).subscribe({
      next: () => { 
        this.responding = false; 
        this.closeRespondModal(); 
        this.loadDispute(); 
        this.loadEvidence();
      },
      error: (err: any) => { 
        this.responding = false; 
        alert('Erreur: ' + (err.error?.message || 'Serveur indisponible ou erreur d\'upload')); 
      }
    });
  }

  // ─── ASSIGN ──────────────────────────────────────────

  openAssignModal(): void { this.showAssignModal = true; this.arbitreId = ''; }
  closeAssignModal(): void { this.showAssignModal = false; }

  submitAssign(): void {
    this.assigning = true;
    this.disputeService.assign(this.disputeId, this.arbitreId || undefined).subscribe({
      next: () => { this.assigning = false; this.closeAssignModal(); this.loadDispute(); },
      error: (err: any) => { this.assigning = false; alert('Erreur: ' + (err.error?.message || '')); }
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
    this.disputeService.resolve(this.disputeId, this.resolveForm).subscribe({
      next: () => { this.resolving = false; this.closeResolveModal(); this.loadDispute(); },
      error: (err: any) => { this.resolving = false; alert('Erreur: ' + (err.error?.message || '')); }
    });
  }

  // ─── HELPERS ─────────────────────────────────────────

  goBack(): void {
    if (this.dispute?.contractId) {
      this.router.navigate(['/app/activity/disputes'], { queryParams: { contractId: this.dispute.contractId } });
    } else {
      this.router.navigate(['/app/activity/disputes']);
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
}
