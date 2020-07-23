package cn.edu.pku.sei.plde.ACS.equalVariable;

import cn.edu.pku.sei.plde.ACS.utils.CodeUtils;
import java.util.Set;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Created by yjxxtd on 4/23/16.
 */
public class EqualVariableCollectTest {

    @Test
    public void testEqualVariableCollect() {
        String path = "filesfortest//Complex.java";
        Set<String> equalVariable = CodeUtils.getEqualVariableInSource(path);

        assertTrue(equalVariable != null);
        assertTrue(equalVariable.size() == 2);
        assertTrue(equalVariable.contains("real"));
        assertTrue(equalVariable.contains("imaginary"));
    }

}
