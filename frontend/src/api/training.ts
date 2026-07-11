import http from './http'
import type {
  ApiResponse,
  AttendanceRecordView,
  CourseEvaluationReport,
  CourseEvaluationSummary,
  CourseOptionRecord,
  CourseRecord,
  CourseStatisticsRecord,
  EvaluationCandidateRecord,
  EvaluationRecordView,
  EnrollmentRecord,
  LecturerRecord,
  LecturerStatisticsRecord,
  PageResult,
  PendingEvaluationCourse,
  PaymentRecordView,
  RevenueStatisticsResponse,
  StudentOptionRecord,
  StudentStatisticsRecord,
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

export interface AttendanceQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
  courseId?: number | undefined
}

export interface AttendanceCheckInPayload {
  remark?: string
}

export interface PaymentQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  paymentStatus?: string
  courseId?: number | undefined
}

export interface PaymentPayPayload {
  paidAmount: number
  paymentMethod: string
}

export interface EvaluationSubmitPayload {
  courseId: number
  studentId?: number
  enrollmentId: number
  rating: number
  commentText?: string
}

export interface EvaluationSummaryQuery {
  keyword?: string
  startDate?: string
  endDate?: string
  hasEvaluation?: boolean
}

export interface StatisticsQuery {
  keyword?: string
  startDate?: string
  endDate?: string
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

export async function fetchAttendancePage(params: AttendanceQuery) {
  return (await http.get('/attendance-records', { params })) as ApiResponse<PageResult<AttendanceRecordView>>
}

export async function checkInAttendance(id: number, payload: AttendanceCheckInPayload) {
  return (await http.post(`/attendance-records/${id}/check-in`, payload)) as ApiResponse<AttendanceRecordView>
}

export async function fetchPaymentPage(params: PaymentQuery) {
  return (await http.get('/payments', { params })) as ApiResponse<PageResult<PaymentRecordView>>
}

export async function payPayment(id: number, payload: PaymentPayPayload) {
  return (await http.post(`/payments/${id}/pay`, payload)) as ApiResponse<PaymentRecordView>
}

export async function fetchPendingEvaluationCourses() {
  return (await http.get('/evaluations/pending-courses')) as ApiResponse<PendingEvaluationCourse[]>
}

export async function fetchMyEvaluations() {
  return (await http.get('/evaluations/mine')) as ApiResponse<EvaluationRecordView[]>
}

export async function fetchEvaluationProxyCourses() {
  return (await http.get('/evaluations/proxy-courses')) as ApiResponse<CourseOptionRecord[]>
}

export async function fetchEvaluationProxyCandidates(courseId: number) {
  return (await http.get('/evaluations/proxy-candidates', { params: { courseId } })) as ApiResponse<EvaluationCandidateRecord[]>
}

export async function submitEvaluation(payload: EvaluationSubmitPayload) {
  return (await http.post('/evaluations', payload)) as ApiResponse<EvaluationRecordView>
}

export async function fetchEvaluationSummaries(params: EvaluationSummaryQuery) {
  return (await http.get('/evaluations/summaries', { params })) as ApiResponse<CourseEvaluationSummary[]>
}

export async function fetchCourseEvaluationReport(courseId: number) {
  return (await http.get(`/evaluations/courses/${courseId}/report`)) as ApiResponse<CourseEvaluationReport>
}

export async function fetchCourseStatistics(params: StatisticsQuery) {
  return (await http.get('/statistics/courses', { params })) as ApiResponse<CourseStatisticsRecord[]>
}

export async function fetchStudentStatistics(params: StatisticsQuery) {
  return (await http.get('/statistics/students', { params })) as ApiResponse<StudentStatisticsRecord[]>
}

export async function fetchLecturerStatistics(params: StatisticsQuery) {
  return (await http.get('/statistics/lecturers', { params })) as ApiResponse<LecturerStatisticsRecord[]>
}

export async function fetchRevenueStatistics(params: StatisticsQuery) {
  return (await http.get('/statistics/revenue', { params })) as ApiResponse<RevenueStatisticsResponse>
}
