# Dr. Munot's Orthopaedic Care Hospital - Backend (Spring Boot)

Hospital Management System API: patients, appointments, queue, consultations, prescriptions, follow-ups,
automatic reminders, billing, reports/documents, audit log. Modular monolith, package `com.munot.hospital`.
The React frontend lives in the sibling project `dr-munot-orthopaedic-care-frontend`.

> **Before real use:** read "Verification status" and "Go-live checklist" at the bottom.

## Features
- **Roles (backend-enforced):** ADMIN, DOCTOR, RECEPTIONIST, PATIENT (JWT + `@PreAuthorize` + per-record ownership checks).
- **Confirmation before booking is final:** an online request from a patient is stored as `BOOKED` and shown everywhere as **"Awaiting confirmation"**. The patient only gets a "request received - NOT confirmed yet" message and **no reminders**. It becomes `CONFIRMED` (confirmation message + reminders) only when the **doctor or reception** confirms it; if they decline, the patient is notified of the cancellation. If a patient later reschedules, it goes back to awaiting confirmation. Walk-ins and staff-made bookings are confirmed immediately. Pending requests hold their slot so nobody else can take it.
- **Workflow:** Patient books / reception creates walk-in -> Appointment -> Check-in -> Token -> Waiting -> Consultation -> Completed.
  Statuses: `BOOKED, CONFIRMED, CHECKED_IN, WAITING, IN_CONSULTATION, COMPLETED, CANCELLED, NO_SHOW`
  (check-in issues the token and moves the appointment straight to `WAITING`; `CHECKED_IN` is kept for compatibility).
- **Doctor can block time at any moment:** *Block slots* lets the doctor block a whole day or a time range for any future date (private reason optional). Blocked times disappear from patient and reception booking immediately; existing appointments inside a block are flagged so they can be declined or rescheduled (they are never cancelled silently).
- **Booking limits (editable in Admin > Settings):** patients can book at most `max_advance_days` (default 60) ahead and hold at most `max_active_appointments` (default 5) upcoming appointments.
- **Scheduling:** weekly working hours, breaks, leave dates, slot length, max patients per slot; double-booking prevented by a
  row lock on the doctor + slot capacity check + per-patient clash check, all in one transaction.
- **Consultation:** symptoms, examination, diagnoses, treatment plan, tests, medicines (dose/frequency/duration), advice, PDF prescription.
- **Follow-up:** none / 3 / 7 / 15 / 30 days / custom date. A follow-up **appointment is created automatically** (next working day with a free
  slot for presets; closest slot on the chosen date for custom). If no slot exists the follow-up is stored as `PENDING` and reminders still go out.
- **Automatic reminders:** confirmation (immediate), previous-day (default 18:00), same-day (default 2h before, never before 07:00) for appointments and
  follow-ups, plus prescription-available. Rows are written to `notifications` with `scheduled_at`; a scheduler (`ReminderScheduler`, every 60s)
  dispatches due rows over **In-app, Email, SMS, WhatsApp**. Unconfigured channels are marked `SKIPPED`, failures retry 3 times.
  Cancel/reschedule removes pending reminders and creates new ones. `dedupe_key` is unique so reminders are never duplicated;
  `SELECT ... FOR UPDATE SKIP LOCKED` makes multi-instance deployments safe.
- **Medical documents (Doctor):** Medical certificate, Physiotherapy certificate/referral, Implant certificate, Admission / operation advice. The doctor picks one of *their own patients*, types the details (**nothing is pre-filled or invented**; implant fields are optional), previews, **saves a draft**, **issues** it (needs the required fields and the doctor's registration number from Admin > Doctors), then **prints** or **downloads a PDF**. Issued documents are locked records (never deleted); a correction is a **Revise** copy linked to the original. Drafts can be edited/deleted. Doctor-only access (`/api/medical-documents/*`); a doctor can read documents only for patients they treat, and only the issuing doctor can change them; patients, reception and admin cannot read them. Every create/issue/print is audit-logged.
  - **Letterhead printing:** the clinic's logo, name, address and phone are already on the physical letterhead, so the printed page contains **only the document text** in plain black - no header, footer, logo, border, watermark or background, and no billing. The blank area is defined by four print margins in millimetres (default top 55, bottom 30, left 20, right 20; stored in `hospital_settings`, edited by the doctor under *Letterhead print area*). The print page sets `@page { size: A4 portrait; margin: 0 }` (this also stops the browser printing its own date/URL header and footer) and reserves the letterhead area with a repeating header/footer spacer, so the margins hold on page 2 as well. A printable **alignment test sheet** (mm rulers + dashed content box) lets you check the margins on the real letterhead before the first document.
  - **PDF:** `GET /api/medical-documents/{id}/pdf` returns a content-only A4 PDF using the same margins (Latin text). Documents containing Marathi/Hindi characters must use *Print -> Save as PDF* (the server PDF engine cannot shape Devanagari); the app says so.
- **Doctor <-> Reception chat:** a shared staff channel (menu *Messages*). Doctors and receptionists can send messages (up to 1000 characters, Marathi/Hindi/English, 30 per minute per person) and everyone in the channel sees the whole conversation with sender, role and time. Unread messages show as a red count on the menu and top bar; new messages appear automatically (polling every 4 s while the chat is open). A sender can delete only their own message (text is erased, a "message deleted" note stays). Patients and Admin cannot read it. API: `GET/POST /api/chat/messages`, `DELETE /api/chat/messages/{id}`, `GET /api/chat/unread`, `PUT /api/chat/read`, `GET /api/chat/members`.
- **Reports (day / week / month):** `GET /api/reports?period=DAY|WEEK|MONTH&from=&to=` returns, per day, Monday-Sunday week or calendar month: appointments, completed, cancelled, no-show, walk-in / online / follow-up counts, new patients, amount billed and amount collected, plus totals and today's outstanding balance. **Admin and Doctor get the same hospital-wide reports** (page *Reports* in both menus, with charts, date range, **CSV download** and **print**). Ranges are capped at 400 periods.
- **Bills can be viewed, not only downloaded:** *View* opens an itemised bill on screen (items, discount, tax, total, paid, pending, payments) with *Open PDF* (new tab) and *Download PDF*; *Download* still saves the PDF directly. Available to patients (portal), reception and doctor.
- **Always-visible navbar with role:** every dashboard has a top bar with a **Home** link back to the website, the signed-in name, a coloured **role badge** (Admin / Doctor / Receptionist / Patient) and Sign out; the public site header shows the role and a *My Dashboard* link when signed in.
- **Doctor portrait + clinic gallery:** the doctor's photo is shown as a large portrait on the home page and doctor profile (Admin > Doctors > photo). Admin > **Clinic photos** manages a gallery (up to 40 photos, JPG/PNG/WebP, 5 MB each, caption, order, show/hide, delete); visitors click a photo to enlarge it. Uploads are type-checked by file content, not just the name (SVG is refused), and stored with random names under `UPLOAD_DIR`.
- **Prescriptions in the patient's language (Marathi / Hindi):** the doctor writes in English and clicks **मराठी** or **हिन्दी**. Advice, tests advised and each medicine's frequency / duration / instructions are translated, shown in editable boxes (the doctor reviews and can correct), then saved with the prescription (`language`, `advice_local`, `tests_advised_local`, `*_local` item columns). Medicine names, compositions and dosage stay as typed. The patient portal and the print view show the local language first with the English original in small print underneath.
  - Translation = (1) a **built-in glossary** for standard prescription wording (frequency, timing, food relation, durations like "5 days", common orthopaedic advice, test names) - offline, instant, no account needed; (2) optional **machine translation** for any other sentence if `TRANSLATE_PROVIDER` is set (`google` = Cloud Translation v2 key, or `libretranslate` = your own/hosted instance). Sentences nobody can translate stay in English and the doctor is warned. Only the advice sentences are sent to a provider, never patient names or IDs.
  - **Printing:** the *Print / Save PDF* button opens a print-ready A4 view in the browser, because the server's PDF library cannot shape Devanagari text correctly; the browser can. The server-side PDF endpoint (`/api/prescriptions/{id}/pdf`) stays English-only.
  - Have a Marathi/Hindi speaker review the glossary once (`util/TranslationGlossary.java`) - it is plain data and easy to extend.
- **Billing:** itemised bills (consultation, procedure, diagnostic, physiotherapy, medicine, other), discount, optional tax, payments,
  `PAID / PARTIAL / PENDING`, invoice PDF.
- **Documents:** PDF/PNG/JPG upload (magic-byte checked, 15 MB), stored outside the web root with random names, served only through
  authorised endpoints. Patients see only their own; doctors only for patients with an appointment with them; Admin has no access to clinical files.
- **Audit log:** logins (success/failure), record views, uploads/downloads, changes.
- **Security:** BCrypt(12), JWT, login lock-out (5 failures / 15 min), DTO validation, global error handler (no stack traces), CORS allow-list, security headers.

## Tech stack
Java 21, Spring Boot 3.4.5 (Web, Data JPA, Security, Validation, Mail), JJWT 0.12, Flyway, MySQL 8, springdoc OpenAPI, OpenPDF, Lombok, Maven.

## Setup
Requirements: JDK 21, Maven 3.9+, MySQL 8 (or `docker compose up -d`).

### 1. MySQL
```sql
CREATE DATABASE munot_hospital CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'munot_app'@'%' IDENTIFIED BY 'a-strong-password';
GRANT ALL PRIVILEGES ON munot_hospital.* TO 'munot_app'@'%';
```
### 2. Environment
```bash
cp .env.example .env        # then edit; the app reads ./.env automatically (never commit it)
openssl rand -base64 48     # use the output as JWT_SECRET
```
### 3. Migrations
Flyway runs automatically at startup (`src/main/resources/db/migration`): `V1__schema.sql` (core tables), `V2__reference_data.sql`
(departments, services, editable hospital settings with `[placeholder]` values), `V3__content_and_photo.sql` (doctor photo column, editable website content; facilities seeded hidden, no testimonials). Hibernate never changes the schema (`ddl-auto: none`).

### 4. Run
```bash
mvn spring-boot:run               # development (set SEED_DEMO=true in .env for demo data)
mvn clean verify                  # build + unit tests
java -jar target/hospital-1.0.0.jar
```
API docs: `http://localhost:8080/swagger-ui.html` (JSON: `/v3/api-docs`).

## Environment variables
| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | **required**, >= 32 chars; app refuses to start otherwise |
| `JWT_EXPIRATION_MINUTES` | token lifetime (default 480) |
| `CORS_ALLOWED_ORIGINS` | frontend origin(s), comma-separated |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP email |
| `WHATSAPP_API_URL`, `WHATSAPP_API_TOKEN` | WhatsApp provider (Cloud-API-style JSON, Bearer token) |
| `SMS_API_URL`, `SMS_API_TOKEN` | SMS gateway (JSON `{to,message}`, Bearer token) |
| `TRANSLATE_PROVIDER`, `TRANSLATE_API_URL`, `TRANSLATE_API_KEY` | optional machine translation for free-text advice (`google` or `libretranslate`); leave blank to use only the built-in glossary |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` | one-time creation of the first admin (see Deployment step 6) |
| `UPLOAD_DIR`, `PORT`, `APP_TIMEZONE`, `SEED_DEMO`, `SWAGGER_ENABLED` | app options |

No credential is hard-coded anywhere. `WhatsAppSender` / `SmsSender` / `EmailSender` (package `notification`) implement `ChannelSender`;
adapt `buildBody()` to your provider. **Note:** WhatsApp business-initiated messages normally require pre-approved templates - adjust the payload accordingly.

## Demo data & credentials (FICTIONAL - local use only)
Start with `SEED_DEMO=true` on an **empty** database. **Never enable in production.**

| Role | Email | Password |
|---|---|---|
| Admin | `demo-admin@example.com` | `Admin@12345` |
| Doctor | `demo-doctor@example.com` | `Doctor@12345` |
| Receptionist | `demo-reception@example.com` | `Reception@12345` |
| Patient | `demo-patient@example.com` | `Patient@12345` |

Seeds: 3 fictional patients, a doctor with Mon-Sat 10:00-18:00 (break 13:00-14:00), a completed visit with diagnosis, prescription, partly paid bill,
a scheduled follow-up with reminders, and today's clinic (one patient already waiting).

## API overview (full details in Swagger)
| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `GET /me`, `POST /change-password` |
| Public | `GET /api/public/settings`, `/doctors`, `/services`, `/departments`, `/doctors/{id}/slots?date=` |
| Appointments | `POST /api/appointments`, `GET /my`, `GET ?date&doctorId&status`, `GET /slots`, `PUT /{id}/cancel|reschedule|confirm|no-show`, `POST /{id}/check-in` |
| Queue | `GET /api/queue`, `POST /api/queue/{appointmentId}/call` (doctor), `/skip` |
| Consultation | `GET/PUT /api/consultations/{id}`, `POST /{id}/complete` (diagnoses, medicines, follow-up) |
| Patients | `POST/PUT /api/patients`, `GET /search?q=`, `GET /{id}`, `GET /{id}/history` (treating doctor) |
| Prescriptions / follow-ups | `GET /api/prescriptions/my`, `/{id}`, `/{id}/pdf`; `GET /api/followups/my` |
| Billing | `POST /api/bills`, `GET /api/bills`, `/my`, `/{id}`, `POST /{id}/payments`, `GET /{id}/pdf` |
| Documents | `POST /api/documents` (multipart), `GET /my`, `GET /{id}/download`, `GET /api/patients/{id}/documents` |
| Notifications | `GET /api/notifications/my`, `/my/unread-count`, `PUT /{id}/read`, `/my/read-all` |
| Reports | `GET /api/reports?period=&from=&to=` (Admin, Doctor) |
| Staff chat | `/api/chat/*` (Doctor, Receptionist only) |
| Medical documents | `/api/medical-documents` (POST, GET list/one, PUT, `/{id}/issue`, `/{id}/revise`, `/{id}/printed`, DELETE draft, `/{id}/pdf`, `/print-settings`) - Doctor only (print-settings: Doctor, Admin) |
| Admin | `/api/admin/{summary,doctors,receptionists,users,patients,departments,services,settings,content,audit-logs}`, doctor `availability`, `leaves`, `photo` |
| Website content (public) | `GET /api/public/content?type=FACILITY\|WHY_US\|TESTIMONIAL\|HEALTH_TIP`, `GET /api/public/doctors/{id}/photo`, `GET /api/public/clinic-photos`, `GET /api/public/clinic-photos/{id}/image` |

## Deployment
1. Provision MySQL (managed service recommended, with automated backups) and create the DB/user.
2. Build `mvn clean package -DskipTests` -> `target/hospital-1.0.0.jar`; run on JDK 21 as a systemd service / container with the env vars above.
3. Put the API behind HTTPS (nginx/Caddy/cloud load balancer). Set `CORS_ALLOWED_ORIGINS` to the exact frontend origin, `SEED_DEMO=false`, `SWAGGER_ENABLED=false`.
4. Mount a persistent, **backed-up** volume for `UPLOAD_DIR` (patient files live here, not in MySQL).
5. Run exactly one instance, or several - reminders are multi-instance safe. Forward `X-Forwarded-For` so audit IPs are correct.
6. **First admin:** set `ADMIN_EMAIL`, `ADMIN_PASSWORD` (and optionally `ADMIN_NAME`) for the very first start. When the `users` table is empty the app creates that one ADMIN
   account (and nothing else - no demo data). Sign in, change the password under *My account*, then delete `ADMIN_PASSWORD` from the environment.
   Doctors, receptionists, services, photos and website content are then managed entirely from the Admin panel - see `HANDOVER.md`.

## Verification status (what was and was NOT verified)
This project was generated in a sandbox that could **not reach Maven Central**, so the Spring backend **has not been compiled or started**. Everything below was actually executed:

| Check | Result |
|---|---|
| Flyway `V1`-`V5` executed against a real database (MariaDB 10.11) | **Passed** - 30 tables |
| All 30 JPA entities vs. actual table columns (automated cross-check) | **Passed**, 0 mismatches |
| DB safety nets (`src/test/sql/constraints_check.sql`): duplicate appointment code, duplicate patient identity, duplicate queue token, bad/half block times, duplicate reminder, two bills per appointment, duplicate email | **8 of 8 rejected correctly** |
| Business-rule unit tests (`src/test/java`): slot generation + breaks, bill totals/status, follow-up dates, **appointment confirmation rules, patient-reschedule re-confirmation, advance-booking limit, slot-block overlap, Marathi/Hindi glossary (frequency, durations, advice, patterns, no-guessing), image upload rules (allowed types, renamed-file detection), **report period buckets (day/Mon-Sun week/calendar month, leap year, every date in exactly one bucket, range limits), chat rules (who may chat, trimming, length limit, Marathi text), **medical-document field rules (required only to issue, no invented values, date order, implant at-least-one, unknown fields, length, Latin-only PDF check, margin ranges)** | **40 of 40 passed** (+ `TranslationServiceTest`, Mockito - its logic was executed separately with a fake machine translator: 15/15 checks) (executed with a mini runner; they are standard JUnit 5 classes) |
| `javac` parse + cross-class references (Lombok expanded in a scratch copy) | **0 syntax errors**, 0 unresolved project members |
| Frontend: strict TypeScript + production build | **Passed** |
| Frontend UI tests (`npm test`, Vitest + Testing Library, mocked API): login validation, booking (full slots disabled, "awaiting confirmation" message, slot-taken error), pending-request confirm/decline, doctor slot blocking (whole day / range / invalid range), medicine auto-suggest (name + composition only, never dose), status labels, **language flow (click मराठी -> translate -> edit -> save payload), print view, bilingual patient view, clinic gallery (enlarge/keyboard/hidden-when-empty), doctor portrait, admin photo upload checks, **bill View vs Download, openFile new-tab/pop-up-blocker behaviour, reports (day/week/month, date-range validation, CSV), dashboard navbar (Home link, role badge, sign-out, bell for patients only), **staff chat (shows sender/role, send, Enter vs Shift+Enter, auto-refresh with 'since', delete own only, load older, unread badge), **medical documents (4 types, validation, draft/issue/revise/lock, registration-number gate, history, print area), letterhead print rules (content-only: no branding/billing/images/backgrounds; A4 portrait, @page margin 0, repeating mm spacers, screen-only hints, draft cannot print)** | **84 of 84 passed** |
| All 81 API calls made by the UI vs. backend controller mappings | **All match** |
| **Printing on the real pre-printed letterhead, real browser print dialog/preview, real printer alignment**, `mvn compile`, Spring context start-up, JPA/JPQL validation, security filter chain, PDF output, scheduler, e-mail/SMS/WhatsApp delivery, real-browser/mobile UI | **NOT RUN** - you must run `mvn clean verify` and the smoke test below |

Expect a small number of compile-level fixes (library API details) on the first `mvn clean verify`.

### Suggested first-run smoke test (about 20 minutes)
0. `curl http://localhost:8080/actuator/health` -> `{"status":"UP"}` (use this for load-balancer/uptime checks).
1. `SEED_DEMO=true mvn spring-boot:run`, open Swagger, log in as each demo role, confirm 403 on cross-role calls.
2. Patient: book a slot (expect **"Awaiting confirmation"**, no reminders) -> book the *same* slot as another user (expect 409) -> doctor/reception **Confirm** (patient now gets a confirmation + reminders) -> reschedule (goes back to awaiting confirmation) -> cancel.
   Doctor: *Block slots* for tomorrow 10:00-12:00 -> those slots vanish for the patient; a whole-day block hides the day.
3. Reception: register patient (duplicate phone+name -> 409), walk-in, check-in (token), bill + part payment, invoice PDF.
4. Doctor: call patient, save draft, complete with a 7-day follow-up -> check `appointments` (type `FOLLOW_UP`), `follow_ups`, `notifications` rows.
5. Set a follow-up for tomorrow and watch `notifications.status` move `PENDING -> SENT/SKIPPED` after `scheduled_at`.
6. As patient A request patient B's `/api/prescriptions/{id}`, `/api/documents/{id}/download`, `/api/bills/{id}` -> expect 403.

## Go-live checklist (not software, but required)
- Replace every `[placeholder]` (address, phones, hours) and the doctor's real qualifications/registration number in Admin; review/replace the generic content in the frontend `src/content/site.ts` (facilities, testimonials are **samples**).
- Medical-records legal/privacy obligations (e.g. India's DPDP Act 2023, record-retention rules, patient consent, a privacy policy page) are the hospital's responsibility - have them reviewed.
- Enable DB + upload backups and test a restore; use HTTPS only; rotate `JWT_SECRET` if ever exposed.
- WhatsApp/SMS providers need account approval; register message templates where required.
- Production: `SWAGGER_ENABLED=false` (default), `SEED_DEMO=false`, HTTPS in front, `CORS_ALLOWED_ORIGINS` set to the real site, health check at `/actuator/health`. A `Dockerfile` is included (untested - run `docker build .`).
- Known limits: login lock-out is in-memory (per instance); JWT is kept in browser `localStorage` by the frontend (httpOnly-cookie auth is a stronger option for hardening); no payment-gateway integration (payments are recorded by reception).
