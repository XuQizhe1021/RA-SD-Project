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
