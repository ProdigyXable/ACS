package cn.edu.pku.sei.plde.ACS.localization.metric;

/**
 * Created by spirals on 24/07/15.
 */
public class SorensenDice implements Metric {

    public double value(int ef, int ep, int nf, int np) {
        // ef / float(ef + ep + nf)
        return 2 * ef / ((double) (2 * ef + (ep + nf)));
    }
}
