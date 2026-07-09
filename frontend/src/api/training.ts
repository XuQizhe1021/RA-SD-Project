import http from './http'
import type {
  ApiResponse,
  CourseOptionRecord,
  CourseRecord,
  EnrollmentRecord,
  LecturerRecord,
  PageResult,
  StudentOptionRecord,
} from '../types/api'

export interface LecturerQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
}

export interface LecturerPayload {
  fullName: string
  title: string
  specialty: string
  phone: string
  email: string
  feeStandard: number
  profileText: string
}

export interface CourseQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
  lecturerId?: number | undefined
}

export interface CoursePayload {
  applicationId?: number | null
  courseName: string
  lecturerId?: number | null
  startTime: string
  endTime: string
  location: string
  quota: number
  feeAmount: number
}

export interface EnrollmentQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
  courseId?: number | undefined
  studentId?: number | undefined
}

export interface EnrollmentCreatePayload {
  courseId: number
  studentId: number
  paymentType: string
}

export interface EnrollmentConfirmPayload {
  approved: boolean
  rejectReason?: string
}

export async function fetchLecturerPage(params: LecturerQuery) {
  return (await http.get('/lecturers', { params })) as ApiResponse<PageResult<LecturerRecord>>
}

export async function fetchLecturerOptions() {
  return (await http.get('/lecturers/options')) as ApiResponse<LecturerRecord[]>
}

export async function createLecturer(payload: LecturerPayload) {
  return (await http.post('/lecturers', payload)) as ApiResponse<LecturerRecord>
}

export async function updateLecturer(id: number, payload: LecturerPayload) {
  return (await http.put(`/lecturers/${id}`, payload)) as ApiResponse<LecturerRecord>
}

export async function disableLecturer(id: number) {
  return (await http.post(`/lecturers/${id}/disable`)) as ApiResponse<LecturerRecord>
}

export async function fetchCoursePage(params: CourseQuery) {
  return (await http.get('/courses', { params })) as ApiResponse<PageResult<CourseRecord>>
}

export async function createCourse(payload: CoursePayload) {
  return (await http.post('/courses', payload)) as ApiResponse<CourseRecord>
}

export async function updateCourse(id: number, payload: CoursePayload) {
  return (await http.put(`/courses/${id}`, payload)) as ApiResponse<CourseRecord>
}

export async function publishCourse(id: number) {
  return (await http.post(`/courses/${id}/publish`)) as ApiResponse<CourseRecord>
}

export async function fetchEnrollmentPage(params: EnrollmentQuery) {
  return (await http.get('/enrollments', { params })) as ApiResponse<PageResult<EnrollmentRecord>>
}

export async function fetchEnrollmentCourseOptions() {
  return (await http.get('/enrollments/options/courses')) as ApiResponse<CourseOptionRecord[]>
}

export async function fetchEnrollmentStudentOptions() {
  return (await http.get('/enrollments/options/students')) as ApiResponse<StudentOptionRecord[]>
}

export async function createEnrollment(payload: EnrollmentCreatePayload) {
  return (await http.post('/enrollments', payload)) as ApiResponse<EnrollmentRecord>
}

export async function confirmEnrollment(id: number, payload: EnrollmentConfirmPayload) {
  return (await http.post(`/enrollments/${id}/confirm`, payload)) as ApiResponse<EnrollmentRecord>
}
