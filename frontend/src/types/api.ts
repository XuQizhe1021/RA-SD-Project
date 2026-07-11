export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface PageResult<T> {
  list: T[]
  pageNum: number
  pageSize: number
  total: number
}

export interface LecturerRecord {
  id: number
  lecturerNo: string
  fullName: string
  title: string
  specialty: string
  phone: string
  email: string
  feeStandard: number
  profileText: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface TrainingApplicationRecord {
  id: number
  applicationNo: string
  companyId: number
  companyName: string
  applicantUserId: number | null
  applicantName: string
  topic: string
  expectedStartDate: string | null
  expectedEndDate: string | null
  attendeeCount: number
  budgetAmount: number | null
  requirementDesc: string
  status: string
  approvalComment: string | null
  approvedBy: number | null
  approvedByName: string
  approvedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ApplicationOptionRecord {
  id: number
  applicationNo: string
  companyName: string
  topic: string
  expectedStartDate: string | null
  expectedEndDate: string | null
  attendeeCount: number
  budgetAmount: number | null
}

export interface CourseRecord {
  id: number
  courseNo: string
  applicationId: number | null
  courseName: string
  lecturerId: number | null
  lecturerName: string
  executorUserId: number
  executorName: string
  startTime: string
  endTime: string
  location: string
  quota: number
  feeAmount: number
  status: string
  sourceType: string
  createdAt: string
  updatedAt: string
}

export interface CourseOptionRecord {
  id: number
  courseNo: string
  courseName: string
  location: string
  startTime: string
  quota: number
  feeAmount: number
  status: string
}

export interface StudentOptionRecord {
  id: number
  studentNo: string
  fullName: string
  companyName: string
  phone: string
  email: string
}

export interface StudentProfileRecord {
  id: number
  userId: number | null
  studentNo: string
  fullName: string
  gender: string
  companyId: number | null
  companyName: string
  jobTitle: string
  educationLevel: string
  techLevel: string
  phone: string
  email: string
  createdAt: string
  updatedAt: string
}

export interface RegistrationReviewRecord {
  userId: number
  username: string
  displayName: string
  phone: string
  email: string
  companyName: string
  jobTitle: string
  educationLevel: string
  techLevel: string
  accountStatus: string
  reviewerName: string
  reviewComment: string
  createdAt: string
  reviewedAt: string | null
}

export interface ManagedAccountRecord {
  userId: number
  username: string
  displayName: string
  phone: string
  email: string
  accountType: string
  roleCode: string
  roleName: string
  accountStatus: string
  reviewerName: string
  reviewComment: string
  createdAt: string
  reviewedAt: string | null
  lastLoginAt: string | null
}

export interface EnrollmentRecord {
  id: number
  enrollmentNo: string
  courseId: number
  courseNo: string
  courseName: string
  courseLocation: string
  courseStartTime: string
  courseFeeAmount: number
  studentId: number
  studentNo: string
  studentName: string
  companyName: string
  paymentType: string
  status: string
  confirmedBy: number | null
  confirmedByName: string
  confirmedAt: string | null
  rejectReason: string | null
  createdAt: string
  updatedAt: string
}

export interface AttendanceRecordView {
  id: number
  enrollmentId: number
  enrollmentNo: string
  courseId: number
  courseNo: string
  courseName: string
  courseStartTime: string
  studentId: number
  studentNo: string
  studentName: string
  companyName: string
  attendanceStatus: string
  checkedInAt: string | null
  checkedInBy: number | null
  checkedInByName: string
  remark: string | null
  materialStatus: string
  materialRemark: string | null
  createdAt: string
  updatedAt: string
}

export interface PaymentRecordView {
  id: number
  enrollmentId: number
  enrollmentNo: string
  courseId: number
  courseNo: string
  courseName: string
  studentId: number
  studentNo: string
  studentName: string
  companyName: string
  paymentType: string
  receivableAmount: number
  paidAmount: number
  paymentMethod: string | null
  paymentStatus: string
  paidAt: string | null
  handledBy: number | null
  handledByName: string
  payerName: string
  paymentRemark: string
  createdAt: string
  updatedAt: string
}

export interface CourseNoticeRecord {
  id: number
  courseId: number
  courseNo: string
  courseName: string
  title: string
  content: string
  registrationStartAt: string | null
  registrationEndAt: string | null
  status: string
  publishedAt: string | null
  externalPublishFlag: boolean
  createdBy: number
  createdByName: string
  createdAt: string
  updatedAt: string
}

export interface PendingEvaluationCourse {
  courseId: number
  enrollmentId: number
  courseNo: string
  courseName: string
  lecturerName: string
  startTime: string
  endTime: string
  location: string
}

export interface EvaluationRecordView {
  id: number
  courseId: number
  enrollmentId: number
  courseNo: string
  courseName: string
  lecturerName: string
  studentId: number
  studentName: string
  companyName: string
  rating: number
  commentText: string
  source: string
  proxyStaffId: number | null
  proxyStaffName: string
  submittedBy: number | null
  submittedByName: string
  submittedAt: string
}

export interface EvaluationCandidateRecord {
  courseId: number
  enrollmentId: number
  studentId: number
  studentNo: string
  studentName: string
  companyName: string
  checkedInAt: string
  evaluationStatus: string
  evaluationSource: string | null
  submittedAt: string | null
}

export interface CourseEvaluationSummary {
  courseId: number
  courseNo: string
  courseName: string
  lecturerName: string
  startTime: string
  endTime: string
  shouldEvaluateCount: number
  evaluatedCount: number
  averageRating: number | null
}

export interface ScoreDistributionItem {
  score: number
  count: number
}

export interface CourseEvaluationReport {
  courseId: number
  courseNo: string
  courseName: string
  lecturerName: string
  startTime: string
  endTime: string
  location: string
  shouldEvaluateCount: number
  evaluatedCount: number
  participationRate: number
  averageRating: number | null
  scoreDistribution: ScoreDistributionItem[]
  details: EvaluationRecordView[]
}

export interface CourseStatisticsRecord {
  courseId: number
  courseNo: string
  courseName: string
  lecturerName: string
  startTime: string
  endTime: string
  quota: number
  enrollmentCount: number
  attendanceCount: number
  paidAmountTotal: number
  averageRating: number | null
}

export interface StudentStatisticsRecord {
  studentId: number
  studentNo: string
  studentName: string
  companyName: string
  attendanceCount: number
  enrollmentCount: number
  paidAmountTotal: number
  averageRating: number | null
}

export interface LecturerStatisticsRecord {
  lecturerId: number
  lecturerNo: string
  lecturerName: string
  courseCount: number
  attendanceCount: number
  averageRating: number | null
  feeAmountTotal: number
}

export interface ExecutorStatisticsRecord {
  executorUserId: number
  executorName: string
  courseCount: number
  publishedCourseCount: number
  enrollmentReviewedCount: number
  trainingCompletedCount: number
  attendanceCount: number
  paidAmountTotal: number
}

export interface RevenueDetailRecord {
  paymentId: number
  courseName: string
  studentName: string
  receivableAmount: number
  paidAmount: number
  paymentMethod: string
  paidAt: string | null
  handledByName: string
}

export interface RevenueStatisticsResponse {
  receivableAmountTotal: number
  paidAmountTotal: number
  specialPaymentCount: number
  cashAmountRatio: number
  transferAmountRatio: number
  details: RevenueDetailRecord[]
}
