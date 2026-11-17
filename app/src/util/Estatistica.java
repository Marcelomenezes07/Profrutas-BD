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

    /**
     * Estatistica descritiva de uma amostra.
     * Variancia e desvio padrao AMOSTRAIS (divisor n - 1).
     * moda: valor(es) mais frequente(s); quando todos os valores aparecem uma unica vez
     * (comum em variaveis continuas, como valores em R$), informa a CLASSE MODAL de uma
     * distribuicao de frequencias com k = 1 + 3,3 log10(n) classes (regra de Sturges).
     */
    public record Descritiva(int n, double media, double mediana, String moda, double variancia,
                             double desvioPadrao, double coefVariacao, double minimo, double maximo) {}

    public static Descritiva descritiva(List<Double> valores, java.util.function.DoubleFunction<String> formato) {
        int n = valores.size();
        if (n == 0) return new Descritiva(0, Double.NaN, Double.NaN, "-", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        double[] v = valores.stream().mapToDouble(Double::doubleValue).sorted().toArray();

        double soma = 0;
        for (double x : v) soma += x;
        double media = soma / n;

        double mediana = n % 2 == 1 ? v[n / 2] : (v[n / 2 - 1] + v[n / 2]) / 2;

        double sq = 0;
        for (double x : v) sq += (x - media) * (x - media);
        double variancia = n > 1 ? sq / (n - 1) : 0;
        double desvio = Math.sqrt(variancia);
        double cv = media == 0 ? Double.NaN : 100 * desvio / media;

        return new Descritiva(n, media, mediana, moda(v, formato), variancia, desvio, cv, v[0], v[n - 1]);
    }

    private static String moda(double[] ordenados, java.util.function.DoubleFunction<String> formato) {
        java.util.Map<Double, Integer> freq = new java.util.TreeMap<>();
        for (double x : ordenados) freq.merge(Math.round(x * 1000) / 1000.0, 1, Integer::sum);
        int max = java.util.Collections.max(freq.values());
        if (max > 1) {
            List<String> modas = new java.util.ArrayList<>();
            freq.forEach((valor, f) -> { if (f == max) modas.add(formato.apply(valor)); });
            return String.join(" e ", modas) + " (" + max + "x)";
        }
        // amodal: usa a classe modal (Sturges)
        int n = ordenados.length;
        double min = ordenados[0], amplitude = ordenados[n - 1] - min;
        if (amplitude == 0) return formato.apply(min);
        int k = (int) Math.ceil(1 + 3.3 * Math.log10(n));
        double h = amplitude / k;
        int[] contagem = new int[k];
        for (double x : ordenados) contagem[Math.min(k - 1, (int) ((x - min) / h))]++;
        int melhor = 0;
        for (int i = 1; i < k; i++) if (contagem[i] > contagem[melhor]) melhor = i;
        return "classe " + formato.apply(min + melhor * h) + " a " + formato.apply(min + (melhor + 1) * h)
             + " (" + contagem[melhor] + " de " + n + ")";
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
