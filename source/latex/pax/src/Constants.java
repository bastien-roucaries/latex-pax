/* Constants.java
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

public interface Constants {

    public String PAX_VERSION = "0.1l";

    public String ENTRY_BEG = "\\[";
    public String ENTRY_END = "\\\\\n";
    public String CMD_BEG = "{";
    public String CMD_END = "}";
    public String ARG_BEG = "{";
    public String ARG_END = "}";
    public String KVS_BEG = "{";
    public String KVS_END = "\n}";
    public String KVS_EMPTY = "{}";
    public String KV_BEG  = "\n  ";
    public String KV_END  = ",";
    public String KEY_BEG = "";
    public String KEY_END = "";
    public String VALUE_BEG = "={";
    public String VALUE_END = "}";
    public String HEX_STR_BEG = "\\<";
    public String HEX_STR_END = "\\>";

    public String CMD_ANNOT   = "annot";
    public String CMD_BASEURL = "baseurl";
    public String CMD_DEST    = "dest";
    public String CMD_FILE    = "file";
    public String CMD_INFO    = "info";
    public String CMD_PAGENUM = "pagenum";
    public String CMD_PAGE    = "page";
    public String CMD_PAX     = "pax";

    // cmd file
    public String KEY_SIZE    = "Size";
    public String KEY_DATE    = "Date";

    // cmd info
    public String KEY_AUTHOR   = "Author";
    public String KEY_CREATOR  = "Creator";
    public String KEY_KEYWORDS = "Keywords";
    public String KEY_PRODUCER = "Producer";
    public String KEY_SUBJECT  = "Subject";
    public String KEY_TITLE    = "Title";

    // cmd page
    public String KEY_ROTATE    = "Rotate";
    public String KEY_MEDIA_BOX = "MediaBox";
    public String KEY_CROP_BOX  = "CropBox";
    public String KEY_BLEED_BOX = "BleedBox";
    public String KEY_TRIM_BOX  = "TrimBox";
    public String KEY_ART_BOX   = "ArtBox";

    // cmd annot attributes
    public String KEY_C      = "C";
    public String KEY_BORDER = "Border";
    public String KEY_BS     = "BS";
    public String KEY_H      = "H";

    // cmd annot/link/URI
    public String KEY_URI = "URI";
    public String KEY_IS_MAP = "IsMap";


    // cmd annot/link/GoToR
    public String KEY_FILE = "File";
    public String KEY_DEST_NAME = "DestName";
    public String KEY_DEST_PAGE = "DestPage";
    public String KEY_DEST_VIEW = "DestView";

    // cmd annot/link/GoTo
    public String KEY_DEST_RECT  = "Rect";
    public String KEY_DEST_X     = "DestX";
    public String KEY_DEST_Y     = "DestY";
    public String KEY_DEST_ZOOM  = "DestZoom";
    public String KEY_DEST_LABEL = "DestLabel";

    // cmd annot/link/Named
    public String KEY_NAME = "Name";

    // baseurl
    public String PDF_URI    = "URI";
    public String PDF_BASE   = "Base";

    // annotations
    public String PDF_ANNOT  = "Annot";
    public String PDF_RECT   = "Rect";
    public String PDF_LINK   = "Link";
    public String PDF_A      = "A";
    public String PDF_C      = "C";
    public String PDF_BORDER = "Border";
    public String PDF_BS     = "BS";
    public String PDF_H      = "H";
    public String PDF_GOTO   = "GoTo";
    public String PDF_DEST   = "Dest";

    // destination views
    public String PDF_XYZ    = "XYZ";
    public String PDF_FIT    = "Fit";
    public String PDF_FITH   = "FitH";
    public String PDF_FITV   = "FitV";
    public String PDF_FITR   = "FitR";
    public String PDF_FITB   = "FitB";
    public String PDF_FITBH  = "FitBH";
    public String PDF_FITBV  = "FitBV";

    public String PDF_NULL   = "null";

    public String PDF_S      = "S";
    public String PDF_NAMED  = "Named";
    public String PDF_N      = "N";
}
