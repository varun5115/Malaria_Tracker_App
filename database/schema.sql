-- =====================================================================
--  M3 Platform — MySQL schema
--  Target: MySQL 8.0.13+ (Cloud SQL for MySQL 8.0 compatible)
--
--  Conventions
--   * Record IDs are CHAR(36) UUIDs. The device may generate them; if
--     not supplied, MySQL fills them with UUID().
--   * Workers are keyed by patient_code (e.g. P0001), assigned by the
--     server at registration via CALL next_patient_code(@code).
--   * Phone numbers live only in `users`: phone_hash (SHA-256/HMAC hex,
--     used for login lookup) + phone_enc (encrypted by the API).
--   * *_enc columns hold values encrypted by the API before insert.
--   * Every data table has created_at, updated_at, created_by, deleted_at.
--     Rows are soft-deleted (deleted_at set), never hard-deleted.
--   * Multi-select answers are stored as JSON arrays.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS m3_platform
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE m3_platform;

-- ---------------------------------------------------------------------
-- 1. Identity & access
-- ---------------------------------------------------------------------

CREATE TABLE users (
  user_id     CHAR(36)       NOT NULL DEFAULT (UUID()),
  phone_hash  CHAR(64)       NOT NULL,
  phone_enc   VARBINARY(255) NOT NULL,
  name_enc    VARBINARY(512) NOT NULL,
  is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
  created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by  CHAR(36)       NULL,
  deleted_at  DATETIME       NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_users_phone_hash (phone_hash),
  CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE roles (
  role_id    TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_name  VARCHAR(30)      NOT NULL,
  PRIMARY KEY (role_id),
  UNIQUE KEY uq_roles_name (role_name)
) ENGINE=InnoDB;

CREATE TABLE permissions (
  permission_id  SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name           VARCHAR(60)       NOT NULL,
  description    VARCHAR(255)      NULL,
  PRIMARY KEY (permission_id),
  UNIQUE KEY uq_permissions_name (name)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
  role_id        TINYINT UNSIGNED  NOT NULL,
  permission_id  SMALLINT UNSIGNED NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles (role_id),
  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. Reference
-- ---------------------------------------------------------------------

CREATE TABLE facilities (
  facility_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name         VARCHAR(100) NOT NULL,
  type         ENUM('UHC','PHC','other') NOT NULL,
  address      VARCHAR(255) NULL,
  district     VARCHAR(60)  NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by   CHAR(36)     NULL,
  deleted_at   DATETIME     NULL,
  PRIMARY KEY (facility_id),
  UNIQUE KEY uq_facilities_name (name),
  CONSTRAINT fk_facilities_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- A user may hold several roles (dual-role: worker + clinician, etc.).
CREATE TABLE user_roles (
  user_id      CHAR(36)         NOT NULL,
  role_id      TINYINT UNSIGNED NOT NULL,
  facility_id  INT UNSIGNED     NULL,
  created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by   CHAR(36)         NULL,
  deleted_at   DATETIME         NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user       FOREIGN KEY (user_id)     REFERENCES users (user_id),
  CONSTRAINT fk_user_roles_role       FOREIGN KEY (role_id)     REFERENCES roles (role_id),
  CONSTRAINT fk_user_roles_facility   FOREIGN KEY (facility_id) REFERENCES facilities (facility_id),
  CONSTRAINT fk_user_roles_created_by FOREIGN KEY (created_by)  REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE worksites (
  worksite_id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name                VARCHAR(120) NOT NULL,
  district            VARCHAR(60)  NULL,
  area                VARCHAR(80)  NULL,
  location            VARCHAR(255) NULL,
  contractor_user_id  CHAR(36)     NULL,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by          CHAR(36)     NULL,
  deleted_at          DATETIME     NULL,
  PRIMARY KEY (worksite_id),
  CONSTRAINT fk_worksites_contractor FOREIGN KEY (contractor_user_id) REFERENCES users (user_id),
  CONSTRAINT fk_worksites_created_by FOREIGN KEY (created_by)         REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Append-only: no updated_at / deleted_at.
CREATE TABLE audit_logs (
  log_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     CHAR(36)        NULL,
  action      VARCHAR(30)     NOT NULL,   -- e.g. create, update, delete, view, login
  table_name  VARCHAR(64)     NULL,
  record_id   VARCHAR(36)     NULL,
  details     JSON            NULL,
  logged_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (log_id),
  KEY ix_audit_logs_record (table_name, record_id),
  CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. Worker profile
-- ---------------------------------------------------------------------

-- Counter behind patient_code. Use CALL next_patient_code(@code).
CREATE TABLE patient_code_seq (
  seq         INT UNSIGNED NOT NULL AUTO_INCREMENT,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (seq)
) ENGINE=InnoDB;

CREATE TABLE workers (
  patient_code    VARCHAR(16)     NOT NULL,
  user_id         CHAR(36)        NOT NULL,
  dob             DATE            NULL,
  age             TINYINT UNSIGNED NULL,
  gender          ENUM('male','female','other') NULL,
  alt_mobile_enc  VARBINARY(255)  NULL,
  govt_id_type    ENUM('aadhaar','voter_id','passport','driving_license','other') NULL,
  govt_id_enc     VARBINARY(255)  NULL,
  languages       JSON            NULL,   -- e.g. ["Hindi","Bengali"]
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by      CHAR(36)        NULL,
  deleted_at      DATETIME        NULL,
  PRIMARY KEY (patient_code),
  UNIQUE KEY uq_workers_user (user_id),
  CONSTRAINT fk_workers_user       FOREIGN KEY (user_id)    REFERENCES users (user_id),
  CONSTRAINT fk_workers_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE worker_addresses (
  id            CHAR(36)        NOT NULL DEFAULT (UUID()),
  patient_code  VARCHAR(16)     NOT NULL,
  address_type  ENUM('present','permanent') NOT NULL,
  address_enc   VARBINARY(1024) NOT NULL,
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by    CHAR(36)        NULL,
  deleted_at    DATETIME        NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_worker_addresses_worker     FOREIGN KEY (patient_code) REFERENCES workers (patient_code),
  CONSTRAINT fk_worker_addresses_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE worker_worksite (
  id            CHAR(36)     NOT NULL DEFAULT (UUID()),
  patient_code  VARCHAR(16)  NOT NULL,
  worksite_id   INT UNSIGNED NOT NULL,
  from_date     DATE         NULL,
  to_date       DATE         NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by    CHAR(36)     NULL,
  deleted_at    DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_worker_worksite_worker     FOREIGN KEY (patient_code) REFERENCES workers (patient_code),
  CONSTRAINT fk_worker_worksite_worksite   FOREIGN KEY (worksite_id)  REFERENCES worksites (worksite_id),
  CONSTRAINT fk_worker_worksite_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Section II – Past history of malaria (one row per worker).
CREATE TABLE malaria_history (
  patient_code       VARCHAR(16) NOT NULL,
  diagnosed_before   ENUM('yes','no','dont_know') NULL,
  times_last_2y      TINYINT UNSIGNED NULL,
  last_detection     ENUM('le_1_year','1_2_years','dont_know') NULL,
  treatment_places   JSON NULL,   -- UHC/PHC, private clinic, govt hospital, pharmacy, other text
  test_performed     BOOLEAN NULL,
  same_day_report    BOOLEAN NULL,
  report_methods     JSON NULL,   -- RDT, microscopy, not sure
  medication_period  ENUM('3_days','14_days_pv','28_days_followup','not_informed') NULL,
  course_completed   ENUM('yes','no','not_sure') NULL,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by         CHAR(36) NULL,
  deleted_at         DATETIME NULL,
  PRIMARY KEY (patient_code),
  CONSTRAINT fk_malaria_history_worker     FOREIGN KEY (patient_code) REFERENCES workers (patient_code),
  CONSTRAINT fk_malaria_history_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Section IV (past travel), Section V (planned travel) and contractor
-- traveller details. created_by records who entered the row.
CREATE TABLE travel_records (
  id                     CHAR(36)    NOT NULL DEFAULT (UUID()),
  patient_code           VARCHAR(16) NOT NULL,
  travel_type            ENUM('past','planned') NOT NULL,
  -- Section IV (past)
  stay_duration_goa      ENUM('le_1_month','1_6_months','ge_6_months') NULL,
  travelled_outside_goa  BOOLEAN NULL,
  states_visited         VARCHAR(255) NULL,
  home_stay_duration     ENUM('7_days','14_days','le_1_month','other') NULL,
  fever_before_coming    ENUM('yes','no','dont_know') NULL,
  -- Section V (planned) / contractor travel details
  departure_date         DATE NULL,
  return_date            DATE NULL,
  fever_during_travel    BOOLEAN NULL,
  treatment_given        BOOLEAN NULL,
  full_course_completed  BOOLEAN NULL,
  place_of_treatment     VARCHAR(255) NULL,
  created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by             CHAR(36) NULL,
  deleted_at             DATETIME NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_travel_records_worker     FOREIGN KEY (patient_code) REFERENCES workers (patient_code),
  CONSTRAINT fk_travel_records_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Anonymised ID for research use.
CREATE TABLE worker_mapping (
  patient_code  VARCHAR(16) NOT NULL,
  research_id   VARCHAR(20) NOT NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by    CHAR(36)    NULL,
  deleted_at    DATETIME    NULL,
  PRIMARY KEY (patient_code),
  UNIQUE KEY uq_worker_mapping_research (research_id),
  CONSTRAINT fk_worker_mapping_worker     FOREIGN KEY (patient_code) REFERENCES workers (patient_code),
  CONSTRAINT fk_worker_mapping_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. Clinical records (each malaria episode = one health_event)
-- ---------------------------------------------------------------------

CREATE TABLE health_events (
  event_id       CHAR(36)     NOT NULL DEFAULT (UUID()),
  patient_code   VARCHAR(16)  NOT NULL,
  facility_id    INT UNSIGNED NULL,
  event_type     ENUM('malaria_episode','fever_screening','other') NOT NULL DEFAULT 'malaria_episode',
  opened_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at      DATETIME     NULL,
  final_outcome  ENUM('cured','ongoing_treatment','recurrent','lost_to_followup') NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by     CHAR(36)     NULL,
  deleted_at     DATETIME     NULL,
  PRIMARY KEY (event_id),
  CONSTRAINT fk_health_events_worker     FOREIGN KEY (patient_code) REFERENCES workers (patient_code),
  CONSTRAINT fk_health_events_facility   FOREIGN KEY (facility_id)  REFERENCES facilities (facility_id),
  CONSTRAINT fk_health_events_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Optional per-event restriction. Empty = role-level permissions apply.
CREATE TABLE event_access (
  event_id    CHAR(36)         NOT NULL,
  role_id     TINYINT UNSIGNED NOT NULL,
  created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by  CHAR(36)         NULL,
  PRIMARY KEY (event_id, role_id),
  CONSTRAINT fk_event_access_event      FOREIGN KEY (event_id)   REFERENCES health_events (event_id),
  CONSTRAINT fk_event_access_role       FOREIGN KEY (role_id)    REFERENCES roles (role_id),
  CONSTRAINT fk_event_access_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Section III – Present clinical details (worker-reported, per visit).
CREATE TABLE symptom_reports (
  id                CHAR(36)     NOT NULL DEFAULT (UUID()),
  event_id          CHAR(36)     NOT NULL,
  fever_today       ENUM('yes','no','not_sure') NULL,
  temperature       DECIMAL(4,1) NULL,
  fever_duration    ENUM('24_hours','1_3_days','gt_3_days','not_required') NULL,
  symptoms          JSON         NULL,
  onset_date        DATE         NULL,
  danger_signs      BOOLEAN      NULL,   -- breathing difficulty, chest pain or confusion
  can_visit_centre  ENUM('yes','no','not_required') NULL,
  reported_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by        CHAR(36)     NULL,
  deleted_at        DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_symptom_reports_event      FOREIGN KEY (event_id)   REFERENCES health_events (event_id),
  CONSTRAINT fk_symptom_reports_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Clinician questionnaire.
CREATE TABLE clinical_assessments (
  id                    CHAR(36)     NOT NULL DEFAULT (UUID()),
  event_id              CHAR(36)     NOT NULL,
  clinician_id          CHAR(36)     NOT NULL,
  body_temp             DECIMAL(4,1) NULL,
  fever_duration        ENUM('lt_24_hours','1_3_days','4_7_days','gt_7_days') NULL,
  symptoms              JSON         NULL,
  general_condition     ENUM('stable','moderately_ill','severe') NULL,
  blood_pressure        VARCHAR(15)  NULL,   -- e.g. 120/80
  comorbidities         JSON         NULL,
  treatment_prescribed  TEXT         NULL,
  admitted              BOOLEAN      NULL,
  treatment_given       BOOLEAN      NULL,   -- if not admitted
  hw_assigned           BOOLEAN      NULL,   -- if not admitted
  assessed_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by            CHAR(36)     NULL,
  deleted_at            DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_clinical_assessments_event      FOREIGN KEY (event_id)     REFERENCES health_events (event_id),
  CONSTRAINT fk_clinical_assessments_clinician  FOREIGN KEY (clinician_id) REFERENCES users (user_id),
  CONSTRAINT fk_clinical_assessments_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Clinician section A: only when admitted = yes.
CREATE TABLE admissions (
  id                       CHAR(36)     NOT NULL DEFAULT (UUID()),
  assessment_id            CHAR(36)     NOT NULL,
  species                  ENUM('pf','pv','mixed','other') NULL,
  species_other            VARCHAR(150) NULL,
  symptoms                 JSON         NULL,
  severity                 ENUM('mild','severe') NULL,
  treatment_completed      ENUM('full_course','on_treatment_at_discharge','stopped_early','referred_district_gmc') NULL,
  reason_not_completed     TEXT         NULL,
  discharge_date           DATE         NULL,
  discharge_outcome        ENUM('recovered','improved_continue_opd','referred_district_gmc','other') NULL,
  discharge_outcome_other  VARCHAR(150) NULL,
  remarks                  TEXT         NULL,
  created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by               CHAR(36)     NULL,
  deleted_at               DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_admissions_assessment (assessment_id),
  CONSTRAINT fk_admissions_assessment FOREIGN KEY (assessment_id) REFERENCES clinical_assessments (id),
  CONSTRAINT fk_admissions_created_by FOREIGN KEY (created_by)    REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Pathology questionnaire. One row per test, including follow-up slides.
CREATE TABLE lab_results (
  id                        CHAR(36)     NOT NULL DEFAULT (UUID()),
  event_id                  CHAR(36)     NOT NULL,
  test_types                JSON         NULL,   -- RDT, SMP, other
  rdt_result                ENUM('negative','positive') NULL,
  rdt_species               ENUM('pf','pv','mixed','other') NULL,
  rdt_species_other         VARCHAR(150) NULL,
  microscopy_result         ENUM('negative','positive') NULL,
  microscopy_species        ENUM('pf','pv','mixed','other') NULL,
  microscopy_species_other  VARCHAR(150) NULL,
  smear_type                ENUM('thick','thin','both') NULL,
  followup_day              TINYINT UNSIGNED NULL,   -- NULL for the first test
  slides_received           BOOLEAN      NULL,
  any_slide_positive        BOOLEAN      NULL,
  positive_slide_day        TINYINT UNSIGNED NULL,
  non_receipt_reasons       JSON         NULL,
  tested_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by                CHAR(36)     NULL,
  deleted_at                DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_lab_results_event      FOREIGN KEY (event_id)   REFERENCES health_events (event_id),
  CONSTRAINT fk_lab_results_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Research Lab questionnaire (PCR & drug resistance).
CREATE TABLE research_lab_results (
  id                  CHAR(36)     NOT NULL DEFAULT (UUID()),
  event_id            CHAR(36)     NOT NULL,
  lab_name            ENUM('NIMR','Goa University') NULL,
  rdt_result          ENUM('negative','positive') NULL,
  rdt_species         ENUM('pf','pv','mixed','other') NULL,
  microscopy_result   ENUM('negative','positive') NULL,
  microscopy_species  ENUM('pf','pv','mixed','other') NULL,
  smear_type          ENUM('thick','thin','both') NULL,
  parasite_density    INT UNSIGNED NULL,   -- parasites/µL
  gametocyte          ENUM('present','absent','not_recorded') NULL,
  pv_markers          JSON NULL,   -- PvCRT-o, Pv MDR1, other
  pf_markers          JSON NULL,   -- Pfkelch13, PfDHFR, Pfdhps, other
  hrp_deletion        ENUM('yes','no','other') NULL,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by          CHAR(36) NULL,
  deleted_at          DATETIME NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_research_lab_results_event      FOREIGN KEY (event_id)   REFERENCES health_events (event_id),
  CONSTRAINT fk_research_lab_results_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Health Worker questionnaire. One row per follow-up day.
CREATE TABLE followups (
  id                    CHAR(36)    NOT NULL DEFAULT (UUID()),
  event_id              CHAR(36)    NOT NULL,
  health_worker_id      CHAR(36)    NOT NULL,
  species_track         ENUM('pf','pv') NULL,
  followup_day          TINYINT UNSIGNED NULL,   -- 1, 2, 3, 7, 14, 28
  patient_status        ENUM('symptom_free','fever_persists','new_or_worsening','did_not_report') NULL,
  treatment_adherence   ENUM('completed','partial','stopped_early','unknown') NULL,
  non_adherence_reason  ENUM('felt_better','side_effects','drug_unavailable','left_goa','other') NULL,
  non_adherence_other   VARCHAR(150) NULL,
  visit_date            DATE        NULL,
  created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by            CHAR(36)    NULL,
  deleted_at            DATETIME    NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_followups_event         FOREIGN KEY (event_id)         REFERENCES health_events (event_id),
  CONSTRAINT fk_followups_health_worker FOREIGN KEY (health_worker_id) REFERENCES users (user_id),
  CONSTRAINT fk_followups_created_by    FOREIGN KEY (created_by)       REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Contractor questionnaire (travel details go to travel_records).
CREATE TABLE contractor_reports (
  id                CHAR(36)     NOT NULL DEFAULT (UUID()),
  patient_code      VARCHAR(16)  NOT NULL,
  worksite_id       INT UNSIGNED NULL,
  contractor_id     CHAR(36)     NOT NULL,
  diagnosis_status  ENUM('pv','pf','pv_pf','other') NULL,
  diagnosis_other   VARCHAR(150) NULL,
  treatment_status  ENUM('treated','untreated') NULL,
  reported_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by        CHAR(36)     NULL,
  deleted_at        DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_contractor_reports_worker     FOREIGN KEY (patient_code)  REFERENCES workers (patient_code),
  CONSTRAINT fk_contractor_reports_worksite   FOREIGN KEY (worksite_id)   REFERENCES worksites (worksite_id),
  CONSTRAINT fk_contractor_reports_contractor FOREIGN KEY (contractor_id) REFERENCES users (user_id),
  CONSTRAINT fk_contractor_reports_created_by FOREIGN KEY (created_by)    REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE assignments (
  id               CHAR(36)    NOT NULL DEFAULT (UUID()),
  event_id         CHAR(36)    NOT NULL,
  patient_code     VARCHAR(16) NOT NULL,
  staff_id         CHAR(36)    NOT NULL,
  assignment_type  ENUM('followup','treatment','other') NOT NULL DEFAULT 'followup',
  assigned_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by       CHAR(36)    NULL,
  deleted_at       DATETIME    NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_assignments_event      FOREIGN KEY (event_id)     REFERENCES health_events (event_id),
  CONSTRAINT fk_assignments_worker     FOREIGN KEY (patient_code) REFERENCES workers (patient_code),
  CONSTRAINT fk_assignments_staff      FOREIGN KEY (staff_id)     REFERENCES users (user_id),
  CONSTRAINT fk_assignments_created_by FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB;

-- Placeholder for future alerts.
CREATE TABLE alerts (
  id              CHAR(36)     NOT NULL DEFAULT (UUID()),
  patient_code    VARCHAR(16)  NOT NULL,
  event_id        CHAR(36)     NULL,
  target_user_id  CHAR(36)     NULL,
  alert_type      VARCHAR(40)  NOT NULL,
  message         VARCHAR(255) NULL,
  status          ENUM('open','acknowledged','resolved') NOT NULL DEFAULT 'open',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by      CHAR(36)     NULL,
  deleted_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_alerts_worker      FOREIGN KEY (patient_code)   REFERENCES workers (patient_code),
  CONSTRAINT fk_alerts_event       FOREIGN KEY (event_id)       REFERENCES health_events (event_id),
  CONSTRAINT fk_alerts_target_user FOREIGN KEY (target_user_id) REFERENCES users (user_id),
  CONSTRAINT fk_alerts_created_by  FOREIGN KEY (created_by)     REFERENCES users (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. patient_code generator
-- ---------------------------------------------------------------------
-- Returns P0001, P0002, ... The 4 is a minimum width, not a cap
-- (P10000 follows P9999).

DELIMITER $$
CREATE PROCEDURE next_patient_code(OUT p_code VARCHAR(16))
BEGIN
  DECLARE n INT UNSIGNED;
  INSERT INTO patient_code_seq () VALUES ();
  SET n = LAST_INSERT_ID();
  SET p_code = CONCAT('P', LPAD(n, GREATEST(4, CHAR_LENGTH(n)), '0'));
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 6. Seed data
-- ---------------------------------------------------------------------

INSERT INTO roles (role_name) VALUES
  ('Worker'), ('Clinician'), ('Pathology'), ('Health Worker'),
  ('Research Lab'), ('Contractor'), ('Admin');

INSERT INTO facilities (name, type) VALUES
  ('UHC Panaji', 'UHC'), ('PHC Candolim', 'PHC'),
  ('UHC Margao', 'UHC'), ('PHC Cortalim', 'PHC');
