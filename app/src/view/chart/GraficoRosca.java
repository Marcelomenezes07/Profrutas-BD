package view.chart;

import view.UI;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.Map;

/** Grafico de rosca (pizza com furo) com legenda e percentuais. */
public class GraficoRosca extends Grafico {

    public GraficoRosca(String titulo) {
        super(titulo);
    }

    @Override
    protected void desenhar(Graphics2D g, Rectangle area) {
        double total = dados.values().stream().mapToDouble(Double::doubleValue).sum();
        int diametro = Math.min(area.height - 8, area.width / 2 - 10);
        int cx = area.x + 6;
        int cy = area.y + (area.height - diametro) / 2;

        double inicio = 90;
        int i = 0;
        for (double v : dados.values()) {
            double ang = -360.0 * v / total;
            g.setColor(UI.PALETA[i % UI.PALETA.length]);
            g.fill(new Arc2D.Double(cx, cy, diametro, diametro, inicio, ang, Arc2D.PIE));
            inicio += ang;
            i++;
        }
        int furo = (int) (diametro * 0.55);
        g.setColor(UI.CARTAO);
        g.fillOval(cx + (diametro - furo) / 2, cy + (diametro - furo) / 2, furo, furo);

        g.setFont(UI.SUBTITULO);
        g.setColor(UI.TEXTO);
        String centro = formato.apply(total);
        FontMetrics fmC = g.getFontMetrics();
        g.drawString(centro, cx + diametro / 2 - fmC.stringWidth(centro) / 2, cy + diametro / 2 + fmC.getAscent() / 2 - 2);

        g.setFont(UI.PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        int xLeg = cx + diametro + 18;
        int larguraLeg = area.x + area.width - xLeg;
        int alturaItem = Math.min(22, Math.max(14, area.height / Math.max(1, dados.size())));
        int yLeg = area.y + (area.height - alturaItem * dados.size()) / 2;
        i = 0;
        for (Map.Entry<String, Double> e : dados.entrySet()) {
            int y = yLeg + i * alturaItem;
            g.setColor(UI.PALETA[i % UI.PALETA.length]);
            g.fillRoundRect(xLeg, y, 11, 11, 3, 3);
            g.setColor(UI.TEXTO);
            String pct = String.format("%.1f%%", 100 * e.getValue() / total);
            String txt = abreviar(g, e.getKey(), larguraLeg - 22 - fm.stringWidth(pct) - 8);
            g.drawString(txt, xLeg + 17, y + 10);
            g.setColor(UI.TEXTO_SUAVE);
            g.drawString(pct, xLeg + larguraLeg - fm.stringWidth(pct), y + 10);
            i++;
        }
    }
}
