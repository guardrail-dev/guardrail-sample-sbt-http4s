package io.chrisdavenport.guardrailhttp4ssample

import munit.CatsEffectSuite
import cats.effect._

class MainSpec extends CatsEffectSuite {

  test("Tests should run") {
    assertEquals(ExitCode.Success, ExitCode.Success)
  }

}
