package analyzer.tests;

import analyzer.ast.ParserVisitor;
import analyzer.visitors.EstheRustConvertorVisitor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.*;

@RunWith(Parameterized.class)
public class ConvertorTest extends BaseTest {

    private static String m_test_suite_path = "./test-suite/EstheRustConvertor/data";

    public ConvertorTest(File file) {
        super(file);
    }

    @Test
    public void run() throws Exception {
        ParserVisitor algorithm = new EstheRustConvertorVisitor(m_output);
        runAndAssert(algorithm);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getFiles() {
        return getFiles(m_test_suite_path);
    }

}
