import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ContractSignatureService } from '../../../core/services/contract-signature.service';
import { ContractService } from '../../../core/services/contract.service';
import { AuthService } from '../../../core/services/auth.service';
import { Contract } from '../../../core/models/contract.model';
import { SignatureStatus, SignatureResponse } from '../../../core/models/signature.model';

import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MilestoneService } from '../../../core/services/milestone.service';

@Component({
  selector: 'app-contract-sign',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule
  ],
  templateUrl: './contract-sign.component.html',
  styleUrl: './contract-sign.component.css'
})
export class ContractSignComponent implements OnInit, OnDestroy {
  requestId: string | null = null;
  contractId: number | null = null;
  token: string | null = null;
  contract: any = null;
  milestones: any[] = [];
  signatureStatus: SignatureStatus | null = null;

  // Signature capture
  signatureType: 'TYPED' | 'DRAWN' = 'TYPED';
  signaturePayload: string = '';
  isPublicMode: boolean = false;

  pdfUrl: SafeResourceUrl | null = null;
  private objectUrl: string | null = null;

  loading = true;
  signing = false;
  signed = false;
  error = '';
  successMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private sanitizer: DomSanitizer,
    private signatureService: ContractSignatureService,
    private contractService: ContractService,
    private milestoneService: MilestoneService,
    public authService: AuthService
  ) { }

  get isClient(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'CLIENT';
  }

  get isFreelancer(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'FREELANCER';
  }

  get alreadySigned(): boolean {
    if (!this.signatureStatus) return false;
    if (this.isClient) return this.signatureStatus.clientSigned;
    if (this.isFreelancer) return this.signatureStatus.freelancerSigned;
    return false;
  }

  get bothSigned(): boolean {
    return !!(this.signatureStatus?.clientSigned && this.signatureStatus?.freelancerSigned);
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.params['id'];
    this.token = this.route.snapshot.queryParamMap.get('token');

    // Determine if we are in public mode (requestId from email) or internal mode (contractId)
    if (this.token) {
      this.isPublicMode = true;
      this.requestId = idParam;
      this.loadPublicRequest();
    } else {
      this.isPublicMode = false;
      this.contractId = +idParam;
      this.loadContract();
      this.loadSignatureStatus();
      this.loadPdf(); // ADAPTATION: Load PDF in internal mode too
    }
  }

  private loadPublicRequest(): void {
    if (!this.requestId || !this.token) return;

    this.loading = true;
    this.signatureService.getPublicRequest(this.requestId, this.token).subscribe({
      next: (data) => {
        console.log('Public Request Data:', data);
        if (!data) {
          this.error = "Données invalides reçues du serveur.";
          this.loading = false;
          return;
        }

        // Mapping on the structure confirmed: SigningRequestViewResponse
        if (data && data.snapshot) {
          this.contract = data.snapshot;
          this.milestones = data.snapshot?.milestones || [];
          this.signatureStatus = {
            contractId: data.contractId,
            hash: data.snapshotHash,
            clientSigned: data.snapshot?.clientSigned || false,
            freelancerSigned: data.snapshot?.freelancerSigned || false,
            contractStatus: data.requestStatus
          };
          this.contractId = data.contractId || (this.contract ? this.contract.id : null);

          // Capturer le freelancerId pour la création automatique du projet
          this.captureFreelancerId(data);

          this.loading = false;
          this.loadPdf();
        } else {
          this.error = "Structure de réponse invalide (snapshot manquant).";
          this.loading = false;
          console.error('Missing snapshot in response:', data);
        }
      },
      error: (err) => {
        this.error = "Lien de signature invalide ou expiré.";
        this.loading = false;
        console.error('Public load error:', err);
      }
    });
  }

  /**
   * Capture le freelancerId depuis toutes les sources disponibles et le stocke
   * dans localStorage pour la création automatique du projet après paiement.
   * Source 1 : utilisateur connecté en tant que FREELANCER (fiable).
   * Source 2 : champs possibles dans la réponse du signing request (backend-dependent).
   */
  private captureFreelancerId(publicRequestData?: any): void {
    if (!this.contractId) return;
    const key = `contract_${this.contractId}_freelancer_id`;

    // Source 1 — utilisateur connecté
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser?.role === 'FREELANCER' && authUser?.userId) {
      localStorage.setItem(key, String(authUser.userId));
      console.log('[ContractSign] freelancerId stocké (auth):', authUser.userId);
      return;
    }

    // Source 2 — champs potentiels dans la réponse du signing request
    if (publicRequestData) {
      const d = publicRequestData as any;
      const candidates = [
        d.userId, d.signerUserId, d.freelancerUserId,
        d.signer?.id, d.signer?.userId,
        d.snapshot?.freelancerUserId, d.snapshot?.freelancerId,
        d.user?.id, d.user?.userId
      ];
      const found = candidates.find(v => v != null && Number.isFinite(Number(v)));
      if (found) {
        localStorage.setItem(key, String(Number(found)));
        console.log('[ContractSign] freelancerId stocké (publicRequest):', found);
        return;
      }
      console.log('[ContractSign] Aucun userId trouvé dans la réponse. Champs disponibles:', Object.keys(d));
    }
  }

  ngOnDestroy(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
    }
  }

  private loadContract(): void {
    if (this.contractId === null) return;
    this.contractService.getById(this.contractId).subscribe({
      next: (contract: any) => {
        this.contract = contract;
        // Load milestones to match public model
        if (contract && contract.milestones) {
          this.milestones = contract.milestones;
        } else {
          this.milestoneService.getByContractId(this.contractId!).subscribe((m: any[]) => this.milestones = m);
        }
      },
      error: (err) => {
        console.error('Erreur chargement contrat:', err);
        this.error = 'Impossible de charger les informations du contrat.';
      }
    });
  }

  private loadSignatureStatus(): void {
    if (this.isPublicMode || this.contractId === null) return;
    this.signatureService.getStatus(this.contractId).subscribe({
      next: (status) => {
        this.signatureStatus = status;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur statut signature (Backend 500?) :', err);
        // Don't block loading if status fails, just default to empty/pending
        this.signatureStatus = { 
          contractId: this.contractId || 0,
          hash: '',
          clientSigned: false, 
          freelancerSigned: false,
          contractStatus: 'PENDING'
        };
        this.loading = false;
      }
    });
  }

  private loadPdf(): void {
    if (this.isPublicMode) {
      if (!this.requestId || !this.token) return;
      console.log('Tentative chargement PDF Public...');
      this.signatureService.getPublicDocument(this.requestId, this.token).subscribe({
        next: (blob: Blob) => {
          console.log('PDF Public reçu, taille:', blob.size);
          this.displayPdf(blob);
        },
        error: (err: any) => {
          console.error('Erreur téléchargement PDF Public:', err);
        }
      });
    } else {
      if (!this.contractId) {
        console.warn('loadPdf: contractId manquant');
        return;
      }
      console.log('Chargement PDF interne pour ID:', this.contractId);
      this.signatureService.downloadPdf(this.contractId).subscribe({
        next: (blob: Blob) => {
          console.log('PDF Interne reçu, taille:', blob.size);
          this.displayPdf(blob);
        },
        error: (err: any) => {
          console.error('Erreur téléchargement PDF Authentifié:', err);
        }
      });
    }
  }

  private displayPdf(blob: Blob): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
    }
    this.objectUrl = URL.createObjectURL(blob);
    this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
    console.log('pdfUrl généré et assigné');
  }

  signContract(): void {
    // 1. Capture payload for DRAWN type
    if (this.signatureType === 'DRAWN') {
      const canvas = document.getElementById('signatureCanvas') as HTMLCanvasElement;
      if (canvas) {
        this.signaturePayload = canvas.toDataURL('image/png');
      }
    }

    // 2. Validate payload
    if (!this.signaturePayload || !this.signaturePayload.trim() || this.signaturePayload === 'data:,') {
      this.error = "Veuillez apposer votre signature avant de valider.";
      return;
    }

    this.signing = true;
    this.error = '';
    this.successMessage = '';

    const requestId = this.requestId || this.contractId?.toString() || '';
    const payload = {
      token: this.token || '',
      signatureType: this.signatureType,
      signaturePayload: this.signaturePayload
    };

    console.log('Sending signature payload for type:', this.signatureType);

    this.signatureService.submitPublicSignature(requestId, payload).subscribe({
      next: (res: SignatureResponse) => {
        this.signing = false;
        this.signed = true;
        this.successMessage = res.message || 'Contrat signé avec succès !';

        // Capturer le freelancerId si c'est le freelancer qui vient de signer
        this.captureFreelancerId();

        // Update local status instantly so "État des signatures" modifies instantly upon success
        if (this.signatureStatus) {
            this.signatureStatus.clientSigned = res.clientSigned;
            this.signatureStatus.freelancerSigned = res.freelancerSigned;
            this.signatureStatus.contractStatus = res.contractStatus;
        }

        if (this.isPublicMode) {
          // Delay reload slightly to let user see instant checkmarks
          setTimeout(() => {
            this.loadPublicRequest();
          }, 1000);
        } else {
          this.loadSignatureStatus();
          this.loadPdf(); // Refresh PDF to show signatures if backend supports it
        }

        if (res.contractStatus === 'PENDING_PAYMENT' && !this.isPublicMode) {
          setTimeout(() => {
            this.router.navigate(['/app/finance/checkout', this.contractId!]);
          }, 2500);
        }
      },
      error: (err) => {
        this.signing = false;
        this.error = err.error?.message || 'Erreur lors de la signature.';
      }
    });
  }

  downloadPdf(): void {
    if (this.contractId === null) return;
    this.signatureService.downloadPdf(this.contractId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contrat-${this.contractId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.error = 'Impossible de télécharger le document.';
      }
    });
  }

  goBack(): void {
    if (this.isPublicMode) {
      this.router.navigate(['/']);
    } else {
      this.router.navigate(['/app/activity/contracts', this.contractId]);
    }
  }

  // --- Canvas Drawing Logic ---
  private isDrawing = false;

  startDrawing(event: MouseEvent | TouchEvent): void {
    this.isDrawing = true;
    this.signatureType = 'DRAWN';
    this.draw(event);
  }

  draw(event: MouseEvent | TouchEvent): void {
    if (!this.isDrawing) return;
    const canvas = document.getElementById('signatureCanvas') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const rect = canvas.getBoundingClientRect();
    const x = ('touches' in event) ? event.touches[0].clientX - rect.left : event.clientX - rect.left;
    const y = ('touches' in event) ? event.touches[0].clientY - rect.top : event.clientY - rect.top;

    ctx.lineWidth = 2.5;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#000000';

    if (event.type === 'mousedown' || event.type === 'touchstart') {
      ctx.beginPath();
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
      ctx.stroke();
    }

    // Prevent scrolling when drawing on touch devices
    if ('touches' in event) event.preventDefault();
  }

  stopDrawing(): void {
    this.isDrawing = false;
    const canvas = document.getElementById('signatureCanvas') as HTMLCanvasElement;
    if (canvas) {
      this.signaturePayload = canvas.toDataURL('image/png');
    }
  }

  clearCanvas(): void {
    const canvas = document.getElementById('signatureCanvas') as HTMLCanvasElement;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      ctx?.clearRect(0, 0, canvas.width, canvas.height);
      this.signaturePayload = '';
    }
  }

  setSignatureType(type: 'DRAWN' | 'TYPED'): void {
    this.signatureType = type;
    this.signaturePayload = '';
    if (type === 'DRAWN') {
      setTimeout(() => this.clearCanvas(), 0);
    }
  }


  downloadSignedPdf(): void {
    const filename = `Contrat_${this.contract?.reference || 'Signe'}.pdf`;
    
    // Use the already loaded blob if available (usually preferred)
    if (this.objectUrl) {
      const link = document.createElement('a');
      link.href = this.objectUrl;
      link.download = filename;
      link.click();
    } else {
      // Fallback: request from service
      const id = this.contractId;
      if (!id) return;
      this.signatureService.downloadPdf(id).subscribe({
        next: (blob: Blob) => {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = filename;
          link.click();
          URL.revokeObjectURL(url);
        },
        error: (err: any) => console.error('Erreur téléchargement:', err)
      });
    }
  }

  closeWindow(): void {
    // Tente de fermer la fenêtre
    window.close();
    
    // Si la fenêtre ne se ferme pas (sécurité navigateur), on redirige vers l'accueil
    setTimeout(() => {
      this.router.navigate(['/']);
    }, 500);
  }
}
