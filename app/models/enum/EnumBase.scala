package models.enum

trait EnumBase[E] {
  val values: Set[E]

  protected lazy val valueMap: Map[String, E] = values.map(v => v.toString -> v).toMap

  def withName(name: String): Option[E] = valueMap.get(name)
}
