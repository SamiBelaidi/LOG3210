/* Generated By:JJTree: Do not edit this line. ASTExpr.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package analyzer.ast;

public
class ASTExpr extends SimpleNode {
  public ASTExpr(int id) {
    super(id);
  }

  public ASTExpr(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, Object data) {

    return
    visitor.visit(this, data);
  }

  private String m_value = null;
  public void setValue(String v) { m_value = v; }
  public String getValue() { return m_value; }

}
/* JavaCC - OriginalChecksum=f3df3198bf68a3600f4b1e7a6a7eb453 (do not edit this line) */
