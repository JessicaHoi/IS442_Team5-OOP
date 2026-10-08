#!/usr/bin/env python3
"""
Generate seed-demo-data.sql from src/main/resources/seed/demo-data.json.

The JSON is the single source of the demo data. The front-end mock and the
backend's DemoDataSeeder use the same data, so regenerate this file whenever
the JSON changes:

    python3 backend/supabase/generate_seed_sql.py

Ids are UUIDv5 values derived from stable keys, so the output is identical on
every run and easy to diff.
"""

import json
import uuid
from pathlib import Path

HERE = Path(__file__).resolve().parent
SOURCE = HERE.parent / "src" / "main" / "resources" / "seed" / "demo-data.json"
OUTPUT = HERE / "seed-demo-data.sql"

# BCrypt hash of "password" (cost 10), made with Spring's BCryptPasswordEncoder.
DEMO_PASSWORD_HASH = "$2a$10$9MLTiPrspHv5dWnqtZxOhuwOeNZL0p9A6J7EUVSCdKDid5lazq10W"

# Same defaults as DEFAULT_MATCHING_CONFIG in frontend/src/utils/constants.js.
DEFAULT_STRATEGY = "BALANCED"
DEFAULT_CRITERIA = [("course", 30), ("availability", 25), ("studyMode", 15), ("studyGoal", 20), ("groupSize", 10)]

DAYS = {"MON": "MONDAY", "TUE": "TUESDAY", "WED": "WEDNESDAY", "THU": "THURSDAY",
        "FRI": "FRIDAY", "SAT": "SATURDAY", "SUN": "SUNDAY"}

# Every table and column the inserts rely on (as Hibernate names them).
REQUIRED_COLUMNS = {
    "users": ["id", "email", "password"],
    "student": ["id", "name", "school", "program", "year_of_study", "contact_num"],
    "systemadministrator": ["id"],
    "study_group_leader": ["id"],
    "course": ["course_code", "course_name", "school"],
    "student_course": ["student_id", "course_code"],
    "study_preference": ["id", "student_id", "course_code", "study_mode", "group_preference"],
    "study_preference_goal": ["study_preference_id", "study_goal"],
    "study_preference_availability": ["study_preference_id", "day_of_week", "start_time", "end_time"],
    "study_group": ["id", "group_name", "description", "course_code", "leader_id", "study_mode", "max_size",
                    "status", "created_at"],
    "study_group_member": ["study_group_id", "student_id"],
    "study_group_goal": ["study_group_id", "study_goal"],
    "study_group_availability": ["study_group_id", "day_of_week", "start_time", "end_time"],
    "membership_request": ["id", "student_id", "study_group_id", "message", "status", "request_date"],
    "match_request": ["id", "sender_id", "receiver_id", "message", "status", "request_date"],
    "study_buddy_connection": ["id", "requester_id", "recipient_id", "status", "start_date"],
    "matching_configuration": ["id", "strategy"],
    "matching_criteria": ["matching_configuration_id", "criteria_name", "weight"],
}

NAMESPACE = uuid.UUID("5b0c3f8e-2f4a-4c55-9a51-442442442442")


def uid(*parts):
    return str(uuid.uuid5(NAMESPACE, "/".join(str(part) for part in parts)))


def q(value):
    """SQL literal."""
    if value is None:
        return "null"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def days_ago(days):
    return f"now() - interval '{days} days'"


def insert(table, columns, rows):
    if not rows:
        return ""
    values = ",\n".join("  (" + ", ".join(row) + ")" for row in rows)
    return f"insert into {table} ({', '.join(columns)}) values\n{values};\n\n"


def slot_rows(owner_id, slots):
    return [[q(owner_id), q(DAYS[s["day"]]), q(s["start"]), q(s["end"])] for s in slots]


def build(data):
    student_ids = {s["key"]: uid("student", s["email"]) for s in data["students"]}
    leader_keys = {g["leaderKey"] for g in data["groups"]}
    admin_ids = [uid("admin", a["email"]) for a in data["administrators"]]

    users, students, student_courses = [], [], []
    preferences, preference_goals, preference_slots = [], [], []
    for s in data["students"]:
        sid = student_ids[s["key"]]
        users.append([q(sid), q(s["email"]), q(DEMO_PASSWORD_HASH)])
        students.append([q(sid), q(s["name"]), q(s["school"]), q(s["programme"]), q(s["yearOfStudy"]),
                         q(s["contactNumber"])])
        student_courses += [[q(sid), q(code)] for code in s["courses"]]
        for p in s["preferences"]:
            pid = uid("preference", s["email"], p["course"])
            preferences.append([q(pid), q(sid), q(p["course"]), q(p["meetingMode"]), q(p["groupFormat"])])
            preference_goals += [[q(pid), q(goal)] for goal in p["goals"]]
            preference_slots += slot_rows(pid, p["availability"])
    for admin, aid in zip(data["administrators"], admin_ids):
        users.append([q(aid), q(admin["email"]), q(DEMO_PASSWORD_HASH)])

    # Connections: every connection has its original match request; accepted ones also get an active connection.
    match_requests, connections = [], []
    for index, c in enumerate(data["connections"]):
        sender, receiver = student_ids[c["fromKey"]], student_ids[c["toKey"]]
        age = index + 1
        match_requests.append([q(uid("match-request", index)), q(sender), q(receiver), q(c["message"]),
                               q(c["status"]), days_ago(age)])
        if c["status"] == "ACCEPTED":
            connections.append([q(uid("connection", index)), q(sender), q(receiver), q("ACTIVE"), days_ago(age - 1)])

    # Groups: the leader is a member; seeded members joined through an accepted request.
    groups, members, group_goals, group_slots, membership_requests = [], [], [], [], []
    for g in data["groups"]:
        gid = uid("group", g["name"])
        closed = g["status"] == "CLOSED"
        groups.append([q(gid), q(g["name"]), q(g["description"]), q(g["courseCode"]),
                       q(student_ids[g["leaderKey"]]), q(g["meetingMode"]), q(g["maxSize"]), q(g["status"]),
                       days_ago(14)])
        members += [[q(gid), q(student_ids[key])] for key in [g["leaderKey"], *g["memberKeys"]]]
        group_goals += [[q(gid), q(goal)] for goal in g["goals"]]
        group_slots += slot_rows(gid, g["availability"])
        for key in g["memberKeys"]:
            membership_requests.append([q(uid("membership", g["name"], key)), q(student_ids[key]), q(gid), q(""),
                                        q("ACCEPTED"), days_ago(13)])
        for r in g["joinRequests"]:
            # Closing a group rejects its pending requests.
            membership_requests.append([q(uid("membership", g["name"], r["studentKey"])),
                                        q(student_ids[r["studentKey"]]), q(gid), q(r["message"]),
                                        q("REJECTED" if closed else "PENDING"), days_ago(1)])

    config_id = uid("matching-configuration")

    out = []
    out.append(f"""-- Study Buddy demo data for Supabase (PostgreSQL).
-- GENERATED by backend/supabase/generate_seed_sql.py from
-- backend/src/main/resources/seed/demo-data.json. Do not edit by hand.
--
-- {len(data['courses'])} courses, {len(data['students'])} students, {len(data['administrators'])} administrator,
-- {len(data['groups'])} study groups, {len(data['connections'])} connection requests, default matching configuration.
-- Every account's password is "password".
--
-- WARNING: this WIPES every Study Buddy table and reloads the demo data.
-- Run it in the Supabase SQL Editor (or psql) after the tables exist.
-- See backend/supabase/README.md.

begin;

-- 1. Stop with a clear message if the schema is missing or out of date.
do $$
declare
  missing text;
begin
  select string_agg(required.table_name || '.' || required.column_name, ', '
                    order by required.table_name, required.column_name)
    into missing
    from (values
""")
    pairs = [f"      ({q(t)}, {q(c)})" for t, cols in REQUIRED_COLUMNS.items() for c in cols]
    out.append(",\n".join(pairs) + "\n")
    out.append("""    ) as required(table_name, column_name)
   where not exists (
     select 1 from information_schema.columns c
      where c.table_schema = 'public'
        and c.table_name = required.table_name
        and c.column_name = required.column_name);

  if missing is not null then
    raise exception 'Study Buddy schema is missing: %', missing
      using hint = 'Start the backend once with SUPABASE_DDL_AUTO=update (see backend/supabase/README.md), then run this script again.';
  end if;
end $$;

-- 2. Wipe every Study Buddy table.
truncate table
""")
    out.append(",\n".join(f"  {t}" for t in REQUIRED_COLUMNS) + "\ncascade;\n\n-- 3. Load the demo data.\n")

    out.append(insert("course", ["course_code", "course_name", "school"],
                      [[q(c["code"]), q(c["name"]), q(c["school"])] for c in data["courses"]]))
    out.append(insert("users", ["id", "email", "password"], users))
    out.append(insert("systemadministrator", ["id"], [[q(aid)] for aid in admin_ids]))
    out.append(insert("student", ["id", "name", "school", "program", "year_of_study", "contact_num"], students))
    out.append(insert("study_group_leader", ["id"], [[q(student_ids[key])] for key in sorted(leader_keys)]))
    out.append(insert("student_course", ["student_id", "course_code"], student_courses))
    out.append(insert("study_preference", ["id", "student_id", "course_code", "study_mode", "group_preference"],
                      preferences))
    out.append(insert("study_preference_goal", ["study_preference_id", "study_goal"], preference_goals))
    out.append(insert("study_preference_availability",
                      ["study_preference_id", "day_of_week", "start_time", "end_time"], preference_slots))
    out.append(insert("match_request", ["id", "sender_id", "receiver_id", "message", "status", "request_date"],
                      match_requests))
    out.append(insert("study_buddy_connection", ["id", "requester_id", "recipient_id", "status", "start_date"],
                      connections))
    out.append(insert("study_group", ["id", "group_name", "description", "course_code", "leader_id", "study_mode",
                                      "max_size", "status", "created_at"], groups))
    out.append(insert("study_group_member", ["study_group_id", "student_id"], members))
    out.append(insert("study_group_goal", ["study_group_id", "study_goal"], group_goals))
    out.append(insert("study_group_availability", ["study_group_id", "day_of_week", "start_time", "end_time"],
                      group_slots))
    out.append(insert("membership_request", ["id", "student_id", "study_group_id", "message", "status",
                                             "request_date"], membership_requests))
    out.append(insert("matching_configuration", ["id", "strategy"], [[q(config_id), q(DEFAULT_STRATEGY)]]))
    out.append(insert("matching_criteria", ["matching_configuration_id", "criteria_name", "weight"],
                      [[q(config_id), q(name), q(weight)] for name, weight in DEFAULT_CRITERIA]))
    out.append("commit;\n")
    return "".join(out)


def main():
    data = json.loads(SOURCE.read_text(encoding="utf-8"))
    OUTPUT.write_text(build(data), encoding="utf-8")
    print(f"Wrote {OUTPUT.relative_to(HERE.parent.parent)}")


if __name__ == "__main__":
    main()
