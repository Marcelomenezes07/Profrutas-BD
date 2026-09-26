package view.chart;

import view.UI;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleFunction;

/** Base dos graficos desenhados com Java2D (sem bibliotecas externas). */
public abstract class Grafico extends JPanel {
    protected final String titulo;
    protected Map<String, Double> dados = new LinkedHashMap<>();
    protected DoubleFunction<String> formato = UI::numero;

    protected Grafico(String titulo) {
        this.titulo = titulo;
        setBackground(UI.CARTAO);
        setBorder(BorderFactory.createLineBorder(UI.BORDA));
        setPreferredSize(new Dimension(420, 280));
    }

    public void setDados(Map<String, Double> dados) {
        this.dados = dados;
        repaint();
    }

    public void setFormato(DoubleFunction<String> formato) {
        this.formato = formato;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(UI.SUBTITULO);
        g.setColor(UI.TEXTO);
        g.drawString(titulo, 14, 24);
        Rectangle area = new Rectangle(14, 38, getWidth() - 28, getHeight() - 50);
        if (dados.isEmpty()) {
            g.setFont(UI.NORMAL);
            g.setColor(UI.TEXTO_SUAVE);
            g.drawString("Sem dados", area.x + area.width / 2 - 30, area.y + area.height / 2);
        } else {
            desenhar(g, area);
        }
        g.dispose();
    }

    protected abstract void desenhar(Graphics2D g, Rectangle area);

    protected static String abreviar(Graphics2D g, String texto, int largura) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(texto) <= largura) return texto;
        while (texto.length() > 1 && fm.stringWidth(texto + "...") > largura) {
            texto = texto.substring(0, texto.length() - 1);
        }
        return texto + "...";
    }

    protected double maximo() {
        return dados.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
    }
}
