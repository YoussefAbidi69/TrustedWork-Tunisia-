export interface LoginPayload {
  email: string;
  password: string;
  twoFactorCode?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  email: string;
  role: string;
  twoFactorRequired: boolean;
  message: string;
}

export interface AuthUser {
  userId: number;
  email: string;
  role: string;
}