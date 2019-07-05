package worker;

import com.github.javaparser.ast.Node;

public class Printer {
    private Printer() {
        throw new UnsupportedOperationException();
    }

    public static void print(Node node) {
        try {
            //this.os = new FileOutputStream(new File("/tmp/out"));
            print(node, 1);
            //this.os.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void print(Node node, int deep) throws Exception {
        print(buildPrefix(deep) + " "
                + node.toString() + " :: "
                + node.getClass().toString() + "<<<<\n");
        int cdeep = deep + 1;
        for (Node child: node.getChildNodes()) {
            print(child, cdeep);
        }
    }

    private static void print(String content) throws Exception {
        //this.os.write(content.getBytes());
        System.out.print(content);
    }

    private static String buildPrefix(int len) {
        StringBuilder sb = new StringBuilder(len * 4);
        for (int i = 0; i < len; i++) {
            sb.append("   >");
        }
        return sb.toString();
    }
}
