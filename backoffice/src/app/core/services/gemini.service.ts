import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { map, catchError, switchMap, delay } from 'rxjs/operators';
import { DisputeAiRecommendation } from '../models/dispute.model';
import { Dispute } from '../models/dispute.model';

const GEMINI_API_KEY = 'AIzaSyCcPY_h6XgktloF-u6sVF3seGUvVi7lYJE';
const GEMINI_BASE = 'https://generativelanguage.googleapis.com/v1beta/models';

// Modèles disponibles pour cette clé — ordre de priorité
const MODELS = [
  'gemini-2.5-flash',
  'gemini-2.0-flash',
  'gemini-2.0-flash-001',
];

@Injectable({ providedIn: 'root' })
export class GeminiService {

  constructor(private http: HttpClient) {}

  /**
   * Analyse un litige via Gemini AI.
   * Essaie plusieurs modèles en cascade si le quota est dépassé sur l'un d'eux.
   */
  analyzeDispute(dispute: Dispute, contractInfo?: any, milestoneInfo?: any, evidenceFiles?: { mimeType: string, base64: string }[]): Observable<DisputeAiRecommendation> {
    const prompt = this.buildPrompt(dispute, contractInfo, milestoneInfo);
    return this.tryModels(prompt, dispute, 0, evidenceFiles);
  }

  private tryModels(prompt: string, dispute: Dispute, modelIndex: number, evidenceFiles?: { mimeType: string, base64: string }[]): Observable<DisputeAiRecommendation> {
    if (modelIndex >= MODELS.length) {
      // Tous les modèles épuisés → fallback local
      console.warn('[GeminiService] All models quota exceeded, using rule-based fallback');
      return of(this.buildFallback(dispute));
    }

    const model = MODELS[modelIndex];
    const url = `${GEMINI_BASE}/${model}:generateContent?key=${GEMINI_API_KEY}`;
    console.log(`[GeminiService] Trying model: ${model}`);

    const parts: any[] = [];
    
    // Ajout des fichiers multimédias si présents
    if (evidenceFiles && evidenceFiles.length > 0) {
      evidenceFiles.forEach(file => {
        parts.push({
          inlineData: {
            mimeType: file.mimeType,
            data: file.base64
          }
        });
      });
    }

    // Le texte du prompt doit toujours y être
    parts.push({ text: prompt });

    const body = {
      contents: [{ parts: parts }],
      generationConfig: {
        temperature: 0.3,
        maxOutputTokens: 1024,
        // Désactiver le mode "thinking" de Gemini 2.5 pour obtenir une réponse JSON directe
        thinkingConfig: { thinkingBudget: 0 }
      }
    };
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    return this.http.post<any>(url, body, { headers }).pipe(
      map(response => {
        console.log(`[GeminiService] ✅ Success with model: ${model}`);
        return this.parseResponse(response, dispute);
      }),
      catchError(err => {
        const status = err?.status;
        const errMsg = err?.error?.error?.message || '';

        if (status === 429 || status === 503 || status === 404) {
          // Quota dépassé ou modèle non dispo → essaie le suivant
          const retryAfterMs = this.extractRetryAfter(errMsg);
          console.warn(`[GeminiService] Model ${model} failed (${status}), retrying with next model in ${retryAfterMs}ms...`);
          return of(null).pipe(
            delay(retryAfterMs),
            switchMap(() => this.tryModels(prompt, dispute, modelIndex + 1, evidenceFiles))
          );
        }
        // Autre erreur (401, réseau) → fallback immédiat
        console.error(`[GeminiService] Fatal error on model ${model}:`, errMsg);
        return of(this.buildFallback(dispute));
      })
    );
  }

  private extractRetryAfter(errorMessage: string): number {
    // Extrait le délai "retry in X.XXs" depuis le message d'erreur Gemini
    const match = errorMessage.match(/retry in ([\d.]+)s/i);
    if (match) {
      return Math.min(Math.ceil(parseFloat(match[1]) * 1000), 5000); // max 5s d'attente
    }
    return 1000; // défaut : 1 seconde
  }

  private buildPrompt(dispute: Dispute, contract?: any, milestone?: any): string {
    const defender = dispute.preuvesDefense?.trim() || 'Aucune réponse.';
    const plaintiff = dispute.preuvesPlaignant?.trim() || 'Non fourni.';
    
    let milestoneContext = '';
    if (milestone) {
      milestoneContext = `\nJALON CIBLÉ: ${milestone.titre}\nDescription des livrables attendus: ${milestone.description}\nMontant du jalon: ${milestone.montant} DT.`;
    }

    return `Tu es un arbitre expert sur une plateforme freelance tunisienne. Analyse ce litige et réponds UNIQUEMENT en JSON valide (sans backticks):
{
  "suggestedDecision": "RESOLVED_CLIENT"|"RESOLVED_FREELANCER"|"SPLIT"|"DISMISSED",
  "confidenceScore": 0.0-1.0,
  "riskLevel": "LOW"|"MEDIUM"|"HIGH",
  "summary": "2 phrases",
  "reasoning": "3 phrases max",
  "suggestedMontantRembourse": 0,
  "suggestedMontantLibere": 0,
  "keyFactors": ["facteur1","facteur2"]
}

LITIGE #${dispute.id} — Contrat #${dispute.contractId}${milestoneContext}
Motif du plaignant: ${(dispute.motif || '').substring(0, 300)}
Preuves du Plaignant: ${plaintiff.substring(0, 300)}
Réponse du Défendeur: ${defender.substring(0, 300)}
Règle: si le défendeur est silencieux → RESOLVED_CLIENT. Si les deux répondent → SPLIT. Si le défendeur est solide → RESOLVED_FREELANCER. Prête une attention particulière aux livrables attendus du jalon s'ils sont fournis pour juger le bien-fondé du motif.`;
  }

  private parseResponse(response: any, dispute: Dispute): DisputeAiRecommendation {
    // Gemini 2.5 peut retourner plusieurs parts (thinking + réponse)
    // On concatène toutes les parts pour être sûr de trouver le JSON
    const parts = response?.candidates?.[0]?.content?.parts || [];
    const fullText = parts.map((p: any) => p.text || '').join('\n').trim();

    // 1) Nettoyer les backticks markdown
    let text = fullText.replace(/```json\n?/gi, '').replace(/```\n?/g, '');

    // 2) Extraire le premier bloc JSON complet { ... }
    const jsonMatch = text.match(/\{[^\{]*"suggestedDecision"[\s\S]*?\}(?=\s*$|\s*\n)/)
                  || text.match(/\{[\s\S]*\}/);

    if (!jsonMatch) {
      console.warn('[GeminiService] No JSON block found, using field extraction. Text preview:', text.substring(0, 200));
      return { ...this.extractFieldsFromText(text), disputeId: dispute.id!, generatedAt: new Date().toISOString(), fallback: false };
    }

    let jsonStr = jsonMatch[0].trim();

    // 3) Remplacer les sauts de ligne à l'intérieur des valeurs JSON string
    // Approche sûre : parser carrément en supprimant tous les \n qui ne sont PAS entre guillemets de façon sécurisée
    jsonStr = this.sanitizeJsonString(jsonStr);

    let parsed: any = {};
    try {
      parsed = JSON.parse(jsonStr);
      console.log('[GeminiService] ✅ JSON parsed successfully');
    } catch (e) {
      console.warn('[GeminiService] JSON.parse failed, attempting field extraction...');
      parsed = this.extractFieldsFromText(jsonStr);
    }

    return {
      disputeId: dispute.id!,
      suggestedDecision: parsed.suggestedDecision || 'SPLIT',
      confidenceScore: typeof parsed.confidenceScore === 'number' ? parsed.confidenceScore : 0.7,
      riskLevel: parsed.riskLevel || 'MEDIUM',
      summary: parsed.summary || '',
      reasoning: parsed.reasoning || '',
      suggestedMontantRembourse: parsed.suggestedMontantRembourse || 0,
      suggestedMontantLibere: parsed.suggestedMontantLibere || 0,
      keyFactors: Array.isArray(parsed.keyFactors) ? parsed.keyFactors : [],
      generatedAt: new Date().toISOString(),
      fallback: false
    };
  }

  /**
   * Sanitize JSON : remplace les \n et \r à l'intérieur des strings JSON
   * sans toucher à la structure du JSON lui-même.
   */
  private sanitizeJsonString(json: string): string {
    let result = '';
    let inString = false;
    let escape = false;

    for (let i = 0; i < json.length; i++) {
      const ch = json[i];
      if (escape) {
        result += ch;
        escape = false;
        continue;
      }
      if (ch === '\\') { escape = true; result += ch; continue; }
      if (ch === '"') { inString = !inString; result += ch; continue; }
      if (inString && (ch === '\n' || ch === '\r')) {
        result += ' '; // remplace les newlines dans les strings par un espace
        continue;
      }
      result += ch;
    }
    return result;
  }

  /** Extraction de secours field-by-field via regex si JSON.parse échoue totalement */
  private extractFieldsFromText(text: string): any {
    const extract = (key: string) => {
      const match = text.match(new RegExp(`"${key}"\\s*:\\s*"([^"]*)"`, 'i'));
      return match ? match[1] : null;
    };
    const extractNum = (key: string) => {
      const match = text.match(new RegExp(`"${key}"\\s*:\\s*([\\d.]+)`, 'i'));
      return match ? parseFloat(match[1]) : null;
    };
    const extractArr = (key: string) => {
      const match = text.match(new RegExp(`"${key}"\\s*:\\s*\\[([^\\]]+)\\]`, 'i'));
      if (!match) return [];
      return match[1].split(',').map(s => s.replace(/["\s]/g, '').trim()).filter(Boolean);
    };

    return {
      suggestedDecision: extract('suggestedDecision') || 'SPLIT',
      confidenceScore: extractNum('confidenceScore') || 0.7,
      riskLevel: extract('riskLevel') || 'MEDIUM',
      summary: extract('summary') || '',
      reasoning: extract('reasoning') || '',
      suggestedMontantRembourse: extractNum('suggestedMontantRembourse') || 0,
      suggestedMontantLibere: extractNum('suggestedMontantLibere') || 0,
      keyFactors: extractArr('keyFactors')
    };
  }

  /**
   * Fallback basé sur les règles métier si Gemini est inaccessible.
   */
  buildFallback(dispute: Dispute): DisputeAiRecommendation {
    const hasDefense = !!dispute.preuvesDefense?.trim();
    const hasPlaignant = !!dispute.preuvesPlaignant?.trim();

    let suggestedDecision: any = 'RESOLVED_CLIENT';
    let confidenceScore = 0.65;
    let riskLevel: any = 'MEDIUM';
    let reasoning = '';
    let keyFactors: string[] = [];

    if (!hasDefense) {
      suggestedDecision = 'RESOLVED_CLIENT';
      confidenceScore = 0.68;
      riskLevel = 'MEDIUM';
      reasoning = 'Le défendeur n\'a pas fourni de réponse au litige. En l\'absence de contre-arguments, la règle d\'arbitrage standard penche en faveur du plaignant. Une résolution en faveur du client est recommandée.';
      keyFactors = ['Défendeur silencieux', 'Aucune preuve de défense', 'Règle par défaut : faveur au plaignant'];
    } else if (hasPlaignant && hasDefense) {
      suggestedDecision = 'SPLIT';
      confidenceScore = 0.52;
      riskLevel = 'HIGH';
      reasoning = 'Les deux parties ont soumis des arguments et des preuves. La situation est ambiguë et un partage équitable est recommandé pour débloquer l\'escrow. Une analyse approfondie des pièces jointes est conseillée avant la décision finale.';
      keyFactors = ['Arguments contradictoires', 'Preuves présentées des deux côtés', 'Partage équitable recommandé', 'Examen des PJ conseillé'];
    } else {
      suggestedDecision = 'RESOLVED_FREELANCER';
      confidenceScore = 0.58;
      riskLevel = 'MEDIUM';
      reasoning = 'Le défendeur a fourni une réponse au litige avec des preuves. Sans preuves solides du côté du plaignant, la décision penche vers le freelancer.';
      keyFactors = ['Le défendeur a répondu', 'Preuves de défense soumises', 'Plaignant sans contre-preuves'];
    }

    return {
      disputeId: dispute.id!,
      suggestedDecision,
      confidenceScore,
      riskLevel,
      summary: `Analyse intelligente du litige ${dispute.reference || '#' + dispute.id} basée sur les données disponibles.`,
      reasoning,
      suggestedMontantRembourse: 0,
      suggestedMontantLibere: 0,
      keyFactors,
      generatedAt: new Date().toISOString(),
      fallback: true
    };
  }
}
