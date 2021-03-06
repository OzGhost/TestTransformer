package storage;

import static meta.Name.*;
import worker.WoodLog;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class LineLoader {

    private static final VoidConsumer DEFAULT_LINE_FEED = new VoidConsumer();

    public static List<String> loadFile(String fn) {
        return loadFile(fn, DEFAULT_LINE_FEED);
    }

    public static List<String> loadFile(String fn, Consumer<String> lineFeed) {
        List<String> lines = new ArrayList<>();
        String workingDir = ClassLoader.getSystemResource(".").getPath();
        File dFile = locateFileByName(workingDir, fn);
        try (BufferedReader reader = new BufferedReader(new FileReader(dFile))) {
            String line = reader.readLine();
            while (line != null) {
                WoodLog.loopLog(LineLoader.class, 25);
                lineFeed.accept(line);
                lines.add(line);
                line = reader.readLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
            WoodLog.attach(WARNING, "Fail to load file " + dFile.getAbsolutePath());
        }
        return lines;
    }

    private static File locateFileByName(String wd, String filename) {
        char[] cs = wd.toCharArray();
        int len = cs.length;
        int i = len;
        int counter = 0;
        while (i >= 0 && counter < 5) {
            WoodLog.loopLog(LineLoader.class, 41);
            if (cs[--i] == '/') {
                counter++;
            }
        }
        char[] scn = filename.toCharArray();
        char[] o = new char[i+1+scn.length];
        int k = i + 1;
        while (i >= 0) {
            WoodLog.loopLog(LineLoader.class, 49);
            o[i] = cs[i];
            i--;
        }
        len = scn.length;
        for (i = 0; i < len; ++i) {
            o[k+i] = scn[i];
        }
        return new File(new String(o));
    }

    private static class VoidConsumer implements Consumer<String> {
        @Override
        public void accept(String treat) {}
    }
}

