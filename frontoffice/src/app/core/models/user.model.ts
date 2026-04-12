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
  twoFactorEnabled?: boolean;
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