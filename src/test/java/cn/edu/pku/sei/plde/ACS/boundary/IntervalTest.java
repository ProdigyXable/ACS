package cn.edu.pku.sei.plde.ACS.boundary;

import cn.edu.pku.sei.plde.ACS.boundary.model.Interval;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

/**
 * Created by yanrunfa on 8/9/16.
 */
public class IntervalTest {

    @Test
    public void testSimplify() {
        Interval one = new Interval(1, 2, false, false);
        Interval two = new Interval(2, 3, false, false);
        Interval three = new Interval(1, 2, true, true);
        Interval four = new Interval(2, 3, true, true);
        Interval five = new Interval(1, 3, true, true);
        ArrayList<Interval> list = new ArrayList<Interval>(Arrays.asList(one, two, three, four, five));
        Interval.simplify(list);
        System.out.println(list);
    }
}
