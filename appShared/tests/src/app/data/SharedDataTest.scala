package app.data

import framework.utils.FrameworkTestSuite

class SharedDataTest extends FrameworkTestSuite {
  test("it works") {
    assertEquals(SharedData("foo").name, "foo")
  }
}
