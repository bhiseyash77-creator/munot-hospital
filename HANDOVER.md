# Handover guide - Dr. Munot's Orthopaedic Care Hospital

For the hospital owner / admin. Nothing below needs a programmer once the system is deployed.

## 1. First start (done once by whoever deploys)
1. Set the environment variables (see `.env.example`): database, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and **`ADMIN_EMAIL` + `ADMIN_PASSWORD`**.
2. Start the backend. On an empty database it creates exactly one account: the admin. No demo or sample patient data is created (keep `SEED_DEMO=false`).
3. Sign in at `/login` with that email and password, open **My account** and change the password. Then remove `ADMIN_PASSWORD` from the server environment.

## 2. Admin: set up the hospital (in this order)
| Step | Where | What to do |
|---|---|---|
| 1 | **Settings** | Replace every `[placeholder]`: hospital name, address, phone, **emergency phone**, email, opening hours, About text, Google Maps embed link (optional), invoice footer, patient cancel cut-off hours. |
| 2 | **Doctors > Add doctor** | Name, login email and password, department, qualifications, registration number, consultation fee, biography, slot length (e.g. 15 min), patients per slot, and the **doctor photo** (JPG/PNG/WebP up to 3 MB). The photo is shown as a large portrait on the home page and doctor profile (a clear, upright head-and-shoulders photo works best; it is cropped to a 4:5 portrait from the top). |
| 3 | **Doctors > Schedule** | Tick working days, set start/end time and break, and add leave dates. Patients can only book inside these hours. |
| 4 | **Staff & users > Add receptionist** | Create a login for each receptionist. Use *Disable* when someone leaves. |
| 5 | **Services** | Edit the starter services to match what the hospital really offers, set prices, hide or delete the rest. |
| 5b | **Clinic photos** | Upload photos of the clinic (reception, consultation room, building). Select many at once; add a caption, set the order, and hide or delete any time. They appear in an enlargeable gallery on the home page and Facilities page. |
| 6 | **Website content** | *Facilities* (starter items are hidden - switch on only what you really have), *Why choose us*, *Testimonials* (only real, permitted feedback; none are pre-loaded), *Health tips* (articles). Changes go live immediately. |

Doctors and receptionists can change their own password under **My account**. The admin can reset a user's password from the edit screen.

## 2b. Online bookings are never final until you confirm
When a patient books online, the request shows as **Awaiting confirmation** (a yellow panel on the Doctor and Reception dashboards). The patient is told it is *not confirmed yet* and gets **no reminders**.
Press **Confirm** (the patient is notified and reminders start) or **Decline** (the patient is told the appointment was cancelled). Walk-ins you book yourself are confirmed immediately.

## 2c. Doctor: block any time, any day
Doctor menu **Block slots**: pick a date, choose *Whole day* or a *Time range*, add an optional private reason, and press Block. Patients and reception cannot book that time from that moment.
If appointments already exist inside the blocked time, you are warned with a count - decline or reschedule them from *Today's clinic*. Remove a block any time to reopen the slots. Weekly hours and holidays are still set by the admin under Doctors > Schedule.

## 2d. Reports (Admin and Doctor)
Open **Reports** in the menu. Choose **Day-wise**, **Week-wise** (Monday to Sunday) or **Month-wise**, optionally pick a date range (or *Last 7 / 30 / 90 days*), and you get totals, two charts and a table: appointments, completed, cancelled, no-shows, walk-in / online / follow-up, new patients, amount billed and amount collected, plus today's outstanding balance. Use **Download CSV** (opens in Excel) or **Print**. The doctor sees the same hospital-wide figures as the admin.

## 2e. Viewing and downloading bills
In Billing (reception/doctor) and *My bills* (patient) each bill has **View** (itemised bill on screen, with *Open PDF* and *Download PDF*) and **Download** (saves the PDF directly).

## 2e2. Messages between Doctor and Reception
Open **Messages** (doctor and reception menus). It is one shared conversation: type a message and press **Enter** (Shift+Enter for a new line). Everyone in the chat sees every message with the sender's name and role, and new messages appear on their own - a red number on *Messages* shows how many you have not read yet.
You can delete your own message (others cannot delete yours). Patients and the admin cannot see this chat. Use it for things like "Patient Asha has arrived" or "Please send the next patient"; avoid putting more patient details than needed.

## 2e3. Medical documents on your letterhead (Doctor)
Menu **Medical documents** -> choose the patient -> choose *Medical certificate*, *Physiotherapy certificate*, *Implant certificate* or *Admission / operation advice* -> type the details (nothing is filled in for you) -> **Save draft** (come back later), then **Issue** when verified. After issuing: **Print**, **Download PDF** or **Revise** (a corrected copy; the original stays on record). Your name, qualification and registration number come from Admin > Doctors (the admin must enter the registration number before you can issue).
**Your letterhead already carries the clinic name, logo, address and phone, so the system prints only the document text.** One-time set-up (10 minutes):
1. Click **Letterhead print area**. Measure on your paper how many millimetres the printed header takes from the top, the footer from the bottom, and the left/right margins. Enter them and Save (defaults: top 55, bottom 30, left 20, right 20).
2. Click **Print test sheet** and print it **on a sheet of your letterhead**: A4, **Portrait**, **Scale 100%**, **Margins: None**, **Headers and footers: off**, **Background graphics: off**. The dashed box is where document text will appear - if it touches the printed header/footer, increase that margin and test again.
3. Print a first real document the same way. Use Chrome or Edge. Keep these print settings for every document.
The page shows a grey "letterhead area" band on screen only; it never prints. If a document runs to a second page, the same margins repeat on it.
Documents with Marathi/Hindi text: use **Print**, then *Save as PDF* in the print window (the *Download PDF* button supports English text only).

## 2f. Moving around
Every screen inside the system has a top bar: **Home** returns to the hospital website, your name and a coloured badge show who is signed in (Admin / Doctor / Receptionist / Patient), and **Sign out** is always one click away.

## 3. Daily workflow
Patient requests online (doctor/reception **confirms**) or reception books a walk-in -> **Reception** checks the patient in (token is issued) -> **Doctor** sees the waiting queue and taps *Call in* -> consultation, diagnosis, medicines, follow-up -> *Complete* ->
prescription PDF is ready, follow-up appointment is booked and reminders are scheduled automatically -> the **Doctor or Reception** creates the bill and records payment (Doctor: Billing menu; reports can also be uploaded from Reports & documents) -> patient sees prescription, reports and bills in the portal.

## 4. Reminders (WhatsApp / SMS / Email)
In-app reminders work immediately. Email, SMS and WhatsApp stay **off until the provider details are set** in the server environment (`MAIL_*`, `SMS_API_*`, `WHATSAPP_API_*`).
WhatsApp business messages normally need an approved provider account and message templates; ask your developer to match the payload to your provider. Check `notifications.status`: `SENT`, `SKIPPED` (channel not configured) or `FAILED`.

## 5. Before announcing the website
- [ ] No `[placeholder]` text is left anywhere (Settings, doctor profile).
- [ ] Doctor qualifications and registration number are correct. Testimonials are real and permitted.
- [ ] HTTPS is on; database and uploaded-files folder are backed up and a restore has been tested.
- [ ] Admin password changed; `ADMIN_PASSWORD`, demo seeding and Swagger are switched off in production.
- [ ] Privacy policy / patient consent and record-retention rules reviewed for your state and for India's DPDP Act.
- [ ] One full test visit done end to end with a test patient (booking -> queue -> consultation -> bill).

## 6. Medicine suggestions (Doctor)
Each prescribed medicine has a **Medicine name** and a **Composition / contains** field (for example *Dolo 650 Tablet* - *Paracetamol 650 mg*). Both are remembered.
Next time, start typing a name (or part of the composition) and pick it from the list - the name and composition fill in automatically.
**Dosage, frequency, duration and instructions are never remembered** - you type them fresh for every patient. The composition is shown on the printed prescription (under the medicine name), in the patient portal and in every visit history.
Use the small **x** in the suggestion list to remove a wrongly spelled entry.

## 6b. Prescription in Marathi or Hindi (for patients who cannot read English)
1. Type the medicines and advice in English as usual (frequency, duration, instructions, advice, tests).
2. In **Patient's language**, click **मराठी** or **हिन्दी**. The advice, tests and each medicine's frequency/duration/instructions appear translated in editable boxes.
3. **Read and correct** the translation if needed. Anything the system could not translate stays in English and a yellow note tells you - type it yourself or leave it.
4. Save / Complete as usual. The Marathi/Hindi text is saved with the prescription and shown to the patient in the portal (local language first, English underneath).
5. **Print prescription** -> choose *Print*, or *Save as PDF* in the print window to give or send the patient a PDF in their language. (Use Chrome/Edge; the device needs a normal Devanagari font, which Windows, Android and iPhone already have.)
Medicine names and compositions are always kept in English so the pharmacist can read them. The wording translations are built in; arbitrary sentences are translated only if the hospital sets up a translation service (see README). Please have a Marathi/Hindi speaker check the standard phrases once.

## 7. Patients see your advice
After a consultation is completed, the patient sees the diagnosis, treatment plan, tests, advice, medicines and follow-up under **My visits** in the portal, with the prescription PDF.
