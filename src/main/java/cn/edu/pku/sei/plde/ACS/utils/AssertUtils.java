package cn.edu.pku.sei.plde.ACS.utils;

/**
 * Created by yanrunfa on 16/3/22.
 */
public class AssertUtils {

    public static boolean isAssertLine(String lineString, String code) {
        lineString = lineString.trim();
        //注释
        if (lineString.startsWith("//")) {
            return false;
        }
        if (lineString.startsWith("Assert") || lineString.startsWith("assert") || lineString.contains(".assert") || lineString.startsWith("fail")) {
            return true;
        } else if (lineString.contains("(") && lineString.contains(")") && !lineString.contains("=")) {
            String callMethod = lineString.substring(0, lineString.indexOf("(")).trim();
            if (code.contains("void " + callMethod + "(")) {
                return true;
            }
        }
        return false;
    }
}
