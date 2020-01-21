package de.dani09.mediaio.data

object MovieGrouping extends Enumeration {
  // TODO comment everything
  val NONE, YEAR = Value

  def parse(s: String): Value = {
    values
      .find(x => x.toString equalsIgnoreCase s)
      .getOrElse(NONE)
  }
}
