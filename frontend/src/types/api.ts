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
