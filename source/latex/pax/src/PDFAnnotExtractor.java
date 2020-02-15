/* PDFAnnotExtractor.java
Copyright (C) 2006-2008, 2011, 2012 Heiko Oberdiek

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
MA 02110-1301  USA

This file is part of PDFAnnotExtractor. See README.
*/
package pax;

import java.io.*;
import java.text.*;
import java.util.*;

import org.pdfbox.cos.*;
import org.pdfbox.pdfparser.*;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;
import org.pdfbox.pdmodel.interactive.action.*;
import org.pdfbox.pdmodel.interactive.action.type.*;
import org.pdfbox.pdmodel.interactive.annotation.*;
import org.pdfbox.pdmodel.interactive.documentnavigation.destination.*;

public class PDFAnnotExtractor implements Constants {

    protected File inputFile;
    protected File outputFile;
    protected PDDocument doc;
    protected PDDocumentCatalog catalog;
    protected Entry entry;
    protected int destCount;

    protected static final String USAGE =
            "Syntax: java PDFAnnotExtractor <pdffiles[.pdf]>";
    protected static final String EXT_PDF = ".pdf";
    protected static final String EXT_PAX = ".pax";

    public static void main(String[] argv) {
        if (argv.length < 1) {
            System.err.println(USAGE);
            System.exit(1);
        }
        for(int i = 0; i < argv.length; i++) {
            processFile(argv[i]);
        }
    }

    public static void processFile(String fileName) {
        File file = new File(fileName);
        if (file.isFile()) {
            fileName = stripFromEnd(fileName, EXT_PDF);
        }
        else {
            File testFile = new File(fileName + EXT_PDF);
            if (testFile.isFile()) {
                file = testFile;
            }
            else {
               System.err.println(USAGE);
               error("PDF file not found: " + fileName, null);
            }
        }
        System.out.println("* Processing file `" + file.toString() + "' ...");
        PDFAnnotExtractor p =
                new PDFAnnotExtractor(file, new File(fileName + EXT_PAX));
        p.parse();
        p.close();
    }

    public PDFAnnotExtractor(File inputFile, File outputFile) {
        this.inputFile  = inputFile;
        this.outputFile = outputFile;
        destCount = 0;
        try {
            doc = PDDocument.load(inputFile);
            catalog = doc.getDocumentCatalog();
        }
        catch (IOException e) {
            error("Loading failed: " + inputFile, e);
        }
        try {
            entry = new Entry(new BufferedWriter(new FileWriter(outputFile)));
        }
        catch (IOException e) {
            error("Cannot open output: " + outputFile, e);
        }
    }

    public void close() {
        try {
            doc.close();
        }
        catch (IOException e) {
            error("Closing failed: " + inputFile, e);
        }
        try {
            entry.close();
        }
        catch (EntryWriteException e) {
            error("Closing failed: " + outputFile, e);
        }
    }

    public void parse() {
        cmd_pax();
        cmd_file();
        cmd_pagenum();
        cmd_baseurl();
        parse_pages();
    }

    public void cmd_pax() {
        try {
            // PAX version info
            entry.setCmd(CMD_PAX);
            entry.addArg(PAX_VERSION);
            entry.write();
        }
        catch (Exception e) { handleCmdException(e); }
    }

    public void cmd_file() {
        try {
            entry.setCmd(CMD_FILE);
            entry.addArg(formatString(inputFile.toString()));
            long size = inputFile.length();
            if (size > 0) {
                entry.putKV(KEY_SIZE, "" + size);
            }
            long time = inputFile.lastModified();
            if (time != 0) {
                Date d = new Date(time);
                DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                String date = "D:" + df.format(d);
                df = new SimpleDateFormat("Z");
                String zone = df.format(d);
                if (zone.equals("+0000")) {
                    zone = "Z";
                }
                else {
                    // ISO format: SIGN DIGIT DIGIT DIGIT DIGIT
                    zone = zone.substring(0, 3)
                           + "'"
                           + zone.substring(3, 5)
                           + "'";
                }
                entry.putKV(KEY_DATE, date + zone);
            }
            entry.write();
        }
        catch (Exception e) { handleCmdException(e); }
    }

    public void cmd_pagenum() {
        try {
            // pagenum
            int pagenum = doc.getNumberOfPages();
            entry.setCmd(CMD_PAGENUM);
            entry.addArg("" + pagenum);
            entry.write();
        }
        catch (Exception e) { handleCmdException(e); }
    }

    public void cmd_info() {
        try {
            // info
            entry.setCmd(CMD_INFO);
            PDDocumentInformation info = doc.getDocumentInformation();
            entry.putKV(KEY_AUTHOR,   formatString(info.getAuthor()));
            entry.putKV(KEY_CREATOR,  formatString(info.getCreator()));
            entry.putKV(KEY_KEYWORDS, formatString(info.getKeywords()));
            entry.putKV(KEY_PRODUCER, formatString(info.getProducer()));
            entry.putKV(KEY_SUBJECT,  formatString(info.getSubject()));
            entry.putKV(KEY_TITLE,    formatString(info.getTitle()));
            if (!entry.isEmptyKV()) {
                entry.write();
            }
        }
        catch (Exception e) { handleCmdException(e); }
    }

    public void cmd_baseurl() {
        try {
            // baseurl
            entry.setCmd(CMD_BASEURL);
            COSDictionary dict = catalog.getCOSDictionary();
            dict = (COSDictionary)
                   dict.getItem(COSName.getPDFName(PDF_URI));
            String baseurl = dict.getString(PDF_BASE);
            if (baseurl.length() > 0) {
                entry.addArg(formatString(baseurl));
                entry.write();
            }
        }
        catch (EntryWriteException e) { handleCmdException(e); }
        catch (Exception e) {}
    }

    public void parse_pages() {
        ListIterator iter = catalog.getAllPages().listIterator();
        while (iter.hasNext()) {
            // page
            PDPage page = (PDPage)iter.next();
            int num = iter.nextIndex();
            parse_page(num, page);
        }
        try {
            entry.flushDelayed();
        }
        catch (EntryWriteException e) {
            System.err.println("Write error: " + outputFile);
            System.err.println(e.getCause().toString());
        }
    }

    public void parse_page(int num, PDPage page) {
        cmd_page(num, page);
        parse_annots(num, page);
    }

    public void cmd_page(int num, PDPage page) {
        try {
            entry.setCmd(CMD_PAGE);
            entry.addArg("" + num);
            entry.withKV();

            // rotate entry
            int rot = page.findRotation();
            if (rot != 0) {
                entry.putKV(KEY_ROTATE, "" + rot);
            }

            // box entries
            PDRectangle mediaBox = page.findMediaBox();
            PDRectangle cropBox  = page.findCropBox();
            PDRectangle bleedBox = page.getBleedBox();
            PDRectangle trimBox  = page.getTrimBox();
            PDRectangle artBox   = page.getArtBox();
            entry.addArg(formatBox(mediaBox));
            if (!equals(cropBox, mediaBox)) {
                entry.putKV(KEY_CROP_BOX, formatBox(cropBox));
            }
            if (!equals(bleedBox, cropBox)) {
                entry.putKV(KEY_BLEED_BOX, formatBox(bleedBox));
            }
            if (!equals(trimBox, cropBox)) {
                entry.putKV(KEY_TRIM_BOX, formatBox(trimBox));
            }
            if (!equals(artBox, cropBox)) {
                entry.putKV(KEY_ART_BOX, formatBox(artBox));
            }
            entry.write();
        }
        catch (Exception e) { handleCmdException(e); }
    }

    public void parse_annots(int num, PDPage page) {
        // get annotations
        COSArray annots = null;
        try {
            COSDictionary page_dict = page.getCOSDictionary();
            annots = (COSArray)page_dict.getDictionaryObject(COSName.ANNOTS);
        }
        catch (Exception e) {}
        if (annots == null) {
            return;
        }

        for (int i = 0; i < annots.size(); i++) {
            try {
                COSDictionary annot = (COSDictionary)annots.getObject(i);
                cmd_annot(num, annot);
            }
            catch (Exception e) {}
        }
    }

    public void cmd_annot(int num, COSDictionary annot) {
        try {
            entry.setCmd(CMD_ANNOT);
            entry.addArg("" + num);

            // Type
            String type = annot.getNameAsString(COSName.TYPE, PDF_ANNOT);
            if (!type.equals(PDF_ANNOT)) {
                throw new Exception("Wrong annotation type: " + type);
            }
            // Subtype
            String subtype = annot.getNameAsString(COSName.SUBTYPE);
            if (subtype == null) {
                throw new Exception("Missing annotation subtype.");
            }
            if (!subtype.equals(PDF_LINK)) {
                throw new Exception("Unsupported annotation subtype: "
                                    + subtype);
            }
            entry.addArg(subtype);
            entry.withKV();
            // Rect
            COSArray array = (COSArray)
                             annot.getDictionaryObject(PDF_RECT);
            PDRectangle rect = new PDRectangle(array);
            entry.addArg(formatBox(rect));
            // A
            COSDictionary a = (COSDictionary)
                              annot.getDictionaryObject(PDF_A);
            if (a == null) {
                COSBase cos = annot.getDictionaryObject(PDF_DEST);
                if (cos == null) {
                  throw new Exception("Unsupported link annotation "
                                      + "without action.");
                }
                entry.addArg(PDF_GOTO);
                destCount++;
                entry.putKV(KEY_DEST_LABEL, "" + destCount);
                annot_attrs(annot);
                entry.write();
                add_dest(PDDestination.create(cos));
                entry.writeDelayed();
                return;
            }
            PDAction action = PDActionFactory.createAction(a);
            if (action == null) {
                // try named action, unsupported by PDFBox
                if (a.getNameAsString(PDF_S).equals(PDF_NAMED)) {
                    entry.addArg(PDF_NAMED);
                    String name = a.getNameAsString(PDF_N);
                    if (name != null) {
                        entry.putKV(KEY_NAME, name);
                        annot_attrs(annot);
                        entry.write();
                        return;
                    }
                }
                throw new Exception("Unsupported link annotation.");
            }
            String actionSubtype = action.getSubType();
            entry.addArg(actionSubtype);
            if (action instanceof PDActionURI) {
                PDActionURI uri = (PDActionURI)action;
                entry.putKV(KEY_URI, formatString(uri.getURI()));
                if (uri.shouldTrackMousePosition()) {
                    entry.putKV(KEY_IS_MAP, null);
                }
            }
            else if (action instanceof PDActionGoTo) {
                destCount++;
                entry.putKV(KEY_DEST_LABEL, "" + destCount);
                annot_attrs(annot);
                entry.write();
                add_dest(((PDActionGoTo)action).getDestination());
                entry.writeDelayed();
                return;
            }
            else if (action instanceof PDActionRemoteGoTo) {
                PDActionRemoteGoTo gotor = (PDActionRemoteGoTo)action;
                String file = gotor.getFile().getFile();
                if (file == null || file.length() == 0) {
                    throw new Exception("GoToR: missing file");
                }
                entry.putKV(KEY_FILE, formatString(gotor.getFile().getFile()));

                /*
                PDDestination d = PDDestination.create(gotor.getD());
                String view = "/";
                if (d instanceof PDNamedDestination) {
                    entry.putKV(KEY_DEST_NAME,
                                ((PDNamedDestination)d)
                                .getNamedDestination());
                }
                else {
                    if (d instanceof PDPageFitDestination) {
                        PDPageFitDestination p =
                                (PDPageFitDestination)d;
                        view += (p.fitBoundingBox()) ? PDF_FITB : PDF_FIT;
                    }
                    else if (d instanceof PDPageFitHeightDestination) {
                        PDPageFitHeightDestination p =
                                (PDPageFitHeightDestination)d;
                        view += (p.fitBoundingBox()) ? PDF_FITBV : PDF_FITV;
                        view += " ";
                        view += (p.getLeft() == -1) ? PDF_NULL : "" + p.getLeft();
                    }
                    else if (d instanceof PDPageFitWidthDestination) {
                        PDPageFitWidthDestination p =
                                (PDPageFitWidthDestination)d;
                        view += (p.fitBoundingBox()) ? PDF_FITBH : PDF_FITH;
                        view += " ";
                        view += (p.getTop() == -1) ? PDF_NULL : "" + p.getTop();
                    }
                    else if (d instanceof PDPageXYZDestination) {
                        PDPageXYZDestination p =
                                (PDPageXYZDestination)d;
                        view += PDF_XYZ;
                        view += " ";
                        view += (p.getLeft() == -1) ? PDF_NULL : "" + p.getLeft();
                        view += " ";
                        view += (p.getTop() == -1) ? PDF_NULL : "" + p.getTop();
                        view += " ";
                        view += (p.getZoom() == -1) ? PDF_NULL : "" + p.getZoom();
                    }
                    else if (d instanceof PDPageFitRectangleDestination) {
                        PDPageFitRectangleDestination p =
                                (PDPageFitRectangleDestination)d;
                        view += PDF_FITR;
                        view += " " + p.getLeft() + " " + p.getBottom();
                        view += " " + p.getRight() + " " + p.getTop();
                    }
                    else {
                        throw new Exception("Unknown destination type");
                    }
                    entry.putKV(KEY_DEST_VIEW, view);
                }
                */

                COSBase d = gotor.getD();
                if (d instanceof COSString) {
                    entry.putKV(KEY_DEST_NAME,
                                formatString(((COSString)d).getString()));
                }
                else if (d instanceof COSName) {
                    entry.putKV(KEY_DEST_NAME,
                                formatString(((COSName)d).getName()));
                }
                else if (d instanceof COSArray) {
                    COSArray dest = (COSArray)d;
                    int page_num = dest.getInt(0);
                    entry.putKV(KEY_DEST_PAGE, "" + page_num);
                    String view = dest.getName(1);
                    int size = dest.size();
                    if (view.equals(PDF_FIT) || view.equals(PDF_FITB)) {
                    }
                    else if (view.equals(PDF_FITH) || view.equals(PDF_FITBH)
                             || view.equals(PDF_FITV) || view.equals(PDF_FITBV)) {
                        if (size >= 3) {
                            try {
                                String args = formatNumber(
                                        ((COSNumber)dest.getObject(2)).floatValue());
                                view += " " + args;
                            }
                            catch (Exception e) {
                                size = 0;
                            }
                        }
                        if (size < 3) {
                            if (view.equals(PDF_FITH) || view.equals(PDF_FITV)) {
                                view = PDF_FIT;
                            }
                            else {
                                view = PDF_FITB;
                            }
                        }
                    }
                    else if (view.equals(PDF_XYZ)) {
                        COSBase obj;
                        view += " ";
                        if (size >= 3) {
                            obj = dest.getObject(2);
                            if (obj instanceof COSNumber) {
                                view += formatNumber(((COSNumber)obj).floatValue());
                            }
                            else {
                               view += PDF_NULL;
                            }
                        }
                        else {
                            view += PDF_NULL;
                        }
                        view += " ";
                        if (size >= 4) {
                            obj = dest.getObject(3);
                            if (obj instanceof COSNumber) {
                                view += formatNumber(((COSNumber)obj).floatValue());
                            }
                            else {
                               view += PDF_NULL;
                            }
                        }
                        else {
                            view += PDF_NULL;
                        }
                        view += " ";
                        if (size >= 5) {
                            obj = dest.getObject(4);
                            if (obj instanceof COSNumber) {
                                view += formatNumber(((COSNumber)obj).floatValue());
                            }
                            else {
                                view += PDF_NULL;
                            }
                        }
                        else {
                            view += PDF_NULL;
                        }
                    }
                    else if (view.equals(PDF_FITR)) {
                        view += " "
                             + formatNumber(((COSNumber)dest.getObject(2)).floatValue())
                             + " "
                             + formatNumber(((COSNumber)dest.getObject(3)).floatValue())
                             + " "
                             + formatNumber(((COSNumber)dest.getObject(4)).floatValue())
                             + " "
                             + formatNumber(((COSNumber)dest.getObject(5)).floatValue());
                    }
                    else {
                        throw new Exception("Unknown destination view type");
                    }
                    entry.putKV(KEY_DEST_VIEW, "/" + view);
                }
                else {
                    throw new Exception("GoToR: unknown dest type");
                }
            }
            else {
                throw new Exception("Unsupported link annotation type: "
                                    + actionSubtype);
            }
            annot_attrs(annot);
            entry.write();
        }
        catch (EntryWriteException e) { handleCmdException(e); }
        catch (Exception e) {
            System.err.println("!!! Warning: Annotation on page " + num
                               + " not recognized!");
            System.err.println("    " + e.toString());
            String msg = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null) {
                System.err.println("    " + cause.toString());
            }
            if (e instanceof ClassCastException) {
                e.printStackTrace();
            }
        }
    }

    private void add_dest(PDDestination dest) throws Exception {
        entry.setCmd(CMD_DEST);
        entry.withKV();
        if (dest instanceof PDNamedDestination) {
            String name = ((PDNamedDestination)dest).getNamedDestination();
            Object obj = catalog.getNames().getDests().getValue(name);
            if (obj instanceof PDDestination) {
                dest = (PDDestination)obj;
            }
        }
        if (dest instanceof PDPageDestination) {
            PDPageDestination pd = (PDPageDestination)dest;
            PDPage page = pd.getPage();
            int pagenum = catalog.getAllPages().indexOf(page);
            if (pagenum < 0) {
                throw new Exception("Link to unknown page");
            }
            entry.addArg("" + (pagenum + 1));
            entry.addArg("" + destCount);
            if (pd instanceof PDPageFitDestination) {
                PDPageFitDestination d = (PDPageFitDestination)pd;
                entry.addArg(d.fitBoundingBox() ? PDF_FITB : PDF_FIT);
                return;
            }
            if (pd instanceof PDPageFitHeightDestination) {
                PDPageFitHeightDestination d = (PDPageFitHeightDestination)pd;
                entry.addArg(d.fitBoundingBox() ? PDF_FITBV : PDF_FITV);
                COSArray a = d.getCOSArray();
                if (a.size() > 2) {
                    COSNumber n = (COSNumber)a.getObject(2);
                    if (n != null) {
                        entry.putKV(KEY_DEST_X, formatNumber(n));
                    }
                }
                return;
            }
            if (pd instanceof PDPageFitWidthDestination) {
                PDPageFitWidthDestination d = (PDPageFitWidthDestination)pd;
                entry.addArg(d.fitBoundingBox() ? PDF_FITBH : PDF_FITH);
                COSArray a = d.getCOSArray();
                if (a.size() > 2) {
                    COSNumber n = (COSNumber)a.getObject(2);
                    if (n != null) {
                        entry.putKV(KEY_DEST_Y, formatNumber(n));
                    }
                }
                return;
            }
            if (pd instanceof PDPageFitRectangleDestination) {
                PDPageFitRectangleDestination d = (PDPageFitRectangleDestination)pd;
                entry.addArg(PDF_FITR);
                COSArray a = d.getCOSArray();
                int size = a.size();
                if (size != 6) {
                    throw new Exception("Rectangle destination without"
                            + " correct number of parameters");
                }
                entry.putKV(KEY_DEST_RECT,
                            formatNumber((COSNumber)a.getObject(2)) + " "
                            + formatNumber((COSNumber)a.getObject(3)) + " "
                            + formatNumber((COSNumber)a.getObject(4)) + " "
                            + formatNumber((COSNumber)a.getObject(5)));
                return;
            }
            if (pd instanceof PDPageXYZDestination) {
                PDPageXYZDestination d = (PDPageXYZDestination)pd;
                entry.addArg(PDF_XYZ);
                COSArray a = d.getCOSArray();
                int size = a.size();
                COSNumber n;
                if (size > 2) {
                    n = (COSNumber)a.getObject(2);
                    if (n != null) {
                        entry.putKV(KEY_DEST_X, formatNumber(n));
                    }
                }
                if (size > 3) {
                    n = (COSNumber)a.getObject(3);
                    if (n != null) {
                        entry.putKV(KEY_DEST_Y, formatNumber(n));
                    }
                }
                if (size > 4) {
                    n = (COSNumber)a.getObject(4);
                    if (n != null) {
                        entry.putKV(KEY_DEST_ZOOM, formatNumber(n));
                    }
                }
                return;
            }
        }
        throw new Exception("Unknown destination type");
    }

    protected void annot_attrs(COSDictionary annot) {
        put_attr(annot, PDF_C, KEY_C);
        put_attr(annot, PDF_BORDER, KEY_BORDER);
        put_attr(annot, PDF_BS, KEY_BS);
        put_attr(annot, PDF_H, KEY_H);
    }

    protected void put_attr(COSDictionary annot,
                            String pdfKey, String entryKey) {
        try {
            COSBase value = annot.getDictionaryObject(pdfKey);
            if (value == null) {
                return;
            }
            String str = value.accept(new StringVisitor()).toString();
            if (str == null || str.length() == 0) {
                return;
            }
            entry.putKV(entryKey, str);
        }
        catch (Exception e) {}
    }

    protected boolean equals(PDRectangle a, PDRectangle b) {
        return a.getLowerLeftX() == b.getLowerLeftX()
            && a.getLowerLeftY() == b.getLowerLeftY()
            && a.getUpperRightX() == b.getUpperRightX()
            && a.getUpperRightY() == b.getUpperRightY();
    }

    protected String formatBox(PDRectangle r) {
        return formatNumber(r.getLowerLeftX()) + ' '
             + formatNumber(r.getLowerLeftY()) + ' '
             + formatNumber(r.getUpperRightX()) + ' '
             + formatNumber(r.getUpperRightY());
    }

    protected String formatString(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return HEX_STR_BEG
               + (new COSString(s)).getHexString()
               + HEX_STR_END;
    }

    protected String formatNumber(float f) {
        String s = "" + f;
        return stripFromEnd(s, ".0");
    }
    protected String formatNumber(COSNumber obj) {
        return formatNumber(obj.floatValue());
    }

    protected static String stripFromEnd(String str, String ext) {
        if (str.endsWith(ext)) {
            return str.substring(0, str.length() - ext.length());
        }
        return str;
    }

    protected static void error(String msg, Exception e, boolean stackTrace) {
        System.err.println("!!! Error: " + msg);
        if (e != null) {
            if (stackTrace) {
                e.printStackTrace();
            }
            else {
                System.err.println(e.toString());
            }
        }
        System.exit(1);
    }
    protected static void error(String msg, Exception e) {
        error(msg, e, false);
    }

    protected void handleCmdException(Exception e) {
        System.err.println("!!! Error during write of entry `"
                           + entry.getCmd() + "'!\n");
        if (e instanceof EntryWriteException) {
            System.err.println("Write error: " + outputFile);
            System.err.println(((EntryWriteException)e).getCause().toString());
        }
        else {
            e.printStackTrace();
        }
    }
}
