/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2021 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (WinZoneCompiler.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.time4j.tz.Timezone;


/**
 * <p>Compiles the original CLDR-file &quot;windowsZones.xml&quot; into
 * a serialized repository file with name &quot;winzone.ser&quot;. </p>
 *
 * @author  Meno Hochschild
 * @since   1.0
 */
/*[deutsch]
 * <p>Kompiliert die originale CLDR-Datei &quot;windowsZones.xml&quot; in
 * eine serialisierte Datei mit dem Namen &quot;winzone.ser&quot;. </p>
 *
 * @author  Meno Hochschild
 * @since   1.0
 */
public class WinZoneCompiler {

    //~ Statische Felder/Initialisierungen --------------------------------

    private static final String LF = System.getProperty("line.separator");
    private static final String VERSION_KEY = "VERSION";
    private static final String CLDR_FILE = "windowsZones.xml";
    private static final String SER_FILE = "winzone.ser";

    //~ Instanzvariablen --------------------------------------------------

    private final File workdir;

    //~ Konstruktoren -----------------------------------------------------

    private WinZoneCompiler(File workdir) {
        super();

        this.workdir = workdir;

        if (
            (workdir == null)
            || !workdir.exists()
        ) {
            throw new IllegalArgumentException(
                "Work directory does not exist: " + workdir);
        } else if (!workdir.isDirectory()) {
            throw new IllegalArgumentException(
                "Directory required: " + workdir);
        }
    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Main executable method which accepts a command line option for
     * the editable work directory containing the original CLDR-file. </p>
     *
     * <p>Recognized options are: </p>
     *
     * <dl>
     *  <dt>-help</dt>
     *  <dd>Print a help message</dd>
     *  <dt>-workdir</dt>
     *  <dd>Set working directory which contains the original CLDR-file
     *  by giving next command line argument as absolute directory path</dd>
     * </dl>
     *
     * @param   args    command line parameters
     * @throws  IllegalArgumentException if the working directory is wrong
     * @throws  IOException in case of any I/O-failure
    */
    /*[deutsch]
     * <p>Ausf&uuml;hrbare Hauptmethode zum Kompilieren der originalen
     * CLDR-Datei mit Hilfe eins Arbeitsverzeichnisses. </p>
     *
     * <p>Unterst&uuml;tzte Kommandozeilenparameter sind: </p>
     *
     * <dl>
     *  <dt>-help</dt>
     *  <dd>Print a help message</dd>
     *  <dt>-workdir</dt>
     *  <dd>Set working directory which contains the original CLDR-file
     *  by giving next command line argument as absolute directory path</dd>
     * </dl>
     *
     * @param   args    Kommandozeilenparameter
     * @throws  IllegalArgumentException bei falschem Arbeitsverzeichnis
     * @throws  IOException bei Zugriffsfehlern
     */
    public static void main(String[] args) throws IOException {
        if (
            (args == null)
            || (args.length == 0)
        ) {
            printOptions();
            return;
        }

        File workdir = null;
        boolean helpMode = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.equals("-help")) {
                helpMode = true;
            } else if (
                arg.equals("-workdir")
                && (workdir == null)
                && (++i < args.length)
            ) {
                workdir = new File(args[i]);
            } else {
                System.out.println("Unrecognized option: " + arg);
            }
        }

        if (helpMode || (workdir == null)) {
            printOptions();
            return;
        }

        WinZoneCompiler compiler = new WinZoneCompiler(workdir);
        Map<String, Map<String, String>> data = compiler.loadCLDR();
        String version = data.get(VERSION_KEY).keySet().iterator().next();
        data.remove(VERSION_KEY);
        compiler.writeRepository(version, data);
    }

    /**
     * <p>Determines the working directory of this instance. </p>
     *
     * @return  directory as {@code File}-object
     */
    /*[deutsch]
     * <p>Gibt das verwendete Basisverzeichnis an. </p>
     *
     * @return  Verzeichnis als {@code File}-Objekt
     */
    public File getWorkingDirectory() {
        return this.workdir;
    }

    private Map<String, Map<String, String>> loadCLDR() throws IOException {
        Map<String, Map<String, String>> repository = new HashMap<>();
        InputStream is = null;

        try {
            XMLInputFactory f = XMLInputFactory.newInstance();
            f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            File source = new File(this.workdir, CLDR_FILE);
            is = new FileInputStream(source);
            XMLEventReader reader = f.createXMLEventReader(is);

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                if (event.isStartElement()) {
                    StartElement element = (StartElement) event;
                    transfer(repository, element);
                }
            }
        } catch (FactoryConfigurationError | XMLStreamException error) {
            throw new IllegalStateException(error);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return repository;
    }

    private static void transfer(
        Map<String, Map<String, String>> repository,
        StartElement element
    ) {
        switch (element.getName().getLocalPart()) {
            case "mapZone":
                String country = getAttribute(element, "territory");
                String id = getAttribute(element, "type");
                String name = getAttribute(element, "other");
                fill(repository, country, id, name);
                break;
            case "version":
                {
                    String version = getAttribute(element, "number");
                    setVersion(repository, version);
                    break;
                }
            case "mapTimezones":
                {
                    String version = getAttribute(element, "typeVersion");
                    enhanceVersion(repository, version);
                    break;
                }
            default:
                break;
        }
    }

    private static String getAttribute(
        StartElement element,
        String name
    ) {
        return element.getAttributeByName(new QName(name)).getValue();
    }

    private static void fill(
        Map<String, Map<String, String>> repository,
        String country,
        String id,
        String name
    ) {
        Map<String, String> data = repository.get(country);

        if (data == null) {
            data = new HashMap<>();
            repository.put(country, data);
        }

        for (String tzid : id.split(" ")) {
            // assumption: no ambivalent mapping from ids to names
            data.put("WINDOWS~" + Timezone.normalize(tzid).canonical(), name);
        }
   }

    private static void setVersion(
        Map<String, Map<String, String>> repository,
        String version
    ) {
        Map<String, String> entry = repository.get(VERSION_KEY);

        if (entry == null) {
            entry = Collections.singletonMap(version, "");
        }
        
        repository.put(VERSION_KEY, entry);        
    }

    private static void enhanceVersion(
        Map<String, Map<String, String>> repository,
        String version
    ) {
        Map<String, String> entry = repository.get(VERSION_KEY);
        StringBuilder sb = new StringBuilder(entry.keySet().iterator().next());
        sb.append(" (");
        sb.append(version);
        sb.append(')');
        String key = sb.toString();
        repository.put(VERSION_KEY, Collections.singletonMap(key, ""));  
    }

    private void writeRepository(
        String version,
        Map<?, ?> data
    ) throws IOException {
        File target = new File(this.workdir, SER_FILE);
        try (
            ObjectOutputStream oos = 
                new ObjectOutputStream(
                    new FileOutputStream(target))
        ) {
            oos.writeUTF(version);
            oos.writeObject(data);
            System.out.println("Successfully created: " + target);
        }
    }

    private static void printOptions() {
        String help =
            "Usage of winzone compiler with command line options:"
            + LF
            + "-help      Print this usage message"
            + LF
            + "-workdir   Set working directory which contains timezone "
            + "data by giving next command line argument as absolute path"
            + LF;
        System.out.println(help);
    }

}
