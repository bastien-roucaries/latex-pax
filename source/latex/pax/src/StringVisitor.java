/* StringVisitor.java
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
import java.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.exceptions.*;

public class StringVisitor implements ICOSVisitor {

    protected StringBuffer buf;
    boolean nodelim;

    public StringVisitor() {
        buf = new StringBuffer();
        nodelim = false;
    }

    public Object visitFromNull(COSNull obj) {
        if (nodelim) {
            buf.append(' ');
        }
        buf.append("null");
        nodelim = true;
        return buf;
    }

    public Object visitFromBoolean(COSBoolean obj) {
        if (nodelim) {
           buf.append(' ');
        }
        buf.append(obj.getValue());
        nodelim = true;
        return buf;
    }

    public Object visitFromInt(COSInteger obj) {
        if (nodelim) {
            buf.append(' ');
        }
        buf.append(obj.intValue());
        nodelim = true;
        return buf;
    }

    public Object visitFromFloat(COSFloat obj) {
        if (nodelim) {
            buf.append(' ');
        }
        String s = "" + obj.floatValue();
        if (s.endsWith(".0")) {
            buf.append(s.substring(0, s.length() - ".0".length()));
        }
        else {
            buf.append(s);
        }
        nodelim = true;
        return buf;
    }

    public Object visitFromName(COSName obj) {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        try {
            obj.writePDF(a);
        }
        catch (IOException e) {}
        buf.append(a.toString());
        nodelim = true;
        return buf;
    }

    public Object visitFromString(COSString obj) {
        buf.append("\\<");
        buf.append(obj.getHexString());
        buf.append("\\>");
        nodelim = false;
        return buf;
    }

    public Object visitFromArray(COSArray obj) throws COSVisitorException {
        buf.append('[');
        nodelim = false;
        for (int i = 0; i < obj.size(); i++) {
            obj.getObject(i).accept(this);
        }
        buf.append(']');
        nodelim = false;
        return buf;
    }

    public Object visitFromDictionary(COSDictionary obj) throws COSVisitorException {
        buf.append("<<");
        nodelim = false;
        Iterator iter = obj.keyList().iterator();
        while (iter.hasNext()) {
            COSName key = (COSName)iter.next();
            COSBase value = obj.getDictionaryObject(key);
            key.accept(this);
            value.accept(this);
        }
        buf.append(">>");
        nodelim = false;
        return buf;
    }

    public Object visitFromStream(COSStream obj) throws COSVisitorException {
        throw new COSVisitorException(new Exception("Unsupported stream object"));
    }
    public Object visitFromDocument(COSDocument obj) throws COSVisitorException {
        throw new COSVisitorException(new Exception("Unsupported documetn object"));
    }
}
