package noescape.service;

import noescape.model.BorderSign;
import noescape.model.Position;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BorderSignKmlParser extends DefaultHandler {
    private static final String RED_PUSHPIN = "http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png";
    private static final String YELLOW_PUSHPIN = "http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png";

    private final List<BorderSign> borderSignList = new ArrayList<>();
    private final Map<String, String> styleMaps = new HashMap<>();
    private final Map<String, String> styleIcons = new HashMap<>();
    private String styleMapId;
    private String styleId;
    private StringBuilder text;
    private boolean folder;
    private boolean placemark;
    private String country;
    private String title;
    private double lat;
    private double lng;
    private boolean generated;

    public static List<BorderSign> parse(File file) throws ParserConfigurationException, SAXException, IOException {
        var saxParser = SAXParserFactory.newInstance().newSAXParser();
        var kmlParser = new BorderSignKmlParser();
        saxParser.parse(file, kmlParser);
        return kmlParser.borderSignList;
    }

    public static List<BorderSign> parse(byte[] content) throws ParserConfigurationException, SAXException, IOException {
        var saxParser = SAXParserFactory.newInstance().newSAXParser();
        var kmlParser = new BorderSignKmlParser();
        saxParser.parse(new ByteArrayInputStream(content), kmlParser);
        return kmlParser.borderSignList;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        text.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        text = new StringBuilder();

        if (qName.equals("StyleMap")) {
            styleMapId = attributes.getValue("id");
        }
        if (qName.equals("Style")) {
            styleId = attributes.getValue("id");
        }
        if (qName.equals("Folder")) {
            folder = true;
        }
        if (qName.equals("Placemark")) {
            folder = false;
            placemark = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("StyleMap")) {
            styleMapId = null;
        }
        if (qName.equals("Style")) {
            styleId = null;
        }
        if (qName.equals("styleUrl") && styleMapId != null) {
            styleMaps.put(styleMapId, text.toString());
        }
        if (qName.equals("href")) {
            String value = text.toString();
            if (value.equals(RED_PUSHPIN) || value.equals(YELLOW_PUSHPIN)) {
                styleIcons.put(styleId, value);
            }
        }
        if (qName.equals("name") && folder) {
            country = text.toString();
            folder = false;
        }
        if (placemark) {
            if (qName.equals("name")) {
                title = text.toString();
            }
            if (qName.equals("coordinates")) {
                String[] coord = text.toString().split(",");
                lng = Double.parseDouble(coord[0]);
                lat = Double.parseDouble(coord[1]);
            }
            if (qName.equals("styleUrl")) {
                String styleUrl = text.toString();
                if (styleUrl.startsWith("#")) {
                    String iconId = styleMaps.get(styleUrl.substring(1)).substring(1);
                    String icon = styleIcons.get(iconId);
                    generated = RED_PUSHPIN.equals(icon);
                }
            }
            if (qName.equals("Placemark") && BorderSign.Key.COUNTRIES.contains(country)) {
                borderSignList.add(new BorderSign(country, title, new Position(lat, lng), generated));
                placemark = false;
            }
        }
    }
}
