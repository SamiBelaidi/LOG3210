/* Generated By:JJTree: Do not edit this line. ASTAssignStmt.java */
package analyzer.ast;

public class ASTAssignStmt extends SimpleNode {
  public ASTAssignStmt(int id) {
    super(id);
  }

  public ASTAssignStmt(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(ParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  //PLB
  private String op = null;
  public void setOp(String o) { op = o; }
  public String getOp() { return op; }
}
