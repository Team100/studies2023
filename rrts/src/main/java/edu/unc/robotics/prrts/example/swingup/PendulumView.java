package edu.unc.robotics.prrts.example.swingup;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.unc.robotics.prrts.Runner;
import edu.unc.robotics.prrts.Path;
import edu.unc.robotics.prrts.example.geom.Obstacle;
import edu.unc.robotics.prrts.tree.Node;

public class PendulumView extends JComponent {
    private final Runner _rrtStar;
    private final PendulumArena _robotModel;
    private int framecounter;


    private Image _backgroundImage;
    //private Path _bestPath = null;

    private final NumberFormat _integerFormat = DecimalFormat.getIntegerInstance();

    public PendulumView(PendulumArena arena, Runner rrtStar) {
        _rrtStar = rrtStar;
        _robotModel = arena;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        doPaint((Graphics2D) graphics, getSize());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
    }

    public void doPaint(Graphics2D g, Dimension size) {
        // System.out.println("paint");

        PendulumArena robotModel = _robotModel;
        double[] min = robotModel.getMin();
        double[] max = robotModel.getMax();

        Path bestPath = _rrtStar.getBestPath();

        // if (_backgroundImage == null ||
        // _backgroundImage.getWidth(null) != size.width ||
        // _backgroundImage.getHeight(null) != size.height ){//||
        // // Path.isBetter(bestPath, _bestPath)) {
        framecounter += 1;
        if (framecounter > 100) {
            framecounter = 0;
            createBGImage(min, max, size, bestPath);
        }
        // _bestPath = bestPath;
        // }

        g.drawImage(_backgroundImage, 0, 0, null);

        AffineTransform savedTransform = g.getTransform();

        g.setTransform(savedTransform);

        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String count = _integerFormat.format(_rrtStar.getStepNo());
        g.drawString(count, 4, 4 + fm.getAscent());
        g.setColor(Color.BLACK);
        g.drawString(count, 3, 3 + fm.getAscent());
    }

    private void createBGImage(double[] min, double[] max, Dimension size, Path link) {
        // System.out.println("BG");
        _backgroundImage = createImage(size.width, size.height);

        Graphics2D g = (Graphics2D) _backgroundImage.getGraphics();
        AffineTransform savedTransform = g.getTransform();

        AffineTransform transform = AffineTransform.getTranslateInstance(size.width / 2, size.height / 2);
        transform.scale(1, -1);
        g.setTransform(transform);

        setupGraphics(min, max, size, g);

        g.setColor(Color.WHITE);
        g.fillRect(-size.width / 2, -size.height / 2, size.width, size.height);

        // obstacles
        g.setStroke(new BasicStroke(0f));
        // g.setColor(new Color(0x8888ff));
        for (Obstacle obstacle : _robotModel.obstacles()) {
            g.setColor(obstacle.color());
            g.fill(obstacle.shape());
        }

        renderRRTTree(g);

        renderPaths(link, g);

        g.setTransform(savedTransform);
        g.dispose();
    }

    private void renderRRTTree(Graphics2D g) {
        Line2D.Double line = new Line2D.Double();
        for (Node node : _rrtStar.getNodes()) {
            Node parent = node.get_incoming().get_source();
            if (parent != null) {
                double[] n = node.get_config();
                double[] p = parent.get_config();
                g.setColor(Color.GRAY);
                // System.out.println("x " + n[0] + " y " + n[1]);
                line.setLine(n[0], n[1], p[0], p[1]);
                g.draw(line);
            }
        }
    }

    private void renderPaths(Path path, Graphics2D g) {
        if (path == null) {
            return;
        }

        Line2D.Double line = new Line2D.Double();
        g.setStroke(new BasicStroke((float) 0.1));

        if (path.get_configs().size() > 1) {
            Iterator<double[]> pathIter = path.get_configs().iterator();
            double[] prev = pathIter.next();
            while (pathIter.hasNext()) {
                double[] curr = pathIter.next();
          
                    g.setColor(brighter(Color.RED));
                    line.setLine(prev[0], prev[1], curr[0], curr[1]);
                    g.draw(line);
                
                prev = curr;
            }
        }
    }

    private void setupGraphics(double[] min, double[] max, Dimension size, Graphics2D g) {
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g.translate(min[0], min[1]);
        // double scale = Math.min(
        // size.width / (max[0] - min[0]),
        // size.height / (max[1] - min[1]));
        double xscale = size.width / (max[0] - min[0]);
        double yscale = size.height / (max[1] - min[1]);
        g.scale(xscale, yscale);
        g.setStroke(new BasicStroke((float) 0.01));
    }

    static Color brighter(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[1] = Math.max(0.0f, hsb[1] - 0.25f);
        hsb[2] = Math.min(1.0f, hsb[2] + 0.25f);
        color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        return color;
    }

}
