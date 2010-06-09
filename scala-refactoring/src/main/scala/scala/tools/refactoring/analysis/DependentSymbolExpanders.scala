/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package scala.tools.refactoring
package analysis

/**
 * Provides various traits that are used by the indexer
 * to expand symbols; that is, to find symbols that are
 * related to each other. For example, it finds overriden
 * methods in subclasses.
 * */
trait DependentSymbolExpanders {
  
  this: Indexes =>
  
  import global._
  
  /**
   * The basic trait that is extended by the
   * concrete expanders.
   * 
   * The expanders themselves need access to the
   * index, but have to take care not to
   * */
  trait DependentSymbolExpander {
    
    this: TrivialIndexLookup =>
    
    def expand(s: Symbol): List[Symbol] = List(s)
  }
  
  trait ExpandGetterSetters extends DependentSymbolExpander {
    
    this: TrivialIndexLookup =>
    
    abstract override def expand(s: Symbol) = super.expand(s) ++ (s match {
      case s if s.isGetterOrSetter =>
        s.accessed :: Nil
      case s if s != NoSymbol && s.owner != NoSymbol =>
        s.getter(s.owner) :: s.setter(s.owner) :: Nil
      case _ =>
        Nil
    })
  }
  
  trait SuperConstructorParameters extends DependentSymbolExpander {
    
    this: TrivialIndexLookup =>
        
    abstract override def expand(s: Symbol) = super.expand(s) ++ (s match {
      
      case s if s != NoSymbol && s.owner.isClass && s.isGetterOrSetter =>

        declaration(s.owner) collect {
          case ClassDef(_, _, _, Template(_, _, body)) => body collect {
            case d @ DefDef(_, _, _, _, _, Block(stats, _)) if d.symbol.isConstructor => stats collect {
              case Apply(_, args) => args collect {
                case symTree: SymTree if symTree.symbol.nameString == s.nameString => symTree.symbol
              }
            } flatten
          } flatten
        } flatten

    case _ => Nil
    })
  }
  
  trait SymbolCompanion extends DependentSymbolExpander {
    
    this: TrivialIndexLookup =>
    
    abstract override def expand(s: Symbol) = super.expand(s) ++ List(s.companionSymbol)
  }
  
  trait OverridesInClassHierarchy extends DependentSymbolExpander {
    
    this: TrivialIndexLookup =>
    
    abstract override def expand(s: Symbol) = super.expand(s) ++ (s match {
      case s: global.TermSymbol if s.owner.isClass =>
      
        def allSubClasses(clazz: Symbol) = allDefinedSymbols.filter {
          case decl if decl.isClass => 
            // it seems like I can't compare these symbols with ==?
            decl.ancestors.exists(t => t.pos.sameRange(clazz.pos) && t.pos.source == clazz.pos.source)
          case _ => false
        }
                
        val overrides = allSubClasses(s.owner) map {
          case otherClassDecl =>
            try {
              s overriddenSymbol otherClassDecl
            } catch {
              case e: Error =>
                // ?
                throw e
            }
        }

        overrides
      case _ => Nil
    })
  } 
  
  trait SameSymbolPosition extends DependentSymbolExpander {
    
    this: TrivialIndexLookup =>
    
    abstract override def expand(s: Symbol) = super.expand(s) ++ (allSymbols collect {
      case sym if sym.pos.sameRange(s.pos) && sym.pos.source.file.name == s.pos.source.file.name && !sym.pos.isTransparent =>
        sym
    })
  }
}