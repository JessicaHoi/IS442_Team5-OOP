# StudyBuddy Front End

Vue 3 + Bootstrap 5 single-page app for the Study Buddy Matcher System (IS442). It covers the three user groups in the brief: **students**, **study group leaders** (students who create groups) and **system administrators**.

## Quick start

Requires Node 20.19+ (or 22.12+).

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, answers API calls from the built-in mock
npm test           # unit tests (Vitest)
npm run build      # production bundle into backend/src/main/resources/static
```

`npm run dev` uses the **mock API** (see below), so the whole UI works without the Spring Boot backend.

**Demo accounts** (the login page has one-click buttons; the password for every account is `password`):

| Role | Email |
|---|---|
| Student (also a group leader) | `aisha.rahman@smu.edu.sg` |
| System administrator | `admin@smu.edu.sg` |

Any other seeded student can sign in with `<first>.<last>@smu.edu.sg`, e.g. `wei.jie.tan@smu.edu.sg`. Mock data is kept in the browser's localStorage so a refresh does not wipe a live demo. Use **Reset demo data** on the login page to start over.

## Configuration

Nothing environment-specific is hard-coded. Settings live in `.env.development`, `.env.production` and `.env.example`.

| Variable | Purpose | Default |
|---|---|---|
| `VITE_USE_MOCK` | `true` answers `/api/*` from the in-memory mock, `false` calls the real backend | `true` in dev, `false` in production |
| `VITE_API_BASE_URL` | Base path of the API | `/api` |
| `VITE_APP_NAME` | Name shown in the navbar and page titles | `StudyBuddy` |
| `BACKEND_URL` | Where `vite dev` proxies `/api` when the mock is off | `http://localhost:8080` |
| `BUILD_OUT_DIR` | Where `npm run build` writes the bundle | `../backend/src/main/resources/static` |

To use the real backend during development, set `VITE_USE_MOCK=false` in `.env.development` (or a `.env.development.local` file) and start the Spring Boot app on port 8080.

## Monolith packaging

`npm run build` writes the bundle straight into Spring Boot's static resources, so the single jar serves both the API and the UI. The router uses hash URLs (`/#/matches`), so Spring only has to serve `index.html`. No server-side route fallback is needed. `pom.xml` is unchanged. Run the frontend build before `mvn package`.

## Architecture

```
src/
  views/          Route-level pages. Orchestrate data loading and user actions.
    student/  leader/  admin/
  components/     Presentational pieces (ui/, matching/, groups/, admin/). Props in, events out.
  stores/         Pinia state: signed-in session, course catalogue.
  composables/    Reusable logic: toasts, confirm dialog, loading/error state.
  api/            One module per resource (*.api.js) on top of a single Axios client (http.js).
    mock/         In-memory backend used when VITE_USE_MOCK=true. Not shipped in production builds.
  router/         Routes and role guards.
  utils/          constants.js (all enums and labels), availability.js, format.js.
```

Design principles applied:

- **Single Responsibility.** Views orchestrate, components render, stores and composables hold state, `*.api.js` files are the only code that knows URLs, and `http.js` is the only code that knows Axios.
- **Open for change, closed to duplication.** Enums, labels and defaults live once in `utils/constants.js`. Availability maths lives once in `utils/availability.js`.
- **Externalised configuration.** Behaviour that differs by environment comes from `.env` files.
- **Same code path for mock and real.** The mock plugs in as an Axios adapter, so the `*.api.js` modules never know which one they are talking to.

## Libraries

| Library | Why |
|---|---|
| Vue 3, Vue Router, Pinia | Required stack. Composition API with `<script setup>` keeps components short. Pinia is the official store. |
| Bootstrap 5 + Bootstrap Icons | Required stack. Installed from npm and themed through Sass variables in `assets/styles/theme.scss`. Only the modal, dropdown and collapse scripts are used. |
| Axios | Interceptors for the auth header and error normalisation. A custom adapter makes the mock trivial. |
| Vite + Sass | Fast dev server, production bundling, Sass for Bootstrap theming. |
| Vitest | Same toolchain as Vite, no extra configuration. |

## Assumptions

- Accounts have two roles, `STUDENT` and `ADMIN`. A **study group leader** is any student who created a group. Leader tools appear on the groups that student leads.
- Admins create accounts, so there is no self-registration page. A new student completes their profile on first login.
- Students can browse study groups and request to join. The brief implies this, because leaders view and answer join requests.
- The brief lists "preferred study mode" twice. The UI calls them **Meeting mode** (in-person / online / either) and **Group format** (one-to-one / small group / either).
- Searching by course or goal and the course filter share one control: the **Course** search field acts as the course filter.
- The backend issues its own login token. Supabase is only the database.
- Populating 10+ courses and 50+ student profiles in Supabase is a backend task. The mock seeds 10 courses and 50 students for the demo.

## API contract

The UI depends on these endpoints, all under `/api`. The mock in `src/api/mock/` implements every one of them, and can be used as an executable reference.

- **Auth.** Send `Authorization: Bearer <token>` on every request except login. A `401` sends the user back to the login page.
- **Errors.** Use the HTTP status plus a JSON body `{ "message": "Human readable text" }`. The message is shown to the user.
- **Privacy.** `contactNumber` must be `null` unless the viewer owns the profile or has an `ACCEPTED` connection with that student. The server enforces this. The UI only displays what it receives.

### Endpoints

| Area | Method and path | Notes |
|---|---|---|
| Auth | `POST /auth/login` | Body `{email, password}` returns `{token, user:{id,name,email,role}}`. `403` for suspended accounts. |
| | `GET /auth/me` | Returns `{id,name,email,role}`. |
| Courses | `GET /courses` | Returns `[{code, name, school}]`. |
| Student | `GET /students/me` | Own profile, see shape below. |
| | `PUT /students/me/profile` | Body `{name, school, programme, yearOfStudy, contactNumber, courses:[code]}`. |
| | `PUT /students/me/preferences` | Body `{course, meetingMode, groupFormat, goals:[..], availability:[slot]}`. |
| | `GET /students/{id}` | Public profile (contact hidden unless connected). |
| Matching | `GET /matches` | Query `course`, `goal`, `day`, `studyMode`, `groupFormat`, all optional. Sorted by `score` descending. |
| Connections | `POST /connections` | Body `{toStudentId, message}` returns `201`. `409` if a pending or active connection already exists. |
| | `GET /connections?view=incoming\|sent\|active` | Returns connection views. |
| | `POST /connections/{id}/accept` and `/decline` | Recipient only. |
| | `DELETE /connections/{id}` | Ends an active connection. `204`. |
| Groups | `GET /groups?course=` | Open groups only. |
| | `GET /groups/mine` | Groups the caller leads or belongs to, any status. |
| | `POST /groups` | Caller becomes leader and first member. `201`. |
| | `GET /groups/{id}` | Includes `members` for the leader and members only. |
| | `PUT /groups/{id}` | Leader only. Course cannot change. Max size cannot go below the member count. |
| | `POST /groups/{id}/close` | Leader only. Rejects pending requests. |
| | `POST /groups/{id}/join-requests` | Body `{message}`. `409` if closed, full, already a member or already pending. |
| | `GET /groups/{id}/join-requests` | Leader only. Pending requests. |
| | `POST /groups/{id}/join-requests/{rid}/accept` and `/reject` | Leader only. Accept returns `409` when the group is full. |
| | `DELETE /groups/{id}/members/{studentId}` | Leader only. The leader cannot be removed. `204`. |
| Admin | `GET /admin/users` | Returns accounts with usage fields. |
| | `POST /admin/users` | Body `{name,email,password,role,status}`. `409` for a duplicate email. |
| | `PUT /admin/users/{id}` | Same body. `password` optional. |
| | `DELETE /admin/users/{id}` | `204`. |
| | `GET` and `PUT /admin/matching-config` | See shape below. |

### Enums

| Name | Values |
|---|---|
| Role | `STUDENT`, `ADMIN` |
| Account status | `ACTIVE`, `SUSPENDED` |
| Meeting mode | `IN_PERSON`, `ONLINE`, `EITHER` |
| Group format | `ONE_TO_ONE`, `SMALL_GROUP`, `EITHER` |
| Study goal | `CONCEPT_REVIEW`, `PROBLEM_SOLVING`, `EXAM_PREPARATION`, `PROJECT_DISCUSSION` |
| Day | `MON` `TUE` `WED` `THU` `FRI` `SAT` `SUN` |
| Strategy | `BALANCED`, `AVAILABILITY_FIRST`, `COURSE_FIRST` |
| Connection status (in a match) | `NONE`, `PENDING_SENT`, `PENDING_RECEIVED`, `CONNECTED` |

### Shapes

```jsonc
// Availability slot (24-hour "HH:mm")
{ "day": "WED", "start": "19:00", "end": "21:00" }

// Student profile (GET /students/me, GET /students/{id})
{
  "id": 2, "name": "Aisha Rahman", "email": "…",            // email only on /me
  "school": "…", "programme": "Information Systems", "yearOfStudy": 2,
  "courses": ["IS442", "IS212"],
  "contactNumber": "+65 9123 4567",                          // null when hidden
  "contactVisible": true,
  "connectionStatus": "NONE", "connectionId": null,
  "preferences": {
    "course": "IS442", "meetingMode": "IN_PERSON", "groupFormat": "SMALL_GROUP",
    "goals": ["EXAM_PREPARATION"], "availability": [ /* slots */ ]
  }
}

// Match (GET /matches)
{
  "student": { /* public profile */ },
  "score": 94,                                                // 0-100 integer
  "breakdown": { "course": 1, "availability": 1, "studyMode": 0.75, "studyGoal": 1, "groupSize": 0.75 },  // each 0-1
  "overlapMinutes": 240,
  "sharedCourses": ["IS442"],
  "connectionStatus": "NONE"
}

// Connection (GET /connections)
{ "id": 7, "status": "PENDING", "direction": "INCOMING", "message": "…",
  "createdAt": "ISO-8601", "respondedAt": null, "other": { /* public profile */ } }

// Group summary (GET /groups, /groups/mine); GET /groups/{id} adds "members"
{
  "id": 1, "name": "…", "description": "…", "courseCode": "IS442", "courseName": "…",
  "goals": [ ], "meetingMode": "IN_PERSON", "availability": [ ],
  "maxSize": 5, "memberCount": 3, "status": "OPEN",           // OPEN | CLOSED
  "leader": { "id": 2, "name": "…" },
  "myRole": "LEADER",                                         // LEADER | MEMBER | NONE
  "myRequestStatus": "NONE",                                  // PENDING | NONE
  "members": [ { "id": 2, "name": "…", "programme": "…", "yearOfStudy": 2, "isLeader": true } ]
}

// Join request (GET /groups/{id}/join-requests)
{ "id": 1, "student": { "id": 3, "name": "…", "programme": "…", "yearOfStudy": 2 }, "message": "…", "createdAt": "ISO-8601" }

// Account (GET /admin/users)
{ "id": 3, "name": "…", "email": "…", "role": "STUDENT", "status": "ACTIVE",
  "lastLogin": "ISO-8601 or null", "createdAt": "ISO-8601", "connectionCount": 1, "groupCount": 2 }

// Matching configuration (GET / PUT /admin/matching-config)
{
  "strategy": "BALANCED",
  "criteria": [
    { "key": "course", "enabled": true, "weight": 30 },       // keys: course, availability, studyMode, studyGoal, groupSize
    { "key": "availability", "enabled": true, "weight": 25 }  // weight: integer 0-100
  ]
}
```

## Mock matching rules

The mock engine (`src/api/mock/matching.js`) is a stand-in for the real engine and follows Appendix A of the brief.

- Each criterion scores 0 to 1: course match, hours of weekly overlap (3 hours or more scores 1), meeting-mode and group-format compatibility (identical 1, either side flexible 0.75, conflicting 0), and shared goals (overlap over union).
- The overall score is the weighted average of the enabled criteria, as a whole number out of 100.
- **Availability-First** and **Course-First** first group candidates into quarter-point tiers of that criterion, then sort by overall score inside each tier.

## Testing

`npm test` runs unit tests for the availability utilities, the mock matching engine and the mock API. The API tests cover the privacy rule for contact numbers, role protection, join-request rules and seeded data size.
