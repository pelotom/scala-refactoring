package scala.tools.refactoring.tests.transformation

import scala.tools.refactoring.tests.util.TestHelper
import org.junit.Test
import org.junit.Assert._
import scala.tools.refactoring.transformation._

@Test
class TransformTest extends TestHelper with Transform {

  @Test
  def testReplaceTrees() = {
    val ts = List(1, 2, 3, 4, 5)
    
    assertEquals(List(1, 6, 3, 4, 5), replaceTrees(ts, 2 :: Nil, 6 :: Nil))
    assertEquals(List(6, 2, 3, 4, 5), replaceTrees(ts, 1 :: Nil, 6 :: Nil))
    assertEquals(List(1, 2, 3, 4, 6, 7), replaceTrees(ts, 5 :: Nil, 6 :: 7 :: Nil))
    assertEquals(List(1, 2, 3, 4, 5), replaceTrees(ts, 6 :: Nil, 1 :: Nil))
  }
}

