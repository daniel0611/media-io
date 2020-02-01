package de.dani09.mediaio.data

object MovieGrouping extends Enumeration {
  val NONE, YEAR, HALF_YEAR = Value

  def parse(s: String): Value = {
    val enumName = s.toUpperCase.replaceAll("-", "_")

    values
      .find(x => x.toString == enumName)
      .getOrElse(NONE)
  }
}
