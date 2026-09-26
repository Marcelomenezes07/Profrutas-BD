package view.chart;

import view.UI;

import java.awt.*;
import java.util.Map;

/** Grafico de barras horizontais (bom para rotulos longos). */
public class GraficoBarras extends Grafico {
    private final Color cor;

    public GraficoBarras(String titulo, Color cor) {
        super(titulo);
        this.cor = cor;
    }

    @Override
    protected void desenhar(Graphics2D g, Rectangle area) {
        g.setFont(UI.PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        int larguraRotulo = Math.min(150, area.width / 3);
        int larguraValor = 70;
        int n = dados.size();
        double alturaLinha = (double) area.height / n;
        int alturaBarra = (int) Math.max(6, Math.min(22, alturaLinha * 0.65));
        double max = maximo();
        int xBarra = area.x + larguraRotulo + 8;
        int larguraMax = area.width - larguraRotulo - larguraValor - 12;

        int i = 0;
        for (Map.Entry<String, Double> e : dados.entrySet()) {
            int yCentro = (int) (area.y + alturaLinha * i + alturaLinha / 2);
            g.setColor(UI.TEXTO);
            String rotulo = abreviar(g, e.getKey(), larguraRotulo);
            g.drawString(rotulo, area.x + larguraRotulo - fm.stringWidth(rotulo), yCentro + fm.getAscent() / 2 - 1);

            int w = (int) Math.max(2, larguraMax * (e.getValue() / max));
            g.setColor(UI.VERDE_CLARO);
            g.fillRoundRect(xBarra, yCentro - alturaBarra / 2, larguraMax, alturaBarra, 6, 6);
            g.setColor(cor);
            g.fillRoundRect(xBarra, yCentro - alturaBarra / 2, w, alturaBarra, 6, 6);

            g.setColor(UI.TEXTO_SUAVE);
            g.drawString(formato.apply(e.getValue()), xBarra + larguraMax + 6, yCentro + fm.getAscent() / 2 - 1);
            i++;
        }
    }
}
