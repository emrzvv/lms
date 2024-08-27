CREATE TABLE "users" (
                         "id" uuid PRIMARY KEY,
                         "username" text UNIQUE NOT NULL CHECK (char_length(username) >= 3 AND char_length(username) <= 30),
                         email text UNIQUE NOT NULL CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    "password_hash" text NOT NULL,
    "roles" text[] NOT NULL DEFAULT '{"user"}' CHECK (array_length(roles, 1) > 0 AND roles <@ ARRAY['user', 'admin', 'tutor']::text[]),
    "registered_at" date NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE "courses" (
                           "id" uuid PRIMARY KEY,
                           "name" text UNIQUE NOT NULL CHECK (char_length(name) <= 128),
                           "creator_id" uuid NOT NULL,
                           "short_description" text NOT NULL CHECK (char_length(short_description) <= 300),
                           "description" text NOT NULL CHECK (char_length(description) <= 2000),
                           "preview_image_url" text,
                           "estimated_time" integer NOT NULL CHECK (estimated_time > 0),
                           "created_at" timestamp NOT NULL DEFAULT now(),
                           "is_published" bool NOT NULL DEFAULT FALSE,
                           "is_free" bool NOT NULL DEFAULT FALSE,
                           CONSTRAINT "fk_creator_id" FOREIGN KEY ("creator_id") REFERENCES "users" ("id") ON DELETE RESTRICT
);

CREATE TABLE "modules" (
                           "id" uuid PRIMARY KEY,
                           "name" text NOT NULL CHECK (char_length(name) <= 128),
                           "course_id" uuid NOT NULL,
                           "description" text CHECK (char_length(description) <= 2000),
                           "order" integer NOT NULL CHECK ("order" > 0),
                           "created_at" timestamp NOT NULL DEFAULT now(),
                           CONSTRAINT "fk_course_id" FOREIGN KEY ("course_id") REFERENCES "courses" ("id") ON DELETE CASCADE
);

CREATE TABLE "lessons" (
                           "id" uuid PRIMARY KEY,
                           "name" text NOT NULL CHECK (char_length(name) <= 128),
                           "module_id" uuid NOT NULL,
                           "order" integer NOT NULL CHECK ("order" > 0),
                           "content" jsonb NOT NULL,
                           "created_at" timestamp NOT NULL DEFAULT now(),
                           "pass_points_percentage" integer NOT NULL DEFAULT 100 CHECK (pass_points_percentage >= 0 AND pass_points_percentage <= 100),
                           CONSTRAINT "fk_module_id" FOREIGN KEY ("module_id") REFERENCES "modules" ("id") ON DELETE CASCADE
);

CREATE TABLE "tasks" (
                         "id" uuid PRIMARY KEY,
                         "lesson_id" uuid NOT NULL,
                         "question" text NOT NULL CHECK (char_length(question) <= 1000),
                         "points" integer NOT NULL DEFAULT 0 CHECK (points >= 0),
                         "task_type" text NOT NULL CHECK (task_type IN ('simple_answer', 'choose_one', 'choose_many')),
                         CONSTRAINT "fk_task_lesson_id" FOREIGN KEY ("lesson_id") REFERENCES "lessons" ("id") ON DELETE CASCADE
);

CREATE TABLE "tasks_simple_answer" (
                                       "id" uuid PRIMARY KEY,
                                       "suggested_answer" text NOT NULL CHECK (char_length(suggested_answer) <= 100),
                                       CONSTRAINT "fk_task_id" FOREIGN KEY ("id") REFERENCES "tasks" ("id") ON DELETE CASCADE
);

CREATE TABLE "tasks_choose_one" (
                                    "id" uuid PRIMARY KEY,
                                    "variants" text[] NOT NULL,
                                    "suggested_variant" text NOT NULL,
                                    CONSTRAINT "fk_task_id" FOREIGN KEY ("id") REFERENCES "tasks" ("id") ON DELETE CASCADE,
                                    CHECK ("suggested_variant" = ANY ("variants"))
);

CREATE TABLE "tasks_choose_many" (
                                     "id" uuid PRIMARY KEY,
                                     "variants" text[] NOT NULL,
                                     "suggested_variants" text[] NOT NULL,
                                     CONSTRAINT "fk_task_id" FOREIGN KEY ("id") REFERENCES "tasks" ("id") ON DELETE CASCADE,
                                     CHECK ("suggested_variants" <@ "variants")
);

CREATE TABLE "users_courses" (
                                 "user_id" uuid NOT NULL,
                                 "course_id" uuid NOT NULL,
                                 "able_to_edit" bool NOT NULL DEFAULT FALSE,
                                 PRIMARY KEY ("user_id", "course_id"),
                                 CONSTRAINT "fk_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE,
                                 CONSTRAINT "fk_course_id" FOREIGN KEY ("course_id") REFERENCES "courses" ("id") ON DELETE CASCADE
);

CREATE TABLE "users_lessons" (
                                 "user_id" uuid NOT NULL,
                                 "lesson_id" uuid NOT NULL,
                                 PRIMARY KEY ("user_id", "lesson_id"),
                                 CONSTRAINT "fk_user_lesson_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE,
                                 CONSTRAINT "fk_user_lesson_lesson_id" FOREIGN KEY ("lesson_id") REFERENCES "lessons" ("id") ON DELETE CASCADE
);

CREATE TABLE "users_tasks" (
                               "user_id" uuid NOT NULL,
                               "task_id" uuid NOT NULL,
                               "points" integer NOT NULL DEFAULT 0,
                               "submitted_at" timestamp NOT NULL DEFAULT now(),
                               "task_type" text NOT NULL,
                               PRIMARY KEY ("user_id", "task_id"),
                               CONSTRAINT "fk_user_task_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE,
                               CONSTRAINT "fk_user_task_task_id" FOREIGN KEY ("task_id") REFERENCES "tasks" ("id") ON DELETE CASCADE
);

CREATE TABLE "users_tasks_simple_answer" (
                                             "user_id" uuid NOT NULL,
                                             "task_id" uuid NOT NULL,
                                             "answer" text NOT NULL CHECK (char_length(answer) <= 100),
                                             PRIMARY KEY ("user_id", "task_id"),
                                             CONSTRAINT "fk_user_task_simple_answer" FOREIGN KEY ("user_id", "task_id") REFERENCES "users_tasks" ("user_id", "task_id") ON DELETE CASCADE
);

CREATE TABLE "users_tasks_choose_one" (
                                          "user_id" uuid NOT NULL,
                                          "task_id" uuid NOT NULL,
                                          "selected_variant" text NOT NULL,
                                          PRIMARY KEY ("user_id", "task_id"),
                                          CONSTRAINT "fk_user_task_simple_answer" FOREIGN KEY ("user_id", "task_id") REFERENCES "users_tasks" ("user_id", "task_id") ON DELETE CASCADE
);

CREATE TABLE "users_tasks_choose_many" (
                                           "user_id" uuid NOT NULL,
                                           "task_id" uuid NOT NULL,
                                           "selected_variants" text[] NOT NULL,
                                           PRIMARY KEY ("user_id", "task_id"),
                                           CONSTRAINT "fk_user_task_simple_answer" FOREIGN KEY ("user_id", "task_id") REFERENCES "users_tasks" ("user_id", "task_id") ON DELETE CASCADE
);

-- TRIGGERS --
CREATE OR REPLACE FUNCTION update_able_to_edit ()
    RETURNS TRIGGER
    AS $$
BEGIN
    IF 'tutor' = ANY (OLD.roles) AND NOT 'tutor' = ANY (NEW.roles) THEN
UPDATE
    users_courses
SET
    able_to_edit = FALSE
WHERE
    user_id = OLD.id;
END IF;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_able_to_edit
    BEFORE UPDATE OF roles ON users
    FOR EACH ROW
    WHEN (OLD.roles IS DISTINCT FROM NEW.roles)
    EXECUTE FUNCTION update_able_to_edit ();

CREATE OR REPLACE FUNCTION check_tutor_role ()
    RETURNS TRIGGER
    AS $$
BEGIN
    IF NEW.able_to_edit = TRUE THEN
        PERFORM
            1
        FROM
            users
        WHERE
            id = NEW.user_id
            AND 'tutor' = ANY (roles);

        IF NOT FOUND THEN
            RAISE EXCEPTION 'User % does not have the tutor role and cannot be granted edit rights on course %', NEW.user_id, NEW.course_id;
END IF;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_tutor_role
    BEFORE INSERT OR UPDATE ON users_courses
                         FOR EACH ROW
                         EXECUTE FUNCTION check_tutor_role ();

CREATE OR REPLACE FUNCTION check_course_published_status ()
    RETURNS TRIGGER
    AS $$
BEGIN
    PERFORM
1
    FROM
        courses
    WHERE
        id = NEW.course_id
        AND is_published = FALSE;

    IF FOUND AND NEW.able_to_edit = FALSE THEN
        RAISE EXCEPTION 'Cannot insert into users_courses for unpublished course % with able_to_edit = false', NEW.course_id;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_course_published_status
    BEFORE INSERT ON users_courses
    FOR EACH ROW
    EXECUTE FUNCTION check_course_published_status ();

CREATE OR REPLACE FUNCTION reassign_course_creator ()
    RETURNS TRIGGER
    AS $$
DECLARE
course RECORD;
    new_creator_id uuid;
BEGIN
FOR course IN
SELECT
    id
FROM
    courses
WHERE
    creator_id = OLD.id LOOP
SELECT
    user_id INTO new_creator_id
FROM
    users_courses
WHERE
    course_id = course.id
  AND able_to_edit = TRUE
    LIMIT 1;

IF new_creator_id IS NOT NULL THEN
UPDATE
    courses
SET
    creator_id = new_creator_id
WHERE
    id = course.id;
ELSE
SELECT
    id INTO new_creator_id
FROM
    users
WHERE
    'admin' = ANY (roles)
    LIMIT 1;

IF new_creator_id IS NOT NULL THEN
UPDATE
    courses
SET
    creator_id = new_creator_id
WHERE
    id = course.id;
ELSE
                    RAISE EXCEPTION 'No suitable user found to assign as the creator for course %', course.id;
END IF;
END IF;
END LOOP;

RETURN OLD;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_reassign_course_creator
    BEFORE DELETE ON users
    FOR EACH ROW
    EXECUTE FUNCTION reassign_course_creator ();

CREATE OR REPLACE FUNCTION check_unique_variants_choose_one ()
    RETURNS TRIGGER
    AS $$
BEGIN
    IF array_length(NEW.variants, 1) <> array_length(ARRAY ( SELECT DISTINCT
            UNNEST(NEW.variants)), 1) THEN
        RAISE EXCEPTION 'Variants in tasks_choose_one cannot contain duplicates: %', NEW.variants;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_unique_variants_choose_one
    BEFORE INSERT OR UPDATE ON tasks_choose_one
                         FOR EACH ROW
                         EXECUTE FUNCTION check_unique_variants_choose_one ();

CREATE OR REPLACE FUNCTION check_unique_variants_choose_many ()
    RETURNS TRIGGER
    AS $$
BEGIN
    IF array_length(NEW.variants, 1) <> array_length(ARRAY ( SELECT DISTINCT
            UNNEST(NEW.variants)), 1) THEN
        RAISE EXCEPTION 'Variants in tasks_choose_many cannot contain duplicates: %', NEW.variants;
END IF;

    IF array_length(NEW.suggested_variants, 1) <> array_length(ARRAY ( SELECT DISTINCT
            UNNEST(NEW.suggested_variants)), 1) THEN
        RAISE EXCEPTION 'Suggested variants in tasks_choose_many cannot contain duplicates: %', NEW.suggested_variants;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_unique_variants_choose_many
    BEFORE INSERT OR UPDATE ON tasks_choose_many
                         FOR EACH ROW
                         EXECUTE FUNCTION check_unique_variants_choose_many ();

CREATE OR REPLACE FUNCTION check_unique_selected_variants_choose_many ()
    RETURNS TRIGGER
    AS $$
BEGIN
    IF array_length(NEW.selected_variants, 1) <> array_length(ARRAY ( SELECT DISTINCT
            UNNEST(NEW.selected_variants)), 1) THEN
        RAISE EXCEPTION 'Selected variants in users_tasks_choose_many cannot contain duplicates: %', NEW.selected_variants;
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_unique_selected_variants_choose_many
    BEFORE INSERT OR UPDATE ON users_tasks_choose_many
                         FOR EACH ROW
                         EXECUTE FUNCTION check_unique_selected_variants_choose_many ();

