/* Entry.java
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

public class Entry implements Constants {

    protected Writer out;

    protected String cmd;
    protected List args;
    protected Map kvs;
    protected boolean withKV;
    protected List delayedList;

    public Entry(Writer out) {
        this.out = out;
        cmd = null;
        args = new Vector();
        kvs = null;
        withKV = false;
        delayedList = new Vector();
    }

    public void clear() {
        cmd = null;
        args.clear();
        kvs = null;
        withKV = false;
    }

    public void setCmd(String cmd) {
        clear();
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }

    public void withKV() {
        this.withKV = true;
        if (kvs == null) {
            kvs = new HashMap();
        }
    }

    public void addArg(String arg) {
        args.add(arg);
    }

    public void putKV(String key, String value) {
        withKV();
        if (value == null) {
            return;
        }
        kvs.put(key, value);
    }

    public boolean isEmptyKV() {
        return !withKV || kvs.isEmpty();
    }

    public void writeDelayed() throws EntryWriteException {
        Writer saved = out;
        try {
            out = new StringWriter();
            write();
            delayedList.add(out.toString());
        }
        catch (Exception e) { throw new EntryWriteException(e); }
        finally {
            out = saved;
        }
    }

    public void write() throws EntryWriteException {
        try {
            out.write(ENTRY_BEG);

            // write command
            out.write(CMD_BEG);
            out.write(cmd);
            out.write(CMD_END);

            // write arguments
            Iterator it = args.iterator();
            while (it.hasNext()) {
                String arg = (String)it.next();
                out.write(ARG_BEG);
                out.write(arg);
                out.write(ARG_END);
            }

            // write key value pairs
            if (withKV) {
                if (kvs.isEmpty()) {
                    out.write(KVS_EMPTY);
                }
                else {
                    out.write(KVS_BEG);
                    it = kvs.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry kv = (Map.Entry)it.next();
                        String key = (String)kv.getKey();
                        String value = (String)kv.getValue();
                        out.write(KV_BEG);
                        out.write(KEY_BEG);
                        out.write(key);
                        out.write(KEY_END);
                        if (value != null) {
                            out.write(VALUE_BEG);
                            out.write(value);
                            out.write(VALUE_END);
                        }
                        out.write(KV_END);
                    }
                    out.write(KVS_END);
                }
            }

            out.write(ENTRY_END);
            out.flush();
        }
        catch (IOException e) {
            throw new EntryWriteException(e);
        }
        finally {
            clear();
        }
    }

    public void flushDelayed() throws EntryWriteException {
        try {
            Iterator it = delayedList.iterator();
            while (it.hasNext()) {
                out.write((String)it.next());
            }
            out.flush();
        }
        catch (IOException e) {
            throw new EntryWriteException(e);
        }
    }

    public void close() throws EntryWriteException {
        try {
            out.close();
        }
        catch (IOException e) {
            throw new EntryWriteException(e);
        }
    }
}
