package view.chart;

import view.UI;

import java.awt.*;
import java.util.Map;

/** Grafico de colunas verticais com linha de tendencia ligando os topos (serie temporal). */
public class GraficoColunas extends Grafico {
    private final Color cor;

    public GraficoColunas(String titulo, Color cor) {
        super(titulo);
        this.cor = cor;
    }

    @Override
    protected void desenhar(Graphics2D g, Rectangle area) {
        g.setFont(UI.PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        int baseY = area.y + area.height - fm.getHeight() - 4;
        int topoY = area.y + fm.getHeight() + 6;
        int alturaUtil = baseY - topoY;
        double max = maximo();
        int n = dados.size();
        double larguraSlot = (double) area.width / n;
        int larguraColuna = (int) Math.min(60, larguraSlot * 0.55);

        // linhas de grade
        g.setColor(UI.BORDA);
        for (int k = 0; k <= 4; k++) {
            int y = baseY - alturaUtil * k / 4;
            g.drawLine(area.x, y, area.x + area.width, y);
        }

        int[] xs = new int[n];
        int[] ys = new int[n];
        int i = 0;
        for (Map.Entry<String, Double> e : dados.entrySet()) {
            int cx = (int) (area.x + larguraSlot * i + larguraSlot / 2);
            int h = (int) (alturaUtil * (e.getValue() / max));
            g.setColor(cor);
            g.fillRoundRect(cx - larguraColuna / 2, baseY - h, larguraColuna, h, 6, 6);
            xs[i] = cx;
            ys[i] = baseY - h;

            g.setColor(UI.TEXTO);
            String rotulo = abreviar(g, e.getKey(), (int) larguraSlot - 4);
            g.drawString(rotulo, cx - fm.stringWidth(rotulo) / 2, baseY + fm.getAscent() + 3);
            g.setColor(UI.TEXTO_SUAVE);
            String valor = formato.apply(e.getValue());
            g.drawString(valor, cx - fm.stringWidth(valor) / 2, baseY - h - 6);
            i++;
        }

        if (n > 1) {
            g.setColor(UI.PALETA[1]);
            g.setStroke(new BasicStroke(2.2f));
            g.drawPolyline(xs, ys, n);
            for (int k = 0; k < n; k++) g.fillOval(xs[k] - 4, ys[k] - 4, 8, 8);
        }
    }
}
