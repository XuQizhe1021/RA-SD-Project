DROP DATABASE IF EXISTS hq_training;
CREATE DATABASE hq_training DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE hq_training;

CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(30),
    account_type VARCHAR(30) NOT NULL,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE IF NOT EXISTS customer_company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_name VARCHAR(150) NOT NULL UNIQUE,
    company_type VARCHAR(50),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(30),
    contact_email VARCHAR(100),
    remark VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS training_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_no VARCHAR(50) NOT NULL UNIQUE,
    company_id BIGINT NOT NULL,
    applicant_user_id BIGINT,
    topic VARCHAR(200) NOT NULL,
    expected_start_date DATE,
    expected_end_date DATE,
    attendee_count INT NOT NULL,
    budget_amount DECIMAL(10,2),
    requirement_desc TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_comment VARCHAR(255),
    approved_by BIGINT,
    approved_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_application_company FOREIGN KEY (company_id) REFERENCES customer_company(id),
    CONSTRAINT fk_application_applicant FOREIGN KEY (applicant_user_id) REFERENCES user_account(id),
    CONSTRAINT fk_application_approver FOREIGN KEY (approved_by) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS lecturer_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    lecturer_no VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    title VARCHAR(100),
    specialty VARCHAR(200),
    phone VARCHAR(30),
    email VARCHAR(100),
    fee_standard DECIMAL(10,2),
    profile_text TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_lecturer_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    student_no VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    company_id BIGINT,
    job_title VARCHAR(100),
    education_level VARCHAR(50),
    tech_level VARCHAR(50),
    phone VARCHAR(30),
    email VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_student_company FOREIGN KEY (company_id) REFERENCES customer_company(id)
);

CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_no VARCHAR(50) NOT NULL UNIQUE,
    application_id BIGINT,
    course_name VARCHAR(200) NOT NULL,
    lecturer_id BIGINT,
    executor_user_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    location VARCHAR(200) NOT NULL,
    quota INT NOT NULL,
    fee_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    attachment_path VARCHAR(255),
    source_type VARCHAR(30) DEFAULT 'SYSTEM',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_course_application FOREIGN KEY (application_id) REFERENCES training_application(id),
    CONSTRAINT fk_course_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturer_profile(id),
    CONSTRAINT fk_course_executor FOREIGN KEY (executor_user_id) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS course_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    registration_start_at DATETIME,
    registration_end_at DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME,
    external_publish_flag TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_notice_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_notice_creator FOREIGN KEY (created_by) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS enrollment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_no VARCHAR(50) NOT NULL UNIQUE,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    payment_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confirmed_by BIGINT,
    confirmed_at DATETIME,
    reject_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_enrollment_course_student UNIQUE (course_id, student_id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES student_profile(id),
    CONSTRAINT fk_enrollment_confirmer FOREIGN KEY (confirmed_by) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL UNIQUE,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_CHECKED_IN',
    checked_in_at DATETIME,
    checked_in_by BIGINT,
    remark VARCHAR(255),
    material_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    material_remark VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
    CONSTRAINT fk_attendance_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES student_profile(id),
    CONSTRAINT fk_attendance_operator FOREIGN KEY (checked_in_by) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL UNIQUE,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    receivable_amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(30),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    paid_at DATETIME,
    handled_by BIGINT,
    payer_name VARCHAR(100),
    payment_remark VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_payment_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
    CONSTRAINT fk_payment_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_payment_student FOREIGN KEY (student_id) REFERENCES student_profile(id),
    CONSTRAINT fk_payment_operator FOREIGN KEY (handled_by) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS course_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enrollment_id BIGINT,
    rating INT NOT NULL,
    comment_text TEXT,
    source VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    proxy_staff_id BIGINT,
    submitted_by BIGINT,
    submitted_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_evaluation_course_student UNIQUE (course_id, student_id),
    CONSTRAINT fk_evaluation_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_evaluation_student FOREIGN KEY (student_id) REFERENCES student_profile(id),
    CONSTRAINT fk_evaluation_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
    CONSTRAINT fk_evaluation_proxy_staff FOREIGN KEY (proxy_staff_id) REFERENCES user_account(id),
    CONSTRAINT fk_evaluation_submitter FOREIGN KEY (submitted_by) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_user_id BIGINT NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    business_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    action_detail TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_operation_user FOREIGN KEY (operator_user_id) REFERENCES user_account(id)
);

CREATE INDEX idx_user_account_username ON user_account (username);
CREATE INDEX idx_application_no_status ON training_application (application_no, status);
CREATE INDEX idx_course_no_status_start_time ON course (course_no, status, start_time);
CREATE INDEX idx_notice_course_status ON course_notice (course_id, status);
CREATE INDEX idx_enrollment_course_status ON enrollment (course_id, status);
CREATE INDEX idx_enrollment_student_status ON enrollment (student_id, status);
CREATE INDEX idx_attendance_course_status ON attendance_record (course_id, status);
CREATE INDEX idx_payment_course_status ON payment_record (course_id, payment_status);
CREATE INDEX idx_evaluation_course_rating ON course_evaluation (course_id, rating);

INSERT INTO role (id, role_code, role_name, description, created_at, updated_at) VALUES
(1, 'MANAGER', '经理', '审批申请、查看统计', NOW(), NOW()),
(2, 'EXECUTOR', '执行人', '负责课程、通知、报名审核', NOW(), NOW()),
(3, 'SITE_STAFF', '现场工作人员', '负责签到与收费', NOW(), NOW()),
(4, 'STUDENT', '学员', '负责报名与评价', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO user_account (
    id, username, password_hash, display_name, email, phone, account_type, account_status, last_login_at, created_at, updated_at
) VALUES
(1, 'manager01', '$2a$10$r7ThwTGHfF7oaTcQ9lT7UO0deo9JdyWFM8kwVu9xWS.DtQrDoahH.', '培训经理', 'manager@hq.local', '13800000001', 'MANAGER', 'ACTIVE', NULL, NOW(), NOW()),
(2, 'executor01', '$2a$10$r7ThwTGHfF7oaTcQ9lT7UO0deo9JdyWFM8kwVu9xWS.DtQrDoahH.', '执行人-李工', 'executor@hq.local', '13800000002', 'EXECUTOR', 'ACTIVE', NULL, NOW(), NOW()),
(3, 'staff01', '$2a$10$r7ThwTGHfF7oaTcQ9lT7UO0deo9JdyWFM8kwVu9xWS.DtQrDoahH.', '现场工作人员', 'staff@hq.local', '13800000003', 'SITE_STAFF', 'ACTIVE', NULL, NOW(), NOW()),
(4, 'student01', '$2a$10$r7ThwTGHfF7oaTcQ9lT7UO0deo9JdyWFM8kwVu9xWS.DtQrDoahH.', '演示学员', 'student@hq.local', '13800000004', 'STUDENT', 'ACTIVE', NULL, NOW(), NOW()),
(5, 'student02', '$2a$10$r7ThwTGHfF7oaTcQ9lT7UO0deo9JdyWFM8kwVu9xWS.DtQrDoahH.', '演示学员-王敏', 'student02@hq.local', '13800000005', 'STUDENT', 'ACTIVE', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    display_name = VALUES(display_name),
    email = VALUES(email),
    phone = VALUES(phone),
    account_type = VALUES(account_type),
    account_status = VALUES(account_status),
    updated_at = NOW();

INSERT INTO user_role (id, user_id, role_id, created_at) VALUES
(1, 1, 1, NOW()),
(2, 2, 2, NOW()),
(3, 3, 3, NOW()),
(4, 4, 4, NOW()),
(5, 5, 4, NOW())
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO customer_company (
    id, company_name, company_type, contact_person, contact_phone, contact_email, remark, created_at, updated_at
) VALUES
(1, '未来软件科技有限公司', '软件企业', '王经理', '13600000001', 'wang@future-soft.com', '增量1演示客户', NOW(), NOW()),
(2, '星河智造集团', '制造企业', '赵主管', '13600000002', 'zhao@star-manufacturing.com', '培训申请与通知演示客户', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO training_application (
    id, application_no, company_id, applicant_user_id, topic, expected_start_date, expected_end_date, attendee_count,
    budget_amount, requirement_desc, status, approval_comment, approved_by, approved_at, created_at, updated_at
) VALUES
(1, 'APP20260711001', 1, 1, 'Spring Boot 企业级开发实战', '2026-07-09', '2026-07-09', 60, 120000.00, '希望围绕 Spring Boot 企业级开发、接口设计和工程规范开展专项培训。', 'COURSE_CREATED', '申请已通过并进入建课执行。', 1, '2026-07-08 09:20:00', '2026-07-08 08:30:00', '2026-07-08 09:20:00'),
(2, 'APP20260711002', 2, 1, '制造业数字化转型项目管理训练营', '2026-07-15', '2026-07-16', 45, 98000.00, '聚焦制造企业项目推进、跨部门协作与敏捷项目管理实践。', 'APPROVED', '预算与主题已确认，可安排执行人建课。', 1, '2026-07-11 09:10:00', '2026-07-11 08:40:00', '2026-07-11 09:10:00')
ON DUPLICATE KEY UPDATE
    company_id = VALUES(company_id),
    topic = VALUES(topic),
    expected_start_date = VALUES(expected_start_date),
    expected_end_date = VALUES(expected_end_date),
    attendee_count = VALUES(attendee_count),
    budget_amount = VALUES(budget_amount),
    requirement_desc = VALUES(requirement_desc),
    status = VALUES(status),
    approval_comment = VALUES(approval_comment),
    approved_by = VALUES(approved_by),
    approved_at = VALUES(approved_at),
    updated_at = VALUES(updated_at);

INSERT INTO student_profile (
    id, user_id, student_no, full_name, gender, company_id, job_title, education_level, tech_level, phone, email, created_at, updated_at
) VALUES
(1, 4, 'STU20260709001', '演示学员', '男', 1, '软件工程师', '本科', '中级', '13800000004', 'student@hq.local', NOW(), NOW()),
(2, 5, 'STU20260709002', '演示学员-王敏', '女', 1, '测试工程师', '本科', '中级', '13800000005', 'student02@hq.local', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    gender = VALUES(gender),
    company_id = VALUES(company_id),
    job_title = VALUES(job_title),
    education_level = VALUES(education_level),
    tech_level = VALUES(tech_level),
    phone = VALUES(phone),
    email = VALUES(email),
    updated_at = NOW();

INSERT INTO lecturer_profile (
    id, user_id, lecturer_no, full_name, title, specialty, phone, email, fee_standard, profile_text, status, created_at, updated_at
) VALUES
(1, NULL, 'LEC20260708001', '周教授', '高级架构师', '微服务架构, Java企业级开发', '13810000001', 'zhou.teacher@hq.local', 6800.00, '负责企业级架构与后端技术培训。', 'ACTIVE', NOW(), NOW()),
(2, NULL, 'LEC20260708002', '陈老师', '敏捷教练', 'Scrum实践, 项目管理', '13810000002', 'chen.agile@hq.local', 5200.00, '擅长敏捷研发流程与项目复盘。', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    title = VALUES(title),
    specialty = VALUES(specialty),
    phone = VALUES(phone),
    email = VALUES(email),
    fee_standard = VALUES(fee_standard),
    profile_text = VALUES(profile_text),
    status = VALUES(status),
    updated_at = NOW();

INSERT INTO course (
    id, course_no, application_id, course_name, lecturer_id, executor_user_id, start_time, end_time, location, quota, fee_amount, status, attachment_path, source_type, created_at, updated_at
) VALUES
(1, 'CRS20260708001', 1, 'Spring Boot 企业级开发实战', 1, 2, '2026-07-09 09:00:00', '2026-07-09 17:30:00', '未来技术学院 A301', 60, 1999.00, 'PUBLISHED', NULL, 'APPLICATION', NOW(), NOW()),
(2, 'CRS20260708002', NULL, 'Scrum 冲刺管理与实践', 2, 2, '2026-07-10 13:30:00', '2026-07-10 18:00:00', '未来技术学院 B201', 45, 1299.00, 'FINISHED', NULL, 'SYSTEM', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    application_id = VALUES(application_id),
    course_name = VALUES(course_name),
    lecturer_id = VALUES(lecturer_id),
    executor_user_id = VALUES(executor_user_id),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    location = VALUES(location),
    quota = VALUES(quota),
    fee_amount = VALUES(fee_amount),
    status = VALUES(status),
    source_type = VALUES(source_type),
    updated_at = NOW();

INSERT INTO course_notice (
    id, course_id, title, content, registration_start_at, registration_end_at, status, published_at, external_publish_flag, created_by, created_at, updated_at
) VALUES
(1, 1, 'Spring Boot 企业级开发实战开班通知', '课程已确认开班，请相关学员根据通知完成报名与现场准备。', '2026-07-08 08:00:00', '2026-07-09 08:30:00', 'PUBLISHED', '2026-07-08 10:00:00', 1, 2, NOW(), NOW()),
(2, 2, 'Scrum 冲刺管理与实践开班通知', '课程已开放报名，请相关学员尽快提交报名。', '2026-07-08 08:00:00', '2026-07-10 12:00:00', 'PUBLISHED', '2026-07-09 09:30:00', 0, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    registration_start_at = VALUES(registration_start_at),
    registration_end_at = VALUES(registration_end_at),
    status = VALUES(status),
    published_at = VALUES(published_at),
    updated_at = NOW();

INSERT INTO enrollment (
    id, enrollment_no, course_id, student_id, payment_type, status, confirmed_by, confirmed_at, reject_reason, created_at, updated_at
) VALUES
(1, 'ENR20260710001', 2, 1, 'PERSONAL', 'CONFIRMED', 2, '2026-07-10 10:00:00', NULL, '2026-07-10 09:40:00', '2026-07-10 10:00:00'),
(2, 'ENR20260710002', 2, 2, 'CORPORATE', 'CONFIRMED', 2, '2026-07-10 10:05:00', NULL, '2026-07-10 09:45:00', '2026-07-10 10:05:00')
ON DUPLICATE KEY UPDATE
    payment_type = VALUES(payment_type),
    status = VALUES(status),
    confirmed_by = VALUES(confirmed_by),
    confirmed_at = VALUES(confirmed_at),
    reject_reason = VALUES(reject_reason),
    updated_at = VALUES(updated_at);

INSERT INTO attendance_record (
    id, enrollment_id, course_id, student_id, status, checked_in_at, checked_in_by, remark, material_status, material_remark, created_at, updated_at
) VALUES
(1, 1, 2, 1, 'CHECKED_IN', '2026-07-10 13:20:00', 3, '已完成现场签到', 'ISSUED', '已领取培训讲义与课程资料袋', '2026-07-10 10:00:00', '2026-07-10 13:20:00'),
(2, 2, 2, 2, 'CHECKED_IN', '2026-07-10 13:25:00', 3, '企业学员签到完成', 'PENDING', '资料待课程中场统一发放', '2026-07-10 10:05:00', '2026-07-10 13:25:00')
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    checked_in_at = VALUES(checked_in_at),
    checked_in_by = VALUES(checked_in_by),
    remark = VALUES(remark),
    material_status = VALUES(material_status),
    material_remark = VALUES(material_remark),
    updated_at = VALUES(updated_at);

INSERT INTO payment_record (
    id, enrollment_id, course_id, student_id, receivable_amount, paid_amount, payment_method, payment_status, paid_at, handled_by, payer_name, payment_remark, created_at, updated_at
) VALUES
(1, 1, 2, 1, 1299.00, 1299.00, 'CASH', 'PAID', '2026-07-10 13:35:00', 3, NULL, '现场现金收费，已核对学员信息', '2026-07-10 10:00:00', '2026-07-10 13:35:00'),
(2, 2, 2, 2, 1299.00, 1299.00, 'CORPORATE', 'CORPORATE_PAID', '2026-07-10 13:40:00', 3, '未来软件科技有限公司', '企业统一结算，现场已登记到账信息', '2026-07-10 10:05:00', '2026-07-10 13:40:00')
ON DUPLICATE KEY UPDATE
    receivable_amount = VALUES(receivable_amount),
    paid_amount = VALUES(paid_amount),
    payment_method = VALUES(payment_method),
    payment_status = VALUES(payment_status),
    paid_at = VALUES(paid_at),
    handled_by = VALUES(handled_by),
    payer_name = VALUES(payer_name),
    payment_remark = VALUES(payment_remark),
    updated_at = VALUES(updated_at);

INSERT INTO course_evaluation (
    id, course_id, student_id, enrollment_id, rating, comment_text, source, proxy_staff_id, submitted_by, submitted_at, created_at, updated_at
) VALUES
(1, 2, 1, 1, 5, '课程节奏清晰，Scrum 例会和任务拆解示例很实用。', 'STUDENT', NULL, 4, '2026-07-10 18:20:00', '2026-07-10 18:20:00', '2026-07-10 18:20:00')
ON DUPLICATE KEY UPDATE
    rating = VALUES(rating),
    comment_text = VALUES(comment_text),
    source = VALUES(source),
    proxy_staff_id = VALUES(proxy_staff_id),
    submitted_by = VALUES(submitted_by),
    submitted_at = VALUES(submitted_at),
    updated_at = VALUES(updated_at);
