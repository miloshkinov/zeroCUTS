package org.matsim.vsp.wasteCollection.Berlin;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class FacilityViewer extends JPanel {

    private static class Facility {
        double x, y;
        String carrier;
        boolean hasSeed;
        Facility(double x, double y, String carrier, boolean hasSeed) {
            this.x = x;
            this.y = y;
            this.carrier = carrier;
            this.hasSeed = hasSeed;
        }
    }

    private final List<Facility> facilities;
    private final Map<String, Color> carrierColors = new HashMap<>();

    public FacilityViewer(List<Facility> facilities) {
        this.facilities = facilities;
        // Assign random colors per carrier
        Random rand = new Random();
        for (Facility f : facilities) {
            carrierColors.putIfAbsent(f.carrier, new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (facilities.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double minX = facilities.stream().mapToDouble(f -> f.x).min().orElse(0);
        double maxX = facilities.stream().mapToDouble(f -> f.x).max().orElse(1);
        double minY = facilities.stream().mapToDouble(f -> f.y).min().orElse(0);
        double maxY = facilities.stream().mapToDouble(f -> f.y).max().orElse(1);

        int width = getWidth();
        int height = getHeight();
        int margin = 40;

        // --- Draw normal facilities first ---
        for (Facility f : facilities) {
            if (f.hasSeed) continue; // skip seed for now
            double normX = (f.x - minX) / (maxX - minX);
            double normY = (f.y - minY) / (maxY - minY);
            int drawX = (int) (margin + normX * (width - 2 * margin));
            int drawY = (int) (height - margin - normY * (height - 2 * margin));

            g2.setColor(carrierColors.get(f.carrier));
            g2.fillOval(drawX - 5, drawY - 5, 10, 10);
        }

        // --- Draw seed facilities last (on top) ---
        for (Facility f : facilities) {
            if (!f.hasSeed) continue;
            double normX = (f.x - minX) / (maxX - minX);
            double normY = (f.y - minY) / (maxY - minY);
            int drawX = (int) (margin + normX * (width - 2 * margin));
            int drawY = (int) (height - margin - normY * (height - 2 * margin));

            g2.setColor(carrierColors.get(f.carrier));
            g2.fillOval(drawX - 5, drawY - 5, 10, 10);

            // yellow highlight
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(4f));
            g2.drawOval(drawX - 7, drawY - 7, 14, 14);
        }

        // --- Optional legend ---
        int y = 20;
        for (Map.Entry<String, Color> entry : carrierColors.entrySet()) {
            g2.setColor(entry.getValue());
            g2.fillRect(10, y, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString(entry.getKey(), 25, y + 10);
            y += 15;
        }
        g2.setColor(Color.RED);
        g2.drawString("= has seed", 25, y + 10);
        g2.fillOval(10, y, 10, 10);
    }

    public static List<Facility> parseFacilities(File xmlFile) throws Exception {
        List<Facility> list = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xmlFile);
        NodeList facilityNodes = doc.getElementsByTagName("facility");

        for (int i = 0; i < facilityNodes.getLength(); i++) {
            Element fEl = (Element) facilityNodes.item(i);
            double x = Double.parseDouble(fEl.getAttribute("x"));
            double y = Double.parseDouble(fEl.getAttribute("y"));

            String carrier = "unknown";
            boolean hasSeed = false;

            NodeList attrs = fEl.getElementsByTagName("attribute");
            for (int j = 0; j < attrs.getLength(); j++) {
                Element attr = (Element) attrs.item(j);
                String name = attr.getAttribute("name");
                if ("carrier".equals(name)) {
                    carrier = attr.getTextContent().trim();
                } else if ("seed".equals(name)) {
                    hasSeed = true;
                }
            }
            list.add(new Facility(x, y, carrier, hasSeed));
        }
        return list;
    }

    public static void showViewer(String xmlPath) {
        try {
            List<Facility> facilities = parseFacilities(new File(xmlPath));
            JFrame frame = new JFrame("Facility Viewer - " + xmlPath);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new FacilityViewer(facilities));
            frame.setSize(800, 600);
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // You can call this main() directly to test manually
    public static void main(String[] args) {
        showViewer("input/tune_seeding/minSpacingv4.xml");
    }
}

