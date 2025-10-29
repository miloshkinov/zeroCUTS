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
        Facility(double x, double y, String carrier) {
            this.x = x;
            this.y = y;
            this.carrier = carrier;
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
        double minX = facilities.stream().mapToDouble(f -> f.x).min().orElse(0);
        double maxX = facilities.stream().mapToDouble(f -> f.x).max().orElse(1);
        double minY = facilities.stream().mapToDouble(f -> f.y).min().orElse(0);
        double maxY = facilities.stream().mapToDouble(f -> f.y).max().orElse(1);

        int width = getWidth();
        int height = getHeight();
        int margin = 40;

        for (Facility f : facilities) {
            double normX = (f.x - minX) / (maxX - minX);
            double normY = (f.y - minY) / (maxY - minY);
            int drawX = (int) (margin + normX * (width - 2 * margin));
            int drawY = (int) (height - margin - normY * (height - 2 * margin)); // flip Y for screen coords

            g2.setColor(carrierColors.get(f.carrier));
            g2.fillOval(drawX - 5, drawY - 5, 10, 10);
        }

        // Optional legend
        int y = 5;
        for (Map.Entry<String, Color> entry : carrierColors.entrySet()) {
            g2.setColor(entry.getValue());
            g2.fillRect(10, y, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString(entry.getKey(), 25, y + 9);
            y += 10;
        }
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
            NodeList attrs = fEl.getElementsByTagName("attribute");
            for (int j = 0; j < attrs.getLength(); j++) {
                Element attr = (Element) attrs.item(j);
                if ("carrier".equals(attr.getAttribute("name"))) {
                    carrier = attr.getTextContent().trim();
                    break;
                }
            }
            list.add(new Facility(x, y, carrier));
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
        showViewer("input/test_centroidClusters/Mo.xml");
    }
}

