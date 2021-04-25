/* Generated By:JJTree: Do not edit this line. ASTNotExpr.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package analyzer.ast;

import java.util.Vector;

public
class ASTNotExpr extends SimpleNode {
  public ASTNotExpr(int id) {
    super(id);
  }

  public ASTNotExpr(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, Object data) {

    return
    visitor.visit(this, data);
  }

  private Vector<String> m_ops = new Vector<>();
  public void addOp(String o) { m_ops.add(o); }
  public Vector getOps() { return m_ops; }

}
/* JavaCC - OriginalChecksum=1383181a44720e6cb18943cac758dc11 (do not edit this line) */