package db.model

import java.util.UUID

trait Task {
  def id: UUID
  def lessonId: UUID
  def question: String
  def points: Int
  def taskType: String
}
trait TaskDB {}

case class BaseTask(id: UUID,
                    lessonId: UUID,
                    question: String,
                    points: Int,
                    taskType: String) extends Task

case class TaskSimpleAnswerDB(id: UUID,
                              suggestedAnswer: String) extends TaskDB

case class TaskSimpleAnswer(id: UUID,
                            lessonId: UUID,
                            question: String,
                            points: Int,
                            taskType: String,
                            suggestedAnswer: String) extends Task

case class TaskChooseOneDB(id: UUID,
                         variants: Seq[String],
                         suggestedVariant: String) extends TaskDB

case class TaskChooseOne(id: UUID,
                         lessonId: UUID,
                         question: String,
                         points: Int,
                         taskType: String,
                         variants: Seq[String],
                         suggestedVariant: String) extends Task

case class TaskChooseManyDB(id: UUID,
                            variants: Seq[String],
                            suggestedVariants: Seq[String]) extends TaskDB

case class TaskChooseMany(id: UUID,
                          lessonId: UUID,
                          question: String,
                          points: Int,
                          taskType: String,
                          variants: Seq[String],
                          suggestedVariants: Seq[String]) extends Task

object TaskFactory {
  def fromBaseTask(baseTask: BaseTask, taskDB: TaskDB): Task = taskDB match {
    case TaskSimpleAnswerDB(_, suggestedAnswer) =>
      TaskSimpleAnswer(
        id = baseTask.id,
        lessonId = baseTask.lessonId,
        question = baseTask.question,
        points = baseTask.points,
        taskType = baseTask.taskType,
        suggestedAnswer = suggestedAnswer
      )

    case TaskChooseOneDB(_, variants, suggestedVariant) =>
      TaskChooseOne(
        id = baseTask.id,
        lessonId = baseTask.lessonId,
        question = baseTask.question,
        points = baseTask.points,
        taskType = baseTask.taskType,
        variants = variants,
        suggestedVariant = suggestedVariant
      )

    case TaskChooseManyDB(_, variants, suggestedVariants) =>
      TaskChooseMany(
        id = baseTask.id,
        lessonId = baseTask.lessonId,
        question = baseTask.question,
        points = baseTask.points,
        taskType = baseTask.taskType,
        variants = variants,
        suggestedVariants = suggestedVariants
      )

    case _ =>
      throw new IllegalArgumentException("Unknown TaskDB type")
  }
}