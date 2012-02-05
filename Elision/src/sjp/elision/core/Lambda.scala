/* Copyright (c) 2012 by Stacy Prowell (sprowell@gmail.com).
 * All rights reserved.  http://stacyprowell.com
 *       _ _     _
 *   ___| (_)___(_) ___  _ __
 *  / _ \ | / __| |/ _ \| '_ \
 * |  __/ | \__ \ | (_) | | | |
 *  \___|_|_|___/_|\___/|_| |_|
 */
package sjp.elision.core

/**
 * A lambda creates an operator that binds a single variable in a term.
 * @param deBrujinIndex			The De Brujin index.
 * @param lvar							The lambda variable which must match the De Brujin
 * 													index.
 * @param body							The lambda body.
 */
case class Lambda(deBrujinIndex: Int, lvar: Variable, body: BasicAtom)
extends BasicAtom {
  // The type is a mapping from one type to another.
	// val theType =
	//  Apply(OperatorLibrary.MAP, AtomList(Seq(lvar.theType, body.theType)))
  val theType = TypeUniverse
	  	
  def tryMatchWithoutTypes(subject: BasicAtom, binds: Bindings) =
    subject match {
	  case Lambda(odbi, olvar, obody) => if (olvar == lvar) {
	    body.tryMatch(obody, binds) match {
	      case fail: Fail =>
	        Fail("Lambda bodies do not match.", this, subject)
	      case mat: Match => mat
	      case mat: Many => mat
	    }
	  } else Fail("Lambda variables do not match.", this, subject)
	  case _ => Fail("Lambdas only match other lambdas.", this, subject)
	}

  def rewrite(binds: Bindings): (BasicAtom, Boolean) =
    body.rewrite(binds) match {
	    case (newatom, changed) if changed => (Lambda(lvar, newatom), true)
	    case _ => (this, false)
	  }
  
  def toParseString = "\\" + lvar.toParseString + "." + body.toParseString
}

/**
 * Companion object with convenient methods to create lambdas.
 */
object Lambda {
  def apply(lvar: Variable, body: BasicAtom): Lambda = {
    /*
     * Note on De Brujin indices.
     * 
     * This apply method is the point at which new De Brujin indices are
     * created.  Basically when a lambda is being created, the De Brujin
     * index of the body is checked.  This is incremented and a new variable
     * created based on the index.  The original variable is then replaced
     * in the body with the new variable.
     * 
     * For example:
     * \$x.fred($x,$y)  becomes  \$:1.fred($:1,$y)
     * \$x.(\$:1.fred($:1,$x))  becomes  \$:2.(\$:1.fred($:1,$:2))
     * 
     * There is a special case to consider, however.  Suppose we have a
     * lambda with this form.
     * 
     * \$x.$body
     * 
     * Lambdas represent functions, and in this case the lambda represents
     * a constant function: \$x.$body.7 = $body.
     * 
     * The difficulty arises if we later rewrite the body.  Consider this.
     * 
     * \$x.$body  becomes  \$:1.$body
     * 
     * We then rewrite $body to \$x.fred($x,$y), itself rewritten to be
     * \$:1.fred($:1,$y), with De Brujin index of one.  Thus since we are
     * building a new lambda (thanks to rewriting the body) we create new
     * variable $:2 and rewrite the body mapping the old variable $:1 to
     * the new one $:2.  We get the following.
     * 
     * \$:2.\$:1.fred(\$:2,$y)
     * 
     * This isn't at all what we want.  What we really want is the following.
     * 
     * \$:1.$body      $body = \$:1.fred($:1,$y)
     * 
     * \$:2.\$:1.fred($:1,$y)
     * 
     * To get this we compute the index of the outer lambda as one more than
     * the index of the body (so it gets index 2).  We then do not rewrite the
     * lambda body since the lambda variable is already a De Brujin index.
     * 
     * The simple answer is that we do not rewrite De Brujin indices.  Terms
     * are immutable, and the De Brujin indices were created (correctly) when
     * the term was originally created.  So...  We reject rewriting De Brujin
     * indices when creating lambdas.
     * 
     * Now, when evaluating lambdas, we have to rewrite De Brujin indices.  So
     * we only block rewriting of one De Brujin index to a different De Brujin
     * index.  That logic can be found in the Variable class.
     */
    
    // First compute the De Brujin index of the term.  It is equal to one
    // greater than the maximum index of the body.
    val deBrujinIndex = body.deBrujinIndex + 1
    
    // Now make a new De Brujin variable for the index.
    val newvar = new Variable(lvar.theType, ":"+deBrujinIndex) {
      override val isDeBrujinIndex = true
    }
    
    // Bind the old variable to the new one.
    var binds = new Bindings
    binds += (lvar.name -> newvar)
    val (newbody, _) = body.rewrite(binds)
    
    // Make and return the new lambda.
    Lambda(deBrujinIndex, newvar, newbody)
  }
}