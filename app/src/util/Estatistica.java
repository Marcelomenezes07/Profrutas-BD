package util;

import java.util.List;

/** Calculos estatisticos usados no grafico de dispersao. */
public final class Estatistica {
    private Estatistica() {}

    /**
     * Resultado da analise de correlacao entre X e Y.
     * Reta de regressao (minimos quadrados): y = a + b*x
     */
    public record Correlacao(int n, double pearson, double r2, double a, double b,
                             double mediaX, double mediaY) {}

    /**
     * Coeficiente de correlacao de Pearson:
     *   r = sum((x - mx)(y - my)) / sqrt( sum((x - mx)^2) * sum((y - my)^2) )
     * Reta de regressao linear simples:
     *   b = sum((x - mx)(y - my)) / sum((x - mx)^2)      a = my - b*mx
     */
    public static Correlacao correlacao(List<double[]> pontos) {
        int n = pontos.size();
        if (n < 2) return new Correlacao(n, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        double mx = 0, my = 0;
        for (double[] p : pontos) { mx += p[0]; my += p[1]; }
        mx /= n;
        my /= n;
        double sxy = 0, sxx = 0, syy = 0;
        for (double[] p : pontos) {
            double dx = p[0] - mx, dy = p[1] - my;
            sxy += dx * dy;
            sxx += dx * dx;
            syy += dy * dy;
        }
        double r = (sxx == 0 || syy == 0) ? Double.NaN : sxy / Math.sqrt(sxx * syy);
        double b = sxx == 0 ? Double.NaN : sxy / sxx;
        double a = my - b * mx;
        return new Correlacao(n, r, r * r, a, b, mx, my);
    }

    /** Classificacao usual da forca da correlacao pelo valor absoluto de r. */
    public static String interpretar(double r) {
        if (Double.isNaN(r)) return "indefinida";
        double v = Math.abs(r);
        String forca = v >= 0.9 ? "muito forte"
                     : v >= 0.7 ? "forte"
                     : v >= 0.5 ? "moderada"
                     : v >= 0.3 ? "fraca"
                     : "desprezivel";
        if (v < 0.3) return "correlacao " + forca;
        return "correlacao " + forca + (r > 0 ? " positiva" : " negativa");
    }
}
