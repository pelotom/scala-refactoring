package scala.tools.refactoring.regeneration

import scala.collection.mutable.HashMap
trait FragmentRepository {

  self: Fragments =>
  
  val global: scala.tools.nsc.interactive.Global
  
  // cache, optimize, whatever!
  class FragmentRepository(val root: Scope) {
        
    def exists(part: Fragment) = root.exists(_ == part)
    
    def scopeIndentation(part: Fragment) = {
      visit(part) map (_.indentation)
    }
    
    def findScope(tree: global.Tree) = {
      find {
        case c: WithTree if c.tree.pos.sameRange(tree.pos) => 
          true
        case _ => 
          false
      }
    }
    
    def scopeIndentation(tree: global.Tree) = {
      find(scope => scope.children.exists {
        case c: WithTree if c.tree.pos.sameRange(tree.pos) => 
          true
        case _ => 
          false
      }) map (_.indentation)
    }
    
    def getNext(part: Fragment) = {
      get(part, (xs => xs), (_, _))
    }
  
    def getPrevious(part: Fragment) = {
      get(part, (_.reverse), (_1, _2) => (_2, _1))
    }
  
    private def get(
        part: Fragment, 
        findPart: List[Fragment] => List[Fragment], 
        mkReturn: (OriginalSourceFragment, OriginalSourceFragment) => (OriginalSourceFragment, OriginalSourceFragment)):
      Option[(OriginalSourceFragment, OriginalSourceFragment)] = {
      
      val neighbourhood = visit(part).getOrElse(return None).children
      
      findPart(neighbourhood).dropWhile(_ != part) match {
        case (partInOriginal: OriginalSourceFragment) :: (next: OriginalSourceFragment) :: _ => 
          Some(mkReturn(partInOriginal, next))
        case _ => None
      }
    }
        
    private def visit(what: Fragment) = find(_.children.exists(_ == what))
    
    private def find(compare: Scope => Boolean): Option[Scope] = {
      root.find {
        case s: Scope if compare(s) => true
        case _ => false
      } map (_.asInstanceOf[Scope])
    }
  }
}