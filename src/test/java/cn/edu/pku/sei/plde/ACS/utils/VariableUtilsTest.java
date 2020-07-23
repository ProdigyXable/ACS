package cn.edu.pku.sei.plde.ACS.utils;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Created by yjxxtd on 4/23/16.
 */
public class VariableUtilsTest {

    @Test
    public void testIsJavaIdentifier() {
        assertFalse(VariableUtils.isJavaIdentifier(".a"));
        assertFalse(VariableUtils.isJavaIdentifier("a*b"));
        assertFalse(VariableUtils.isJavaIdentifier("a.c"));
        assertTrue(VariableUtils.isJavaIdentifier("a2"));
    }
}
