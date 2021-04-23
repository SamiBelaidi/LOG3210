package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;

/**
 * Created: 28-02-21
 * Author: Esther Guerrier
 * <p>
 * Description: Ce visiteur explorer l'AST de EstheRust et génère son équivalent en Python
 */

public class EstheRustConvertorVisitor implements ParserVisitor {

    private final PrintWriter m_writer;

    private int indentationLevel = 0;
    private static final String INDENT = "\t";

    /*
        ceci n'est pas nécessaire à être utilisée pour imprimer le code. Utilisez-le si cela vous intéresse, sinon,
        vous pouvez imprimer au fur et à mesure avec m_writer. Pour ajouter une valeure (string, int, etc) dans le StringBuilder, utilisez
        la méthode append. Pour afficher avec m_writer, utiliser la méthode toString()
     */
    private StringBuilder code = new StringBuilder();


    public EstheRustConvertorVisitor(PrintWriter writer) {
        m_writer = writer;
    }


    /**
     * Permet de mettre la première lettre d'un string en CAPS. Exemple: allo -> Allo
     *
     * @param str: la string qu'on veut que sa première lettre soit en majuscule
     * @return la string avec la première lettre en majuscule
     */
    private String capitalize(String str) {
        if (str == null) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        if (node.jjtGetNumChildren() == 0 &&
                (node.jjtGetParent() instanceof ASTWhileStmt ||
                        node.jjtGetParent() instanceof ASTIfStmt)) {
            indentation();
            m_writer.println("pass");
            return null;
        }
        node.childrenAccept(this, data);
        return node.jjtGetNumChildren() == 0;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        if (node.jjtGetNumChildren() != 0)
            indentation();
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);

        for (int i = 0; i < node.getOps().size(); i++) {
            switch ((String) node.getOps().get(i)) {
                case "||":
                    m_writer.print(" or ");
                    break;
                case "&&":
                    m_writer.print(" and ");
                    break;
            }
            node.jjtGetChild(i + 1).jjtAccept(this, data);
        }
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);
        if (node.jjtGetNumChildren() > 1) {
            m_writer.print(" " + node.getValue() + " ");
            node.jjtGetChild(1).jjtAccept(this, data);
        }
        return null;
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);
        for (int i = 0; i < node.getOps().size(); i++) {
            m_writer.print(" " + node.getOps().get(i) + " ");
            node.jjtGetChild(i + 1).jjtAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);
        for (int i = 0; i < node.getOps().size(); i++) {
            m_writer.print(" " + node.getOps().get(i) + " ");
            node.jjtGetChild(i + 1).jjtAccept(this, data);
        }
        return null;
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        for (int i = 0; i < node.getOps().size(); i++) {
            m_writer.print("-");
        }
        node.jjtGetChild(0).jjtAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
        for (int i = 0; i < node.getOps().size(); i++) {
            m_writer.print("not ");
        }
        node.jjtGetChild(0).jjtAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTParExpr node, Object data) {
        m_writer.print("(");
        node.childrenAccept(this, data);
        m_writer.print(")");
        return null;
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTimportStmt node, Object data) {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            m_writer.println("import " + ((ASTIdentifier) node.jjtGetChild(i)).getValue());
        }
        return null;
    }

    @Override
    public Object visit(ASTDeclarationStmt node, Object data) {
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        m_writer.print(varName + " = ");
        node.jjtGetChild(1).jjtAccept(this, data);
        m_writer.println();
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        m_writer.print("while ");
        node.jjtGetChild(0).jjtAccept(this, data);
        m_writer.println(":");
        this.indentationLevel++;
        node.jjtGetChild(1).jjtAccept(this, data);
        this.indentationLevel--;
        return null;
    }

    @Override
    public Object visit(ASTIfStmt node, Object data) {
        m_writer.print("if ");
        node.jjtGetChild(0).jjtAccept(this, data);
        m_writer.println(":");
        this.indentationLevel++;
        node.jjtGetChild(1).jjtAccept(this, data);
        this.indentationLevel--;
        if (node.jjtGetNumChildren() > 2) {
            m_writer.println("else:");
            this.indentationLevel++;
            node.jjtGetChild(2).jjtAccept(this, data);
            this.indentationLevel--;
        }

        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTFunctionDeclaration node, Object data) {
        m_writer.print("def ");
        m_writer.print(((ASTIdentifier) node.jjtGetChild(0)).getValue());
        node.jjtGetChild(1).jjtAccept(this, data);
        m_writer.println(":");
        this.indentationLevel++;
        boolean emptyBlock = (boolean) node.jjtGetChild(2).jjtAccept(this, data);
        if (emptyBlock && node.jjtGetNumChildren() == 3) {
            indentation();
            m_writer.println("pass");
        } else if (node.jjtGetNumChildren() > 3) {
            node.jjtGetChild(3).jjtAccept(this, data);
        }

        this.indentationLevel--;
        m_writer.print("\n\n");
        return null;
    }

    @Override
    public Object visit(ASTFunctionParams node, Object data) {
        m_writer.print("(");
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (i != 0)
                m_writer.print(", ");
            node.jjtGetChild(i).jjtAccept(this, data);
        }
        m_writer.print(")");
        return null;
    }

    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        node.childrenAccept(this, data);
        if (node.jjtGetParent() instanceof ASTStmt)
            m_writer.println();
        return null;
    }

    @Override
    public Object visit(ASTFunctionCallParams node, Object data) {
        m_writer.print("(");
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (i != 0)
                m_writer.print(", ");
            node.jjtGetChild(i).jjtAccept(this, data);
        }
        m_writer.print(")");
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {
        indentation();
        m_writer.print("return ");
        node.jjtGetChild(0).jjtAccept(this, data);
        m_writer.println();
        return null;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        m_writer.print(node.getValue());
        return null;
    }

    @Override
    public Object visit(ASTRealValue node, Object data) {
        m_writer.print(node.getValue());
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        m_writer.print(node.getValue());
        return null;
    }

    @Override
        public Object visit(ASTBoolValue node, Object data) {
            m_writer.print(capitalize(node.getValue()));
        return null;
    }

    @Override
    public Object visit(ASTStringValue node, Object data) {
        m_writer.print(node.getValue());
        return null;
    }

    @Override
    public Object visit(ASTcoeurValue node, Object data) {
        m_writer.println("print(\"Compilateurs est le meilleur cours au monde\")");
        return null;
    }

    private void indentation() {
        for (int i = 0; i < this.indentationLevel; i++) {
            m_writer.print(INDENT);
        }
    }
}
