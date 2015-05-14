package models.enum

sealed trait NotificationTypes

object NotificationTypes extends EnumBase[NotificationTypes] {
  case object Comment       extends NotificationTypes
  case object EntryLike     extends NotificationTypes
  case object CommentLike   extends NotificationTypes
  case object Follow        extends NotificationTypes
  case object Mention       extends NotificationTypes
  case object DirectMessage extends NotificationTypes

  val values: Set[NotificationTypes] = Set(
    Comment,
    EntryLike,
    CommentLike,
    Follow,
    Mention,
    DirectMessage
  )
}
