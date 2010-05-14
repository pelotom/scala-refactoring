package scala.tools.refactoring.sourcegen

/**
 * Transformations is the basis for all refactoring transformations.
 * 
 * A transformation is a Function from X ⇒ Option[Y], and can be
 * combined with other transformations in two ways:
 *   andThen - which applies the second transformation only if the 
 *             first one was successful, i.e. returned Some(_).
 *   orElse  - which is applied only when the first transformation
 *             returned None.
 * 
 * X any Y are typically instances of global.Tree, but this is not 
 * enforced. Once a transformations is assembled, it has to be applied
 * to a tree and its children. The function `all` applies a transformation
 * to the children of a tree. To keep the transformations generic, the 
 * trees have to be convertible to (X ⇒ X) ⇒ X. In the case of the 
 * trees, the tree has to apply the transformation to all children and
 * return one single tree.
 * 
 * Additional functions are provided that apply a transformation top-down
 * or bottom-up.
 * */
object Transformations {
    
  abstract class Transformation[X, Y] extends (X ⇒ Option[Y]) { 
    
    self ⇒

    def apply(x: X): Option[Y]
  
    def combineRecursively[Z](f: (T[X,Z], Y) => Z) = new T[X, Z] {
      
      def apply(x: X): Option[Z] = {
            
        def rec: T[X, Z] = self &> transform[Y, Z] {
          case y => f(rec, y)
        }
        
        self(x) map (f(rec, _))
      }
    }

    def andThen[Z](t: ⇒ T[Y, Z]) = new T[X, Z] {
      def apply(x: X): Option[Z] = {
        self(x) flatMap t
      }
    }
    def &>[Z](t: ⇒ T[Y, Z]) = andThen(t)

    def orElse(t: ⇒ T[X, Y]) = new T[X, Y] {
      def apply(x: X): Option[Y] = {
        self(x) orElse t(x)
      }
    }
    def |>(t: ⇒ T[X, Y]) = orElse(t)
  }
  
  type T[X, Y] = Transformation[X, Y]

  /**
   * Construct a transformation from a partial function; this is the
   * most commonly used way to create new transformations, for example
   * like:
   * 
   *   val reverse_all_class_members = transform[Tree, Tree] {
   *     case t: Template => t.copy(body = t.body.reverse) setPos t.pos
   *   }
   * */
  def transform[X, Y](f: PartialFunction[X, Y]) = new T[X, Y] {
    def apply(x: X): Option[Y] = f lift x
  }
  
  /**
   * We often want to use transformations as predicates, which execute
   * the next transformations if the result is true. For example:
   *   
   *   val tree_with_range_pos = filter[Tree] {
   *     case t: Tree => t.pos.isRange
   *   }
   *   
   * We can then use the preidacte like this:
   *   tree_with_range_pos andThen do_something_with_the_tree orElse nothing
   * */
  def predicate[X](f: ⇒ PartialFunction[X, Boolean]) = new T[X, X] {
    def apply(t: X): Option[X] = if (f.isDefinedAt(t) && f(t)) Some(t) else None
  }

  /**
   * Always succeeds and returns the input unchanged.
   * */
  def succeed[X] = new T[X, X] {
    def apply(in: X): Option[X] = Some(in)
  }

  def id[X] = succeed[X]

  /**
   * Always fails, independent of the input.
   * */
  def fail[X] = new T[X, X] {
    def apply(in: X): Option[X] = None
  }

  /**
   * Success and fail can be used to implement `not` 
   * */
  def not[X](t: ⇒ T[X, X]) = t &> fail |> succeed
  def !  [X](t: ⇒ T[X, X]) = not(t)
  
  /**
   * Applies a transformation to all subtrees of a tree T, 
   * returning a new tree,typically of the same kind as T.
   * 
   * If the transformation fails on one child, abort and
   * fail the whole application.
   * */
  def all[X <% (X ⇒ Y) ⇒ Y, Y](t: ⇒ T[X, Y]) = new T[X, Y] {
    def apply(in: X): Option[Y] = {
      Some(in(child => t(child) getOrElse (return None)))
    }
  }
  def ∀  [X <% (X ⇒ Y) ⇒ Y, Y](t: ⇒ T[X, Y]) = all(t)

  /**
   * Applies a transformation to all subtrees of a tree T, 
   * returning a new tree,typically of the same kind as T.
   * 
   * If the transformation fails on one child, apply the 
   * identity transformation `id` and don't fail, unlike
   * `all`.
   * */
  def any [X <% (X ⇒ X) ⇒ X](t: T[X, X]) = ∀(t |> id[X]) //possible?
  def ⊆   [X <% (X ⇒ X) ⇒ X](t: T[X, X]) = any(t)
  
  /**
   * Applies a transformation top-down, that is, it applies
   * the transformation to the tree T and then passes the
   * transformed T to all children. The consequence is that
   * the children "see" their new parent.
   * */
  def ↓       [X <% (X ⇒ X) ⇒ X](t: ⇒ T[X, X]): T[X, X] = t &> ∀(↓(t))
  def topdown [X <% (X ⇒ X) ⇒ X](t: ⇒ T[X, X]) = ↓(t)
  def preorder[X <% (X ⇒ X) ⇒ X](t: ⇒ T[X, X]) = ↓(t)

   /**
   * Applies a transformation bottom-up, that is, it applies
   * the transformation to the children of the tree first and
   * then to their parent. The consequence is that the parent
   * "sees" its transformed children.
   * */ 
  def ↑        [X <% (X ⇒ X) ⇒ X](t: ⇒ T[X, X]): T[X, X] = ∀(↑(t)) &> t
  def bottomup [X <% (X ⇒ X) ⇒ X](t: ⇒ T[X, X]) = ↑(t)
  def postorder[X <% (X ⇒ X) ⇒ X](t: ⇒ T[X, X]) = ↑(t)
  
  
   /**
   * Creates a transformation that always returns the value x.
   * */ 
  def constant[X](x: X) = transform[X, X] {
    case _ => x
  }

}