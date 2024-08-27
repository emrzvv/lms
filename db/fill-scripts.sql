-- Вставка пользователей
INSERT INTO "users" ("id", "username", "email", "password_hash", "roles", "registered_at")
VALUES
    (gen_random_uuid(), 'admin', 'admin@example.com', '$2a$10$eYhl/jG6Mmm94sJ9ET5SCeyCpQgcx5t6SIVR6Q3G6Z/xETFvPOFRC', ARRAY['admin', 'user'], current_date),
    (gen_random_uuid(), 'tutor', 'tutor@example.com', '$2a$10$yULudg5ui6gqAeVnLoJs1uGrNa24kuuyynaT4oyq27GT99qo38oxe', ARRAY['tutor', 'user'], current_date),
    (gen_random_uuid(), 'user1', 'user1@example.com', '$2a$10$PFTZkAHbhSu1hfD64plKm.ppmX3VgmlL7fo1934JQ8qMGvGQ84SeO', ARRAY['user'], current_date),
    (gen_random_uuid(), 'user2', 'user2@example.com', '$2a$10$Xj2Q9R2UF4GZQoVQhxYZTejZtv4rIQLq2LNDDAJ6VDXrEtz6WIBQS', ARRAY['user'], current_date);

-- Вставка курсов
INSERT INTO "courses" ("id", "name", "creator_id", "short_description", "description", "preview_image_url", "estimated_time", "is_published", "is_free")
VALUES
    (gen_random_uuid(), 'Course 1', (SELECT "id" FROM "users" WHERE "username" = 'tutor'), 'Short description 1', 'Detailed description of Course 1', 'https://techcrazee.com/wp-content/uploads/2021/04/digital-marketing-courses.png', 10, true, true),
    (gen_random_uuid(), 'Course 2', (SELECT "id" FROM "users" WHERE "username" = 'tutor'), 'Short description 2', 'Detailed description of Course 2', 'https://avatars.mds.yandex.net/i?id=9e5c3438bdbc60b0e1a45c34fbf0406d_l-9052188-images-thumbs&n=13', 15, true, false);

-- Вставка модулей для каждого курса
INSERT INTO "modules" ("id", "name", "course_id", "description", "order")
VALUES
    -- Модули для курса 1
    (gen_random_uuid(), 'Module 1-1', (SELECT "id" FROM "courses" WHERE "name" = 'Course 1'), 'Description of Module 1-1', 1),
    (gen_random_uuid(), 'Module 1-2', (SELECT "id" FROM "courses" WHERE "name" = 'Course 1'), 'Description of Module 1-2', 2),
    (gen_random_uuid(), 'Module 1-3', (SELECT "id" FROM "courses" WHERE "name" = 'Course 1'), 'Description of Module 1-3', 3),

    -- Модули для курса 2
    (gen_random_uuid(), 'Module 2-1', (SELECT "id" FROM "courses" WHERE "name" = 'Course 2'), 'Description of Module 2-1', 1),
    (gen_random_uuid(), 'Module 2-2', (SELECT "id" FROM "courses" WHERE "name" = 'Course 2'), 'Description of Module 2-2', 2),
    (gen_random_uuid(), 'Module 2-3', (SELECT "id" FROM "courses" WHERE "name" = 'Course 2'), 'Description of Module 2-3', 3);

-- Вставка уроков для каждого модуля
INSERT INTO "lessons" ("id", "name", "module_id", "order", "content")
VALUES
    -- Уроки для модуля 1-1
    (gen_random_uuid(), 'Lesson 1-1-1', (SELECT "id" FROM "modules" WHERE "name" = 'Module 1-1'), 1, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),
    (gen_random_uuid(), 'Lesson 1-1-2', (SELECT "id" FROM "modules" WHERE "name" = 'Module 1-1'), 2, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),

    -- Уроки для модуля 1-2
    (gen_random_uuid(), 'Lesson 1-2-1', (SELECT "id" FROM "modules" WHERE "name" = 'Module 1-2'), 1, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),
    (gen_random_uuid(), 'Lesson 1-2-2', (SELECT "id" FROM "modules" WHERE "name" = 'Module 1-2'), 2, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),

    -- Уроки для модуля 1-3
    (gen_random_uuid(), 'Lesson 1-3-1', (SELECT "id" FROM "modules" WHERE "name" = 'Module 1-3'), 1, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),
    (gen_random_uuid(), 'Lesson 1-3-2', (SELECT "id" FROM "modules" WHERE "name" = 'Module 1-3'), 2, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),

    -- Уроки для модуля 2-1
    (gen_random_uuid(), 'Lesson 2-1-1', (SELECT "id" FROM "modules" WHERE "name" = 'Module 2-1'), 1, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),
    (gen_random_uuid(), 'Lesson 2-1-2', (SELECT "id" FROM "modules" WHERE "name" = 'Module 2-1'), 2, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),

    -- Уроки для модуля 2-2
    (gen_random_uuid(), 'Lesson 2-2-1', (SELECT "id" FROM "modules" WHERE "name" = 'Module 2-2'), 1, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),
    (gen_random_uuid(), 'Lesson 2-2-2', (SELECT "id" FROM "modules" WHERE "name" = 'Module 2-2'), 2, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),

    -- Уроки для модуля 2-3
    (gen_random_uuid(), 'Lesson 2-3-1', (SELECT "id" FROM "modules" WHERE "name" = 'Module 2-3'), 1, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb),
    (gen_random_uuid(), 'Lesson 2-3-2', (SELECT "id" FROM "modules" WHERE "name" = 'Module 2-3'), 2, '{"time": 1723556509398, "blocks": [], "version": "2.30.4"}'::jsonb);

-- Вставка задач для уроков
INSERT INTO "tasks" ("id", "lesson_id", "question", "points", "task_type")
VALUES
    -- Задачи для уроков в курсе 1
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-1-1'), 'What is the capital of France?', 5, 'simple_answer'),
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-1-2'), 'What is 2+2?', 5, 'simple_answer'),

    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-2-1'), 'Select the correct option 1-2-1', 10, 'choose_one'),
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-2-2'), 'What is the color of the sky?', 5, 'simple_answer'),

    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-3-1'), 'Choose the correct options 1-3-1', 15, 'choose_many'),
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-3-2'), 'What is the capital of Germany?', 5, 'simple_answer'),

    -- Задачи для уроков в курсе 2
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-1-1'), 'What is the capital of Italy?', 5, 'simple_answer'),
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-1-2'), 'What is 3+5?', 5, 'simple_answer'),

    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-2-1'), 'Select the correct option 2-2-1', 10, 'choose_one'),
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-2-2'), 'What is the color of the grass?', 5, 'simple_answer'),

    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-3-1'), 'Choose the correct options 2-3-1', 15, 'choose_many'),
    (gen_random_uuid(), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-3-2'), 'What is the capital of Spain?', 5, 'simple_answer');

-- Вставка задач с простым ответом
INSERT INTO "tasks_simple_answer" ("id", "suggested_answer")
VALUES
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of France?'), 'Paris'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is 2+2?'), '4'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is the color of the sky?'), 'Blue'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of Germany?'), 'Berlin'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of Italy?'), 'Rome'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is 3+5?'), '8'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is the color of the grass?'), 'Green'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of Spain?'), 'Madrid');

-- Вставка задач с выбором одного ответа
INSERT INTO "tasks_choose_one" ("id", "variants", "suggested_variant")
VALUES
    ((SELECT "id" FROM "tasks" WHERE "question" = 'Select the correct option 1-2-1'), ARRAY['Option 1', 'Option 2', 'Option 3'], 'Option 2'),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'Select the correct option 2-2-1'), ARRAY['Option A', 'Option B', 'Option C'], 'Option B');

-- Вставка задач с выбором нескольких ответов
INSERT INTO "tasks_choose_many" ("id", "variants", "suggested_variants")
VALUES
    ((SELECT "id" FROM "tasks" WHERE "question" = 'Choose the correct options 1-3-1'), ARRAY['Option 1', 'Option 2', 'Option 3', 'Option 4'], ARRAY['Option 1', 'Option 3']),
    ((SELECT "id" FROM "tasks" WHERE "question" = 'Choose the correct options 2-3-1'), ARRAY['Option A', 'Option B', 'Option C', 'Option D'], ARRAY['Option B', 'Option D']);

-- Вставка связей пользователей с курсами
INSERT INTO "users_courses" ("user_id", "course_id", "able_to_edit")
VALUES
    ((SELECT "id" FROM "users" WHERE "username" = 'tutor'), (SELECT "id" FROM "courses" WHERE "name" = 'Course 1'), true),
    ((SELECT "id" FROM "users" WHERE "username" = 'tutor'), (SELECT "id" FROM "courses" WHERE "name" = 'Course 2'), true),
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "courses" WHERE "name" = 'Course 1'), false),
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "courses" WHERE "name" = 'Course 2'), false);

-- Вставка связей пользователей с уроками
INSERT INTO "users_lessons" ("user_id", "lesson_id")
VALUES
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-1-1')),
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-1-2')),
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 1-2-1')),
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-1-1')),
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-1-2')),
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "lessons" WHERE "name" = 'Lesson 2-2-1'));

-- Вставка связей пользователей с задачами
INSERT INTO "users_tasks" ("user_id", "task_id", "points", "task_type")
VALUES
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of France?'), 5, 'simple_answer'),
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "tasks" WHERE "question" = 'What is 2+2?'), 5, 'simple_answer'),
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of Italy?'), 5, 'simple_answer'),
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "tasks" WHERE "question" = 'Select the correct option 1-2-1'), 10, 'choose_one');

-- Вставка ответов пользователей на задачи с простым ответом
INSERT INTO "users_tasks_simple_answer" ("user_id", "task_id", "answer")
VALUES
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of France?'), 'Paris'),
    ((SELECT "id" FROM "users" WHERE "username" = 'user1'), (SELECT "id" FROM "tasks" WHERE "question" = 'What is 2+2?'), '4'),
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "tasks" WHERE "question" = 'What is the capital of Italy?'), 'Rome');

-- Вставка ответов пользователей на задачи с выбором одного ответа
INSERT INTO "users_tasks_choose_one" ("user_id", "task_id", "selected_variant")
VALUES
    ((SELECT "id" FROM "users" WHERE "username" = 'user2'), (SELECT "id" FROM "tasks" WHERE "question" = 'Select the correct option 1-2-1'), 'Option B');