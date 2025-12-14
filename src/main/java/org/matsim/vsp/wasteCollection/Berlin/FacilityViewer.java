package org.matsim.vsp.wasteCollection.Berlin;

import org.w3c.dom.*;

import javax.imageio.ImageIO;
import javax.xml.parsers.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public class FacilityViewer extends JPanel {

    //Data structure for the facilities
    private static class Facility {
        double x, y;
        String carrier;
        boolean isSeed;
        boolean isDepot;
        boolean isDropoff;
        Facility(double x, double y, String carrier, boolean isSeed, boolean isDepot, boolean isDropoff) {
            this.x = x;
            this.y = y;
            this.carrier = carrier;
            this.isSeed = isSeed;
            this.isDepot = isDepot;
            this.isDropoff = isDropoff;
        }
    }

    private final List<Facility> facilities;
    private final Map<String, Color> carrierColors = new TreeMap<>();


    public FacilityViewer(List<Facility> facilities) {
        this.facilities = facilities;
        // Assign random colors per carrier
        Random rand = new Random();
        for (Facility facility : facilities) {
            carrierColors.putIfAbsent(facility.carrier, new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
        }
    }


    //This actually draws all the points
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
        int margin = 50;

        //Draw normal facilities first
        for (Facility facility : facilities) {
            if (facility.isSeed) continue; // skip seed for now
            double normX = (facility.x - minX) / (maxX - minX);
            double normY = (facility.y - minY) / (maxY - minY);
            int drawX = (int) (margin + normX * (width -  2*margin));
            int drawY = (int) (height - margin - normY * (height - 2 * margin));
            if (facility.isDepot) {
                continue;
            }
            if (facility.isDropoff) {
                continue;
            }

            g2.setColor(carrierColors.get(facility.carrier));
            g2.fillOval(drawX - 5, drawY - 5, 10, 10);
        }

        //Draw seed facilities last (on top)
        for (Facility facility : facilities) {
            if (!facility.isSeed) continue;
            double normX = (facility.x - minX) / (maxX - minX);
            double normY = (facility.y - minY) / (maxY - minY);
            int drawX = (int) (margin + normX * (width -  2*margin));
            int drawY = (int) (height - margin - normY * (height - 2 * margin));

            g2.setColor(carrierColors.get(facility.carrier));
            g2.fillOval(drawX - 5, drawY - 5, 10, 10);

            // red highlight
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(4f));
            g2.drawOval(drawX - 7, drawY - 7, 14, 14);
        }

        // --- Optional legend ---
//        int y = 20;
//        for (Map.Entry<String, Color> entry : carrierColors.entrySet()) {
//            g2.setColor(entry.getValue());
//            g2.fillRect(10, y, 10, 10);
//            g2.setColor(Color.BLACK);
//            g2.drawString(entry.getKey(), 25, y + 10);
//            y += 12;
//        }
//        g2.setColor(Color.RED);
//        g2.drawString("Seeds", 25, y + 10);
//        g2.fillOval(10, y, 10, 10);
    }

    //Read in the facilities
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
            boolean isSeed = false;
            boolean isDepot = false;
            boolean isDropoff = false;

            NodeList attributes = fEl.getElementsByTagName("attribute");
            for (int j = 0; j < attributes.getLength(); j++) {
                Element attr = (Element) attributes.item(j);
                String name = attr.getAttribute("name");
                if ("carrier".equals(name)) {
                    carrier = attr.getTextContent().trim();
                } else if ("seed".equals(name)) {
                    isSeed = true;
                } else if ("depot".equals(name)) {
                    isDepot = true;
                } else if ("dropOff".equals(name)) {
                    isDropoff = true;
                }
            }
            list.add(new Facility(x, y, carrier, isSeed, isDepot, isDropoff));
        }
        return list;
    }

    //This can be called to from the showViewer to save the images
    public static void savePanelAsImage(JPanel panel, String filePath) {
        int w = panel.getWidth();
        int h = panel.getHeight();

        if (w <= 0 || h <= 0) {
            System.err.println("Panel has zero size, cannot save image.");
            return;
        }

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        try {
            ImageIO.write(image, "png", new File(filePath));
            System.out.println("Saved image to: " + filePath);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void showViewer(String xmlPath) {
        try {
            List<Facility> facilities = parseFacilities(new File(xmlPath));
            FacilityViewer viewer = new FacilityViewer(facilities);

            //Setup for JFrame
            JFrame frame = new JFrame("Facility Viewer - " + xmlPath);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(viewer);
            frame.setSize(1920, 1080);
            frame.setVisible(true);


        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    // You can call this main() directly to test manually
    public static void main(String[] args) {
        showViewer("final_output/centroids/Mo/facilities.xml");
    }
}

