-- Account tables before account-management fields were added, used by the H2 schema-upgrade test.
CREATE TABLE users (
    id uuid PRIMARY KEY,
    email varchar(255) NOT NULL UNIQUE,
    password varchar(255) NOT NULL
);

CREATE TABLE systemadministrator (
    id uuid PRIMARY KEY REFERENCES users(id)
);

INSERT INTO users (id, email, password)
VALUES ('00000000-0000-0000-0000-000000000001', 'legacy.admin@test.studybuddy.example', 'legacy-password-hash');

INSERT INTO systemadministrator (id)
VALUES ('00000000-0000-0000-0000-000000000001');
