export interface SignInRequest {
  usernameOrEmailOrPhoneNumber: string;
  password: string;
}

export interface VerifyRequest {
  email: string;
  code: string;
}

export interface CreationRequest {
  username: string;
  email: string;
  password: string;
  phoneNumber: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  phoneNumber: string;
  status: number;
  createdAt: string;
  updatedAt: string;
}
