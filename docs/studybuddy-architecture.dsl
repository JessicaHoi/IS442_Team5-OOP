// StudyBuddy C4 architecture model (Structurizr DSL).
// Paste this entire file into https://playground.structurizr.com/ to view the diagrams.
//
// The frontend and backend are separate during development. For production, the
// Vue build is packaged as static assets in the Spring Boot application.
// The backend uses either local H2 or Supabase PostgreSQL, not both at once.
// This model shows backend-enabled mode. With VITE_USE_MOCK=true, the Axios
// adapter answers requests inside the browser without the Java backend or database.
// Local development uses HTTP. HTTPS requires deployment infrastructure.

workspace "StudyBuddy" "Architecture of the Study Buddy Matcher System." {
    model {
        student = person "Student" "Finds study buddies, manages preferences, joins groups, and schedules study sessions. A student may also lead groups."
        administrator = person "System Administrator" "Manages user accounts and configures and previews matching strategies."

        studyBuddy = softwareSystem "StudyBuddy" "A study buddy matching and group collaboration system." {
            webApp = container "Web Application" "Single-page application used by students, group leaders, and administrators." "Vue 3, JavaScript, Bootstrap 5" {
                tags "Web Browser"
            }

            api = container "Application API" "Provides authenticated REST endpoints and implements StudyBuddy business workflows." "Java 21, Spring Boot, Spring MVC" {
                restControllers = component "REST Controllers" "Expose and validate HTTP endpoints, then delegate requests to application services." "Spring MVC"
                identity = component "Authentication and Identity" "Registers and authenticates accounts, resolves bearer-token sessions, and enforces roles." "Spring services"
                studentCourses = component "Student and Course Services" "Manage student profiles, study preferences, and the course catalogue." "Spring services"
                matchingConnections = component "Matching and Connections" "Rank compatible students and manage buddy connection requests." "Spring services"
                groupsSessions = component "Groups and Study Sessions" "Manage study groups, membership, group availability, sessions, and RSVPs." "Spring services"
                scheduling = component "Scheduling" "Calculate shared availability, suggested meeting windows, and group heatmaps." "Spring services"
                administration = component "Administration" "Manage user accounts and matching configuration and previews." "Spring services"
                repositories = component "Persistence Repositories" "Load and store domain entities through the relational database." "Spring Data JPA"
                telegramIntegration = component "Telegram Integration" "Link Telegram accounts and coordinate group chats, session cards, reminders, and bot updates when enabled." "Telegram Bot API client"
            }

            localDatabase = container "Local Database" "Persistent application data in the default local configuration." "H2, file-backed relational database" {
                tags "Database"
            }
        }

        supabase = softwareSystem "Supabase PostgreSQL" "Managed PostgreSQL database used by the shared Supabase profile." {
            tags "Database"
        }

        telegram = softwareSystem "Telegram" "External messaging platform used for optional account linking, group chats, and study-session reminders." {
            tags "External"
        }

        student -> webApp "Uses the StudyBuddy interface" "HTTP(S)"
        administrator -> webApp "Uses account and matching administration features" "HTTP(S)"
        webApp -> api "Calls authenticated and public endpoints" "JSON over HTTP(S) (/api)"
        api -> localDatabase "Reads and writes application data (default local configuration)" "JPA/JDBC"
        api -> supabase "Reads and writes application data (Supabase profile only)" "JDBC over PostgreSQL"
        api -> telegram "Uses the Bot API when the optional integration is enabled" "HTTPS"

        webApp -> restControllers "Calls REST endpoints" "JSON over HTTP(S)"
        restControllers -> identity "Uses current-user resolution; the request interceptor authenticates before dispatch"
        restControllers -> studentCourses "Delegates profile and course requests"
        restControllers -> matchingConnections "Delegates matching and connection requests"
        restControllers -> groupsSessions "Delegates group and study-session requests"
        restControllers -> scheduling "Delegates availability and scheduling requests"
        restControllers -> administration "Delegates administrator requests"
        identity -> repositories "Reads account data and records account changes"
        studentCourses -> repositories "Reads and writes profiles, preferences, and courses"
        matchingConnections -> repositories "Reads matching data and stores connections"
        groupsSessions -> repositories "Reads and writes groups, memberships, and sessions"
        scheduling -> repositories "Reads group and member availability"
        administration -> repositories "Reads and writes accounts and matching configuration"
        groupsSessions -> telegramIntegration "Coordinates group chat and session messaging when enabled"
        telegramIntegration -> repositories "Reads and updates Telegram links and chat state"
        repositories -> localDatabase "Persists application data (default local configuration)" "JPA/JDBC"
        repositories -> supabase "Persists application data (Supabase profile only)" "JPA/JDBC"
        telegramIntegration -> telegram "Calls the Bot API and polls for bot updates" "HTTPS"
    }

    views {
        systemContext studyBuddy "SystemContext" {
            include *
            autoLayout lr
        }

        container studyBuddy "ContainerView" {
            include *
            autoLayout lr
        }

        component api "BackendComponents" {
            include *
            autoLayout lr
        }

        styles {
            element "Person" {
                shape person
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Database" {
                shape cylinder
            }
            element "External" {
                background #999999
                color #ffffff
            }
            relationship "Relationship" {
                routing orthogonal
            }
        }
    }
}
