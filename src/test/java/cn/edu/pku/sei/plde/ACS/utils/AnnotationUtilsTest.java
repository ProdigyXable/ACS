package cn.edu.pku.sei.plde.ACS.utils;

import java.util.List;
import org.junit.Test;

/**
 * Created by yanrunfa on 6/10/16.
 */
public class AnnotationUtilsTest {

    @Test
    public void testParser() {
        List<String> keywords = AnnotationUtils.Parse("the text with any replacements processed, <code>null</code> if null String input");
        System.out.println(keywords.toString());
    }

}
