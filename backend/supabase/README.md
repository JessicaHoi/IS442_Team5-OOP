# Supabase demo data

`seed-demo-data.sql` loads the demo data into the team's Supabase database: 10 courses, 50 students (17 with two course preferences), 1 administrator, 6 study groups with members and join requests, 12 buddy requests (7 accepted) and the default matching configuration. It's the same data the front-end mock and the local H2 database use. Every account's password is `password`.

> **The script wipes every Study Buddy table before loading.** Anything else in the shared database is lost, so tell the team before you run it. Run it again any time to reset the demo.

## Load it

1. **Bring the schema up to date.** The script only inserts data. It expects the tables Hibernate creates from the entities, and stops with a "schema is missing" message if any are missing. From `backend/`, with `backend/.env` set up (see the main README):

   ```bash
   SUPABASE_DDL_AUTO=update ./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase
   ```

   In Windows PowerShell, use `$env:SUPABASE_DDL_AUTO="update"; .\mvnw spring-boot:run "-Dspring-boot.run.profiles=supabase"`. Wait for `Started StudybuddyApplication`, check for schema-update errors, then stop it with Ctrl+C and run `Remove-Item Env:SUPABASE_DDL_AUTO` to restore the default validation mode. If schema updates fail, see [Changing an entity](../../README.md#changing-an-entity).

2. **Run the script.** In the Supabase dashboard, open **SQL Editor**, then **New query**. Paste the whole of `seed-demo-data.sql` and click **Run**. It runs in one transaction, so it loads everything or nothing. With `psql`, run `psql "<connection string>" -v ON_ERROR_STOP=1 -f backend/supabase/seed-demo-data.sql`.

3. **Use it.** Start the backend normally (`./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase`). To make the frontend use it instead of the mock, put `VITE_USE_MOCK=false` in `frontend/.env.development.local`, as described in the main README's Quick start.

## Change the demo data

Don't edit the SQL by hand. The demo data lives in `backend/src/main/resources/seed/demo-data.json`, which the local `DemoDataSeeder` also reads. After changing the JSON, regenerate the SQL from the repository root:

```bash
python3 backend/supabase/generate_seed_sql.py
```

Ids are derived from stable keys (email, group name), so regenerating gives the same ids, and the diff only shows what changed. If an entity gains a required column, add it to `REQUIRED_COLUMNS` and to the matching insert in the generator.

## Account fields

Account status, creation and last-login timestamps, and administrator names are mapped by the entities. Local H2 updates automatically on startup. Existing Supabase databases use the same one-run Hibernate update described above; no separate migration script is required for these additions. To preserve existing data, run only the schema-update step, not the demo seed script.

The required new columns use `@ColumnDefault` so Hibernate can add them to populated tables. Existing accounts receive `ACTIVE` status and the schema-update time as their creation timestamp; administrator names default to `System Administrator`. New accounts receive their creation timestamp when saved. Last login starts as `NULL`. Login recording and suspension enforcement are separate from these persistence changes.

Account persistence and upgrading an existing H2 schema are covered by backend tests.

The account-field schema update and generated seed were verified against a disposable local PostgreSQL 18 database. Existing account details were preserved, and the seed script ran twice successfully. The shared Supabase database was not modified.
