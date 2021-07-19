package saioapi.util;

public class ZipUtils {
        public static native int excuteCommand(String command);
        static {
                System.loadLibrary("p7zip");
        }
}
