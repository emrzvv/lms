CREATE TABLE "users" (
  "id" uuid PRIMARY KEY,
  "username" varchar UNIQUE NOT NULL,
  "email" varchar UNIQUE NOT NULL,
  "role" varchar NOT NULL DEFAULT 'student',
  "registered_at" date NOT NULL DEFAULT current_date
);

CREATE TABLE "courses" (
  "id" uuid PRIMARY KEY,
  "name" varchar UNIQUE NOT NULL,
  "creator_id" uuid,
  "description" varchar,
  "created_at" timestamp NOT NULL DEFAULT 'now()',
  "last_modified_at" timestamp NOT NULL DEFAULT 'now()'
);

CREATE TABLE "users_courses" (
  "user_id" uuid NOT NULL,
  "course_id" uuid NOT NULL,
  "rating" integer,

  CONSTRAINT "fk_user_id" FOREIGN KEY ("user_id") REFERENCES "users"("id"),
  CONSTRAINT "fk_course_id" FOREIGN KEY ("course_id") REFERENCES "courses"("id"),
  CONSTRAINT "pk_user_course" PRIMARY KEY ("user_id", "course_id")
);

CREATE TABLE "users_lessons" (
  "user_id" uuid,
  "lesson_id" uuid,
  PRIMARY KEY ("user_id", "lesson_id")
);

CREATE TABLE "users_tasks" (
  "user_id" uuid,
  "task_id" uuid,
  "answer" varchar,
  PRIMARY KEY ("user_id", "task_id")
);

CREATE TABLE "categories" (
  "id" uuid PRIMARY KEY,
  "name" varchar NOT NULL,
  "description" varchar
);

CREATE TABLE "courses_categories" (
  "course_id" uuid,
  "category_id" uuid,
  PRIMARY KEY ("course_id", "category_id")
);

CREATE TABLE "modules" (
  "id" uuid PRIMARY KEY,
  "name" varchar,
  "description" varchar,
  "created_at" timestamp NOT NULL DEFAULT 'now()',
  "last_modified_at" timestamp NOT NULL DEFAULT 'now()'
);

CREATE TABLE "lessons" (
  "id" uuid PRIMARY KEY,
  "name" varchar NOT NULL,
  "points" integer NOT NULL
);

CREATE TABLE "content" (
  "id" uuid PRIMARY KEY,
  "page_index" integer NOT NULL,
  "text" varchar NOT NULL,
  "image_url" varchar NOT NULL,
  "video" varchar NOT NULL
);

CREATE TABLE "lessons_content" (
  "lesson_id" uuid,
  "content_id" uuid,
  PRIMARY KEY ("lesson_id", "content_id")
);

CREATE TABLE "tasks" (
  "id" uuid PRIMARY KEY,
  "question" varchar
);

ALTER TABLE "courses" ADD FOREIGN KEY ("creator_id") REFERENCES "users" ("id");

ALTER TABLE "users_lessons" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "users_tasks" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");


ALTER TABLE "modules" ADD FOREIGN KEY ("id") REFERENCES "courses" ("id");

ALTER TABLE "courses_categories" ADD FOREIGN KEY ("course_id") REFERENCES "courses" ("id");

ALTER TABLE "courses_categories" ADD FOREIGN KEY ("category_id") REFERENCES "categories" ("id");

ALTER TABLE "lessons" ADD FOREIGN KEY ("id") REFERENCES "modules" ("id");

ALTER TABLE "users_lessons" ADD FOREIGN KEY ("lesson_id") REFERENCES "lessons" ("id");

ALTER TABLE "lessons_content" ADD FOREIGN KEY ("lesson_id") REFERENCES "lessons" ("id");

ALTER TABLE "lessons_content" ADD FOREIGN KEY ("content_id") REFERENCES "content" ("id");

ALTER TABLE "content" ADD FOREIGN KEY ("id") REFERENCES "tasks" ("id");

ALTER TABLE "users_tasks" ADD FOREIGN KEY ("task_id") REFERENCES "tasks" ("id");