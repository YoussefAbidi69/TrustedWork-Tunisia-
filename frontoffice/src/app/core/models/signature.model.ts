export interface SignatureStatus {
  contractId: number;
  hash: string;
  clientSigned: boolean;
  freelancerSigned: boolean;
  clientSignedAt?: string;
  freelancerSignedAt?: string;
  contractStatus: string;
}

export interface SignatureResponse {
  message: string;
  contractStatus: string;
  clientSigned: boolean;
  freelancerSigned: boolean;
}
