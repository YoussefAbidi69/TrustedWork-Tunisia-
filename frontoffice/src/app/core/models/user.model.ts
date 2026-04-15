export interface ConnectedUserResponse {
  id?: number;
  userId?: number;
  firstName?: string;
  lastName?: string;
  firstname?: string;
  lastname?: string;
  nom?: string;
  prenom?: string;
  fullName?: string;
  email?: string;
  role?: string;
  cin?: string | number;
  phone?: string;
  photo?: string;

  // ✅ nouveaux champs profil
  headline?: string;
  location?: string;
  bio?: string;

  twoFactorEnabled?: boolean;
  trustLevel?: number;
  livenessPassed?: boolean;
  lastLoginAt?: string;
  accountStatus?: string;
  kycStatus?: string;
  updatedAt?: string;
  createdAt?: string;
}

export interface DashboardUser {
  id: number | null;
  fullName: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  cin?: string | number;
}