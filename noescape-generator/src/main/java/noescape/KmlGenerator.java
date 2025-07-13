package noescape;

import noescape.model.BorderSign;

import java.util.Comparator;
import java.util.List;

import static java.util.Collections.sort;
import static java.util.Comparator.comparing;

public class KmlGenerator {

    public static String toKml(List<BorderSign> borderSigns) {
        borderSigns.sort(BorderSign::compareTo);
        borderSigns.sort(comparing(BorderSign::country));

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
            <Document>
                <name>signs</name>
                <StyleMap id="msn_grn-pushpin">
                    <Pair>
                        <key>normal</key>
                        <styleUrl>#sn_grn-pushpin</styleUrl>
                    </Pair>
                    <Pair>
                        <key>highlight</key>
                        <styleUrl>#sh_grn-pushpin</styleUrl>
                    </Pair>
                </StyleMap>
                <StyleMap id="msn_red-pushpin">
                    <Pair>
                        <key>normal</key>
                        <styleUrl>#sn_red-pushpin</styleUrl>
                    </Pair>
                    <Pair>
                        <key>highlight</key>
                        <styleUrl>#sh_red-pushpin</styleUrl>
                    </Pair>
                </StyleMap>
                <StyleMap id="msn_ylw-pushpin">
                    <Pair>
                        <key>normal</key>
                        <styleUrl>#sn_ylw-pushpin</styleUrl>
                    </Pair>
                    <Pair>
                        <key>highlight</key>
                        <styleUrl>#sh_ylw-pushpin</styleUrl>
                    </Pair>
                </StyleMap>
                <Style id="sh_grn-pushpin">
                    <IconStyle>
                        <scale>1.3</scale>
                        <Icon>
                            <href>http://maps.google.com/mapfiles/kml/pushpin/grn-pushpin.png</href>
                        </Icon>
                        <hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
                    </IconStyle>
                </Style>
                <Style id="sh_red-pushpin">
                    <IconStyle>
                        <scale>1.3</scale>
                        <Icon>
                            <href>http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png</href>
                        </Icon>
                        <hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
                    </IconStyle>
                </Style>
                <Style id="sh_ylw-pushpin">
                    <IconStyle>
                        <scale>1.3</scale>
                        <Icon>
                            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>
                        </Icon>
                        <hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
                    </IconStyle>
                </Style>
                <Style id="sn_grn-pushpin">
                    <IconStyle>
                        <scale>1.1</scale>
                        <Icon>
                            <href>http://maps.google.com/mapfiles/kml/pushpin/grn-pushpin.png</href>
                        </Icon>
                        <hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
                    </IconStyle>
                </Style>
                <Style id="sn_red-pushpin">
                    <IconStyle>
                        <scale>1.1</scale>
                        <Icon>
                            <href>http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png</href>
                        </Icon>
                        <hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
                    </IconStyle>
                </Style>
                <Style id="sn_ylw-pushpin">
                    <IconStyle>
                        <scale>1.1</scale>
                        <Icon>
                            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>
                        </Icon>
                        <hotSpot x="20" y="2" xunits="pixels" yunits="pixels"/>
                    </IconStyle>
                </Style>
            """);
        String prevCountry = null;
        for (var borderSign : borderSigns) {
            var pushPin = borderSign.generated() ? "msn_red-pushpin" : "msn_grn-pushpin";

            if (!borderSign.country().equals(prevCountry)) {
                if (prevCountry != null) {
                    sb.append("\n    </Folder>");
                }
                sb.append("\n    <Folder>");
                sb.append("\n        <name>").append(borderSign.country()).append("</name>");
                sb.append("\n        <open>0</open>");
                sb.append("\n        <visibility>0</visibility>");
                prevCountry = borderSign.country();
            }

            sb.append("\n        <Placemark><name>")
                    .append(borderSign.title())
                    .append("</name><styleUrl>#")
                    .append(pushPin)
                    .append("</styleUrl><Point><coordinates>")
                    .append(borderSign.position().lng()).append(",").append(borderSign.position().lat())
                    .append("</coordinates></Point></Placemark>");
        }
        if (prevCountry != null) {
            sb.append("\n    </Folder>\n");
        }
        sb.append("""
                </Document>
                </kml>
                """);
        return sb.toString();
    }
}
