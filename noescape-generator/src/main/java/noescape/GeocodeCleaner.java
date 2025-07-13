package noescape;

import noescape.model.Locality;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GeocodeCleaner {

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, IllegalStateException {
        GoogleClient googleClient = new GoogleClient();
        for (var file : new File("geocode").listFiles()) {
            try {
                String json = Files.readString(file.toPath());
                var jsonLines = json.lines().toList();

                var geocodeResults = googleClient.parseJson(json);
                var localities = geocodeResults.stream().map(GoogleClient.GeocodeResult::locality).toList();

                int[] starts = null;
                int[] ends = null;
                try {
                    starts = new int[localities.size()];
                    ends = new int[localities.size()];
                    int placeIndex = 0;
                    for (int i = 0; i < jsonLines.size(); i++) {
                        String line = jsonLines.get(i);
                        if (line.startsWith("      {")) {
                            starts[placeIndex] = i;
                        }
                        if (line.startsWith("      }")) {
                            ends[placeIndex] = i;
                            placeIndex++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println(file);
                    System.err.println(json);
                    throw new RuntimeException(e);
                }

                // Validate
                for (int i = 0; i < localities.size(); i++) {
                    int start = starts[i];
                    int end = ends[i];
                    if (start == 0) {
                        throw new IllegalStateException(file.toString());
                    }
                    if (end == 0) {
                        throw new IllegalStateException(file.toString());
                    }
                    if (start >= end) {
                        throw new IllegalStateException(file.toString());
                    }
                }

                for (int i = 0; i < localities.size(); i++) {
                    var locality = localities.get(i);
                    if (locality.oblast() != null) {
                        String fileName = getFileName(locality);
                        var newFile = new File(file.getParent(), fileName);
                        if (!newFile.equals(file)) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("{\n");
                            sb.append("   \"results\" : \n");
                            sb.append("   [\n");
                            sb.append("      {\n");
                            for (int j = starts[i] + 1; j < ends[i]; j++) {
                                sb.append(jsonLines.get(j)).append("\n");
                            }
                            sb.append("      }\n");
                            sb.append("   ],\n");
                            sb.append("   \"status\" : \"OK\"\n");
                            sb.append("}");
                            String placeJson = sb.toString();
                            if (newFile.exists()) {
                                var targetJson = Files.readString(newFile.toPath());
                                if (targetJson.equals(placeJson)) {
                                } else {
                                    System.err.println("cannot rename " + file + " to " + newFile + ". File already exists");
                                }
                            } else {
                                Files.writeString(newFile.toPath(), placeJson);
                            }
                            file.delete();
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getFileName(Locality locality) {
        String fileName = locality.oblast();
        if (locality.settlement() != null) {
            fileName += "_" + locality.rayon() + "_" + locality.settlement();
        } else if (locality.rayon() != null) {
            fileName += "_" + locality.rayon();
        }
        String key = fileName.toLowerCase().replace(' ', '-').replace("'", "ʼ");
        return key + ".json";
    }

}
