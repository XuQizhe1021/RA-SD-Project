import http from './http'
import type { ApiResponse, ManagedAccountRecord, RegistrationReviewRecord } from '../types/api'

export interface StudentRegisterPayload {
  username: string
  password: string
  displayName: string
  gender?: string
  companyName: string
  jobTitle?: string
  educationLevel?: string
  techLevel?: string
  phone: string
  email?: string
}

export interface RegistrationReviewPayload {
  approved: boolean
  reviewComment?: string
}

export interface InternalAccountCreatePayload {
  username: string
  password: string
  displayName: string
  roleCode: string
  phone?: string
  email?: string
}

export async function registerStudentAccount(payload: StudentRegisterPayload) {
  return (await http.post('/auth/register/student', payload)) as ApiResponse<RegistrationReviewRecord>
}

export async function fetchPendingRegistrations() {
  return (await http.get('/accounts/registrations/pending')) as ApiResponse<RegistrationReviewRecord[]>
}

export async function reviewRegistration(userId: number, payload: RegistrationReviewPayload) {
  return (await http.post(`/accounts/registrations/${userId}/review`, payload)) as ApiResponse<RegistrationReviewRecord>
}

export async function fetchManagedAccounts() {
  return (await http.get('/accounts')) as ApiResponse<ManagedAccountRecord[]>
}

export async function createInternalAccount(payload: InternalAccountCreatePayload) {
  return (await http.post('/accounts/internal', payload)) as ApiResponse<ManagedAccountRecord>
}
