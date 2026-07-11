import http from './http'
import type {
  ApiResponse,
  ApplicationOptionRecord,
  AttendanceRecordView,
  CourseEvaluationReport,
  CourseEvaluationSummary,
  CourseNoticeRecord,
  CourseOptionRecord,
  CourseRecord,
  CourseStatisticsRecord,
  ExecutorStatisticsRecord,
  EvaluationCandidateRecord,
  EvaluationRecordView,
  EnrollmentRecord,
  LecturerRecord,
  LecturerStatisticsRecord,
  PageResult,
  PendingEvaluationCourse,
  PaymentRecordView,
  RevenueStatisticsResponse,
  StudentProfileRecord,
  StudentOptionRecord,
  StudentStatisticsRecord,
  TrainingApplicationRecord,
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

export interface ApplicationQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
}

export interface ApplicationPayload {
  companyName: string
  topic: string
  expectedStartDate?: string
  expectedEndDate?: string
  attendeeCount: number
  budgetAmount?: number | null
  requirementDesc?: string
}

export interface ApplicationApprovePayload {
  approved: boolean
  approvalComment?: string
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
  materialStatus?: string
  materialRemark?: string
}

export interface AttendanceMaterialPayload {
  materialStatus: string
  materialRemark?: string
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
  payerName?: string
  paymentRemark?: string
}

export interface StudentQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  companyId?: number | undefined
}

export interface StudentPayload {
  fullName: string
  gender?: string
  companyName: string
  jobTitle?: string
  educationLevel?: string
  techLevel?: string
  phone?: string
  email?: string
}

export interface NoticeQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
  courseId?: number | undefined
}

export interface NoticePayload {
  courseId: number
  title: string
  content: string
  registrationStartAt?: string
  registrationEndAt?: string
  externalPublishFlag?: boolean
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

export async function fetchApplicationPage(params: ApplicationQuery) {
  return (await http.get('/applications', { params })) as ApiResponse<PageResult<TrainingApplicationRecord>>
}

export async function createApplication(payload: ApplicationPayload) {
  return (await http.post('/applications', payload)) as ApiResponse<TrainingApplicationRecord>
}

export async function approveApplication(id: number, payload: ApplicationApprovePayload) {
  return (await http.post(`/applications/${id}/approve`, payload)) as ApiResponse<TrainingApplicationRecord>
}

export async function fetchApprovedApplicationOptions() {
  return (await http.get('/applications/options/approved')) as ApiResponse<ApplicationOptionRecord[]>
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

export async function updateAttendanceMaterials(id: number, payload: AttendanceMaterialPayload) {
  return (await http.post(`/attendance-records/${id}/materials`, payload)) as ApiResponse<AttendanceRecordView>
}

export async function fetchPaymentPage(params: PaymentQuery) {
  return (await http.get('/payments', { params })) as ApiResponse<PageResult<PaymentRecordView>>
}

export async function payPayment(id: number, payload: PaymentPayPayload) {
  return (await http.post(`/payments/${id}/pay`, payload)) as ApiResponse<PaymentRecordView>
}

export async function fetchStudentPage(params: StudentQuery) {
  return (await http.get('/students', { params })) as ApiResponse<PageResult<StudentProfileRecord>>
}

export async function createStudent(payload: StudentPayload) {
  return (await http.post('/students', payload)) as ApiResponse<StudentProfileRecord>
}

export async function updateStudent(id: number, payload: StudentPayload) {
  return (await http.put(`/students/${id}`, payload)) as ApiResponse<StudentProfileRecord>
}

export async function fetchNoticePage(params: NoticeQuery) {
  return (await http.get('/notices', { params })) as ApiResponse<PageResult<CourseNoticeRecord>>
}

export async function createNotice(payload: NoticePayload) {
  return (await http.post('/notices', payload)) as ApiResponse<CourseNoticeRecord>
}

export async function updateNotice(id: number, payload: NoticePayload) {
  return (await http.put(`/notices/${id}`, payload)) as ApiResponse<CourseNoticeRecord>
}

export async function publishNotice(id: number) {
  return (await http.post(`/notices/${id}/publish`)) as ApiResponse<CourseNoticeRecord>
}

export async function revokeNotice(id: number) {
  return (await http.post(`/notices/${id}/revoke`)) as ApiResponse<CourseNoticeRecord>
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

export async function fetchExecutorStatistics(params: StatisticsQuery) {
  return (await http.get('/statistics/executors', { params })) as ApiResponse<ExecutorStatisticsRecord[]>
}

export async function fetchRevenueStatistics(params: StatisticsQuery) {
  return (await http.get('/statistics/revenue', { params })) as ApiResponse<RevenueStatisticsResponse>
}
