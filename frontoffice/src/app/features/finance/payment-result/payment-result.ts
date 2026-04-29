import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ContractService } from '../../../core/services/contract.service';
import { ProjectApiService } from '../../project/services/project-api.service';
import { AuthService } from '../../../core/services/auth.service';
import { WalletService } from '../../../core/services/wallet.service';
import { UserService } from '../../../core/services/user.service';
import { Contract } from '../../../core/models/contract.model';
import { Project, CreateProjectDTO } from '../../project/models/project.models';

@Component({
  selector: 'app-payment-result',
  templateUrl: './payment-result.html',
  styleUrls: ['./payment-result.css']
})
export class PaymentResultComponent implements OnInit {
  status: 'success' | 'cancel' = 'success';
  contractId: number | null = null;

  projectCreating = false;
  projectCreated = false;
  projectId: number | null = null;
  projectError = '';

  constructor(
    private route: ActivatedRoute,
    private contractService: ContractService,
    private projectApiService: ProjectApiService,
    private authService: AuthService,
    private walletService: WalletService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      console.log('[PaymentResult] queryParams:', params);
      this.status = params['status'] === 'cancel' ? 'cancel' : 'success';
      this.contractId = params['contractId'] ? Number(params['contractId']) : null;
      console.log('[PaymentResult] status:', this.status, '| contractId:', this.contractId);

      if (this.status === 'success' && this.contractId) {
        this.autoCreateProject(this.contractId);
      }
    });
  }

  private autoCreateProject(contractId: number): void {
    console.log('[PaymentResult] autoCreateProject start, contractId=', contractId);
    this.projectCreating = true;
    this.projectError = '';

    const authUser = this.authService.getCurrentAuthUser();
    const clientId = authUser?.userId;
    console.log('[PaymentResult] clientId from auth:', clientId);

    if (!clientId) {
      this.projectCreating = false;
      this.projectError = 'Utilisateur non connecté.';
      return;
    }

    this.contractService.getById(contractId).subscribe({
      next: (contract: Contract) => {
        console.log('[PaymentResult] contract loaded:', contract);
        const contractAny = contract as any;
        const directId: number | undefined =
          contractAny.freelancerId ?? contractAny.freelancerUserId ??
          contractAny.freelancerUser?.id ?? contractAny.freelancerUser?.userId;
        if (directId) {
          console.log('[PaymentResult] freelancerId from contract fields:', directId);
          this.createProject(contract, clientId, directId);
          return;
        }
        this.resolveFreelancerId(contractId, contract.freelancerCin, (freelancerId) => {
          if (!freelancerId) {
            this.projectCreating = false;
            this.projectError = `Impossible de récupérer l'ID du freelancer (CIN: ${contract.freelancerCin}).`;
            return;
          }
          this.createProject(contract, clientId, freelancerId);
        });
      },
      error: (err: any) => {
        console.error('[PaymentResult] getById contract error:', err);
        this.projectCreating = false;
        this.projectError = `Erreur chargement contrat (${err.status}).`;
      }
    });
  }

  /**
   * Résolution en cascade du freelancerId depuis 3 sources :
   *  1. localStorage  (stocké lors de la signature interne)
   *  2. wallet service (GET /api/v1/wallets/user/{cin} — la réponse JSON contient parfois userId)
   *  3. user service  (GET /api/users/{cin} — peut retourner 500 sur certains envs)
   */
  private resolveFreelancerId(contractId: number, freelancerCin: string, callback: (id: number | null) => void): void {

    // ── Source 1 : localStorage ────────────────────────────────────────────
    const stored = localStorage.getItem(`contract_${contractId}_freelancer_id`);
    if (stored) {
      const id = Number(stored);
      console.log('[PaymentResult] freelancerId from localStorage:', id);
      callback(id);
      return;
    }

    // ── Source 2 : wallet service ──────────────────────────────────────────
    this.walletService.getWallet(freelancerCin).pipe(catchError(() => of(null))).subscribe(wallet => {
      console.log('[PaymentResult] wallet response:', wallet);
      const walletAny = wallet as any;
      const fromWallet: number | undefined =
        walletAny?.userId ?? walletAny?.user?.id ?? walletAny?.user?.userId ?? walletAny?.ownerId;

      if (fromWallet) {
        console.log('[PaymentResult] freelancerId from wallet:', fromWallet);
        callback(fromWallet);
        return;
      }

      // ── Source 3 : user service ────────────────────────────────────────
      this.userService.getUserByCin(freelancerCin).pipe(catchError(() => of(null))).subscribe(user => {
        console.log('[PaymentResult] user response:', user);
        const userAny = user as any;
        const fromUser: number | undefined = userAny?.id ?? userAny?.userId;

        if (fromUser) {
          console.log('[PaymentResult] freelancerId from userService:', fromUser);
          callback(fromUser);
          return;
        }

        console.error('[PaymentResult] Toutes les sources ont échoué pour CIN:', freelancerCin);
        callback(null);
      });
    });
  }

  private createProject(contract: Contract, clientId: number, freelancerId: number): void {
    const dto: CreateProjectDTO = {
      title: contract.projectTitle,
      description: contract.description,
      contractId: contract.id!,
      clientId,
      freelancerId,
      startDate: new Date(contract.dateDebut).toISOString().split('T')[0],
      endDate: new Date(contract.dateFin).toISOString().split('T')[0],
      budget: contract.montantTotal
    };

    console.log('[PaymentResult] createProject DTO:', dto);

    this.projectApiService.createProject(dto).subscribe({
      next: (project: Project) => {
        console.log('[PaymentResult] project created:', project);
        this.projectCreating = false;
        this.projectCreated = true;
        this.projectId = project.id;
        localStorage.removeItem(`contract_${contract.id}_freelancer_id`);
      },
      error: (err: any) => {
        console.error('[PaymentResult] createProject error:', err);
        this.projectCreating = false;
        if (err.status === 409) {
          this.projectError = 'Un projet existe déjà pour ce contrat.';
        } else {
          this.projectError = err.error?.message || `Erreur création projet (${err.status}).`;
        }
      }
    });
  }
}
