CREATE TABLE "users" (
  "id" uuid PRIMARY KEY,
  "username" text UNIQUE NOT NULL,
  "email" text UNIQUE NOT NULL,
  "roles" text[] NOT NULL DEFAULT '{"user"}',
  "registered_at" date NOT NULL DEFAULT current_date
);

CREATE TABLE "courses" (
  "id" uuid PRIMARY KEY,
  "name" varchar UNIQUE NOT NULL,
  "creator_id" uuid NOT NULL,
  "short_description" text NOT NULL,
  "description" text NOT NULL,
  "preview_image_url" text,
  "estimated_time" integer NOT NULL,
  "created_at" timestamp NOT NULL DEFAULT now(),
  "last_modified_at" timestamp NOT NULL DEFAULT now(),
  "is_published" bool NOT NULL DEFAULT false,
  CONSTRAINT "fk_course_creator_id" FOREIGN KEY ("creator_id") REFERENCES "users"("id")
);

CREATE TABLE "categories" (
  "id" uuid PRIMARY KEY,
  "name" varchar NOT NULL,
  "description" varchar
);

CREATE TABLE "modules" (
  "id" uuid PRIMARY KEY,
  "name" varchar,
  "description" varchar,
  "created_at" timestamp NOT NULL DEFAULT now(),
  "last_modified_at" timestamp NOT NULL DEFAULT now()
);

CREATE TABLE "lessons" (
  "id" uuid PRIMARY KEY,
  "name" varchar NOT NULL,
  "points" integer NOT NULL,
  "content" json NOT NULL,
  "created_at" timestamp NOT NULL DEFAULT now(),
  "last_modified_at" timestamp NOT NULL DEFAULT now(),
  CONSTRAINT "fk_lesson_module_id" FOREIGN KEY ("id") REFERENCES "modules"("id")
);


CREATE TABLE "tasks" (
  "id" uuid PRIMARY KEY,
  "lesson_id" uuid NOT NULL,
  "question" varchar,
  CONSTRAINT "fk_task_lesson_id" FOREIGN KEY ("lesson_id") REFERENCES "lessons"("id")
);

CREATE TABLE "users_courses" (
  "user_id" uuid NOT NULL,
  "course_id" uuid NOT NULL,
  "rating" integer,
  PRIMARY KEY ("user_id", "course_id"),
  CONSTRAINT "fk_user_id" FOREIGN KEY ("user_id") REFERENCES "users"("id"),
  CONSTRAINT "fk_course_id" FOREIGN KEY ("course_id") REFERENCES "courses"("id")
);

CREATE TABLE "users_lessons" (
  "user_id" uuid NOT NULL,
  "lesson_id" uuid NOT NULL,
  PRIMARY KEY ("user_id", "lesson_id"),
  CONSTRAINT "fk_user_lesson_user_id" FOREIGN KEY ("user_id") REFERENCES "users"("id"),
  CONSTRAINT "fk_user_lesson_lesson_id" FOREIGN KEY ("lesson_id") REFERENCES "lessons"("id")
);

CREATE TABLE "users_tasks" (
  "user_id" uuid NOT NULL,
  "task_id" uuid NOT NULL,
  "answer" varchar,
  PRIMARY KEY ("user_id", "task_id"),
  CONSTRAINT "fk_user_task_user_id" FOREIGN KEY ("user_id") REFERENCES "users"("id"),
  CONSTRAINT "fk_user_task_task_id" FOREIGN KEY ("task_id") REFERENCES "tasks"("id")
);

CREATE TABLE "courses_categories" (
  "course_id" uuid NOT NULL,
  "category_id" uuid NOT NULL,
  PRIMARY KEY ("course_id", "category_id"),
  CONSTRAINT "fk_course_category_course_id" FOREIGN KEY ("course_id") REFERENCES "courses"("id"),
  CONSTRAINT "fk_course_category_category_id" FOREIGN KEY ("category_id") REFERENCES "categories"("id")
);