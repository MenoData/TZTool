package de.menodata.cldr;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class TransferZoneNamesCLDR {

    private static final String CLDR_PATH =  "C:/work/libs/unicode/core_35/common/main/";
    private static final File OUTPUT_DIR = new File("C:/work/libs/unicode/output/");
    private static final String SEP = System.getProperty("line.separator");

    private static final String UTC_LITERAL = "utc-literal";
    private static final String OFFSET_PATTERN = "offset-pattern";

    public static void main(String[] args) throws IOException, XMLStreamException {

//        process("as");

        String[] locales =
            ("af am ar as ast az be bg bn br bs ca cs cy da de ee el en eo es et eu fa fi fil fo fr fy ga gd gl gu "
            + "he hi hr hu hy id is it ja ka kab kk km kn ko ku ky lb lo lt lv mk ml mn mr ms mt my nb ne nl nn or "
            + "pa pl ps pt ro ru sd si sk sl so sq sr sv sw ta te th tk to tr ug uk ur uz vi zh zu")
            .split(" ");

        for (String locale : locales) {
            process(locale);
        }

        process("ar_DZ");
        process("ar_IQ");
        process("ar_JO");
        process("ar_LB");
        process("ar_MA");
        process("ar_MR");
        process("ar_PS");
        process("ar_SY");
        process("ar_TN");

        process("de_AT");

        process("en_AU");
        process("en_CA");
        process("en_GB");
        process("en_IE");
        process("en_IN");
        process("en_MT");
        process("en_NZ");
        process("en_ZA");

        process("es_AR");
        process("es_CL");
        process("es_CO");
        process("es_DO");
        process("es_EC");
        process("es_GT");
        process("es_HN");
        process("es_MX");
        process("es_PA");
        process("es_PE");
        process("es_PH");
        process("es_PR");
        process("es_PY");
        process("es_US");
        process("es_UY");
        process("es_VE");

        process("fa_AF");

        process("fr_BE");
        process("fr_CA");
        process("fr_CH");
        process("fr_MA");

        process("it_CH");

        process("pt_PT");

        process("zh_TW");

/*
*/
    }

    private static void process(String locale) throws IOException, XMLStreamException {
        Map<String, String> transferMap = new LinkedHashMap<String, String>();

        int underscore = locale.indexOf("_");
        Locale loc;
        if (underscore == -1) {
            loc = new Locale(locale);
        } else {
            loc = new Locale(locale.substring(0, underscore), locale.substring(underscore + 1));
        }
        transferZoneNames(loc, transferMap);

        if (transferMap.isEmpty()) {
            System.out.println("No entries found for: " + loc);
            return;
        }

        Writer writer = null;
        try {
            File target =
                new File(OUTPUT_DIR, "tzname" + (locale.equals("root") ? "" : "_" + locale) + ".properties");
            OutputStream os = new FileOutputStream(target);
            writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            String previous = null;

            if (transferMap.containsKey(UTC_LITERAL)) {
                writer.write("# localized offset format");
                writer.write(SEP);
                writer.write(UTC_LITERAL);
                writer.write('=');
                writer.write(transferMap.get(UTC_LITERAL));
                writer.write(SEP);
                writer.write(OFFSET_PATTERN);
                writer.write('=');
                writer.write(transferMap.get(OFFSET_PATTERN));
                writer.write(SEP);
                transferMap.remove(UTC_LITERAL);
                transferMap.remove(OFFSET_PATTERN);
            }

            for (String key : transferMap.keySet()) {
                String start = key;
                if (key.startsWith("region-") && (previous != null) && previous.startsWith("region-")) {
                    start = previous;
                }
                if (!start.equals(previous)) {
                    previous = key;
                    if (start.startsWith("region-")) {
                        writer.write(SEP);
                        writer.write("# region formats");
                        writer.write(SEP);
                    }
                }

                writer.write(key);
                writer.write('=');
                writer.write(transferMap.get(key));
                writer.write(SEP);
            }

        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        System.out.println("End of CLDR-transfer of: " + loc);
    }

    private static void transferZoneNames(Locale locale, Map<String, String> transferMap)
        throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {

        XMLEventReader reader = xmlReader(locale);
        boolean hourFormat = false;
        boolean gmtFormat = false;
        boolean gmtZeroFormat = false;
        String regionKey = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                StartElement element = (StartElement) event;
                String ename = element.getName().getLocalPart();

                if (ename.equals("hourFormat")) {
                    hourFormat = true;
                } else if (ename.equals("gmtFormat")) {
                    gmtFormat = true;
                } else if (ename.equals("gmtZeroFormat")) {
                    gmtZeroFormat = true;
                } else if (ename.equals("regionFormat")) {
                    String type = getAttribute(element, "type");
                    if (type == null) {
                        regionKey = "region-generic";
                    } else if (type.equals("standard")) {
                        regionKey = "region-standard";
                    } else if (type.equals("daylight")) {
                        regionKey = "region-daylight";
                    }
                }
            } else if (event.isEndElement()) {
                hourFormat = false;
                gmtFormat = false;
                gmtZeroFormat = false;
                regionKey = null;
            } else if (event.isCharacters()) {
                String data = event.asCharacters().getData();
                if (hourFormat) {
                    int index = data.lastIndexOf("HH");
                    if (index == -1) {
                        index = data.lastIndexOf("H");
                        index++;
                    } else {
                        index += 2;
                    }
                    if (index > 0) {
                        int next = data.indexOf("mm", index);
                        if (next == -1) {
                            throw new IllegalStateException("Literal mm not found: " + locale);
                        }
                        String separator = data.substring(index, next);
                        String pattern = "\u00B1hh" + separator + "mm";
                        transferMap.put(OFFSET_PATTERN, pattern);
                    }
                } else if (gmtFormat) {
                    String pattern = transferMap.get(OFFSET_PATTERN);
                    if (pattern == null) {
                        throw new IllegalStateException("Offset pattern not found: " + locale);
                    }
                    data = data.replace("{0}", pattern).trim();
                    transferMap.put(OFFSET_PATTERN, data);
                } else if (gmtZeroFormat) {
                    transferMap.put(UTC_LITERAL, data.trim());
                } else if (regionKey != null) {
                    transferMap.put(regionKey, data.trim());
                }
            }
        }

    }

    private static String getAttribute(
        StartElement element,
        String name
    ) {

        Attribute attr = element.getAttributeByName(new QName(name));
        return ((attr == null) ? null : attr.getValue());

    }

    private static XMLEventReader xmlReader(Locale locale)
        throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {

        return xmlReader(locale, CLDR_PATH);

    }

    private static XMLEventReader xmlReader(Locale locale, String path)
        throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {

        String language = locale.getLanguage();

        if (language.equals("in")) {
            language = "id";
        } else if (language.equals("iw")) {
            language = "he";
        }

        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        String country = locale.getCountry();
        if (language.equals("zh") && country.equals("TW")) {
            country = "Hant";
        }
        if (!country.isEmpty()) {
            country = "_" + country;
        }
        String source = path + language + country + ".xml";
        InputStream is = new FileInputStream(source);
        InputStreamReader reader = new InputStreamReader(is, "UTF-8");
        return f.createXMLEventReader(reader);

    }

}
