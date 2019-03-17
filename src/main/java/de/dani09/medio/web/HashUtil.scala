package de.dani09.medio.web

import java.security.MessageDigest

object HashUtil {

  private lazy val mdSha256 = MessageDigest.getInstance("SHA-256")

  /**
    * Calculates an sha256 hash and returns the first 7 digits
    *
    * @param s the text to hash
    * @return the first 7 digits of the hash
    */
  def sha256Short(s: String): String = sha256(s).substring(0, 7)

  /**
    * Calculates an sha256 hash
    *
    * @param s the text to hash
    * @return the hash as an string
    */
  def sha256(s: String): String = {
    mdSha256.digest(s.getBytes)
      .map("%02x".format(_))
      .mkString
  }
}
