package org.fengfei.lanproxy.server.config.web;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MimeType {

    final static Pattern pattern = Pattern.compile("\\S*[?]\\S*");

    private static Map<String, String> h = new HashMap<String, String>();

    static {
        h.put("", "application/octet-stream");
        h.put("323", "text/h323");
        h.put("acx", "application/internet-property-stream");
        h.put("ai", "application/postscript");
        h.put("aif", "audio/x-aiff");
        h.put("aifc", "audio/x-aiff");
        h.put("aiff", "audio/x-aiff");
        h.put("asf", "video/x-ms-asf");
        h.put("asr", "video/x-ms-asf");
        h.put("asx", "video/x-ms-asf");
        h.put("au", "audio/basic");
        h.put("avi", "video/x-msvideo");
        h.put("axs", "application/olescript");
        h.put("bas", "text/plain");
        h.put("bcpio", "application/x-bcpio");
        h.put("bin", "application/octet-stream");
        h.put("bmp", "image/bmp");
        h.put("c", "text/plain");
        h.put("cat", "application/vnd.ms-pkiseccat");
        h.put("cdf", "application/x-cdf");
        h.put("cer", "application/x-x509-ca-cert");
        h.put("class", "application/octet-stream");
        h.put("clp", "application/x-msclip");
        h.put("cmx", "image/x-cmx");
        h.put("cod", "image/cis-cod");
        h.put("cpio", "application/x-cpio");
        h.put("crd", "application/x-mscardfile");
        h.put("crl", "application/pkix-crl");
        h.put("crt", "application/x-x509-ca-cert");
        h.put("csh", "application/x-csh");
        h.put("css", "text/css");
        h.put("dcr", "application/x-director");
        h.put("der", "application/x-x509-ca-cert");
        h.put("dir", "application/x-director");
        h.put("dll", "application/x-msdownload");
        h.put("dms", "application/octet-stream");
        h.put("doc", "application/msword");
        h.put("dot", "application/msword");
        h.put("dvi", "application/x-dvi");
        h.put("dxr", "application/x-director");
        h.put("eps", "application/postscript");
        h.put("etx", "text/x-setext");
        h.put("evy", "application/envoy");
        h.put("exe", "application/octet-stream");
        h.put("fif", "application/fractals");
        h.put("flr", "x-world/x-vrml");
        h.put("gif", "image/gif");
        h.put("gtar", "application/x-gtar");
        h.put("gz", "application/x-gzip");
        h.put("h", "text/plain");
        h.put("hdf", "applicatin/x-hdf");
        h.put("hlp", "application/winhlp");
        h.put("hqx", "application/mac-binhex40");
        h.put("hta", "application/hta");
        h.put("htc", "text/x-component");
        h.put("htm", "text/html");
        h.put("html", "text/html");
        h.put("htt", "text/webviewhtml");
        h.put("ico", "image/x-icon");
        h.put("ief", "image/ief");
        h.put("iii", "application/x-iphone");
        h.put("ins", "application/x-internet-signup");
        h.put("isp", "application/x-internet-signup");
        h.put("jfif", "image/pipeg");
        h.put("jpe", "image/jpeg");
        h.put("jpeg", "image/jpeg");
        h.put("jpg", "image/jpeg");
        h.put("js", "application/x-javascript");
        h.put("latex", "application/x-latex");
        h.put("lha", "application/octet-stream");
        h.put("lsf", "video/x-la-asf");
        h.put("lsx", "video/x-la-asf");
        h.put("lzh", "application/octet-stream");
        h.put("m13", "application/x-msmediaview");
        h.put("m14", "application/x-msmediaview");
        h.put("m3u", "audio/x-mpegurl");
        h.put("man", "application/x-troff-man");
        h.put("mdb", "application/x-msaccess");
        h.put("me", "application/x-troff-me");
        h.put("mht", "message/rfc822");
        h.put("mhtml", "message/rfc822");
        h.put("mid", "audio/mid");
        h.put("mny", "application/x-msmoney");
        h.put("mov", "video/quicktime");
        h.put("movie", "video/x-sgi-movie");
        h.put("mp2", "video/mpeg");
        h.put("mp3", "audio/mpeg");
        h.put("mpa", "video/mpeg");
        h.put("mpe", "video/mpeg");
        h.put("mpeg", "video/mpeg");
        h.put("mpg", "video/mpeg");
        h.put("mpp", "application/vnd.ms-project");
        h.put("mpv2", "video/mpeg");
        h.put("ms", "application/x-troff-ms");
        h.put("mvb", "application/x-msmediaview");
        h.put("nws", "message/rfc822");
        h.put("oda", "application/oda");
        h.put("p10", "application/pkcs10");
        h.put("p12", "application/x-pkcs12");
        h.put("p7b", "application/x-pkcs7-certificates");
        h.put("p7c", "application/x-pkcs7-mime");
        h.put("p7m", "application/x-pkcs7-mime");
        h.put("p7r", "application/x-pkcs7-certreqresp");
        h.put("p7s", "application/x-pkcs7-signature");
        h.put("pbm", "image/x-portable-bitmap");
        h.put("pdf", "application/pdf");
        h.put("pfx", "application/x-pkcs12");
        h.put("pgm", "image/x-portable-graymap");
        h.put("pko", "application/ynd.ms-pkipko");
        h.put("pma", "application/x-perfmon");
        h.put("pmc", "application/x-perfmon");
        h.put("pml", "application/x-perfmon");
        h.put("pmr", "application/x-perfmon");
        h.put("pmw", "application/x-perfmon");
        h.put("pnm", "image/x-portable-anymap");
        h.put("pot,", "application/vnd.ms-powerpoint");
        h.put("ppm", "image/x-portable-pixmap");
        h.put("pps", "application/vnd.ms-powerpoint");
        h.put("ppt", "application/vnd.ms-powerpoint");
        h.put("prf", "application/pics-rules");
        h.put("ps", "application/postscript");
        h.put("pub", "application/x-mspublisher");
        h.put("qt", "video/quicktime");
        h.put("ra", "audio/x-pn-realaudio");
        h.put("ram", "audio/x-pn-realaudio");
        h.put("ras", "image/x-cmu-raster");
        h.put("rgb", "image/x-rgb");
        h.put("rmi", "audio/mid");
        h.put("roff", "application/x-troff");
        h.put("rtf", "application/rtf");
        h.put("rtx", "text/richtext");
        h.put("scd", "application/x-msschedule");
        h.put("sct", "text/scriptlet");
        h.put("setpay", "application/set-payment-initiation");
        h.put("setreg", "application/set-registration-initiation");
        h.put("sh", "application/x-sh");
        h.put("shar", "application/x-shar");
        h.put("sit", "application/x-stuffit");
        h.put("snd", "audio/basic");
        h.put("spc", "application/x-pkcs7-certificates");
        h.put("spl", "application/futuresplash");
        h.put("src", "application/x-wais-source");
        h.put("sst", "application/vnd.ms-pkicertstore");
        h.put("stl", "application/vnd.ms-pkistl");
        h.put("stm", "text/html");
        h.put("svg", "image/svg+xml");
        h.put("sv4cpio", "application/x-sv4cpio");
        h.put("sv4crc", "application/x-sv4crc");
        h.put("swf", "application/x-shockwave-flash");
        h.put("t", "application/x-troff");
        h.put("tar", "application/x-tar");
        h.put("tcl", "application/x-tcl");
        h.put("tex", "application/x-tex");
        h.put("texi", "application/x-texinfo");
        h.put("texinfo", "application/x-texinfo");
        h.put("tgz", "application/x-compressed");
        h.put("tif", "image/tiff");
        h.put("tiff", "image/tiff");
        h.put("tr", "application/x-troff");
        h.put("trm", "application/x-msterminal");
        h.put("tsv", "text/tab-separated-values");
        h.put("txt", "text/plain");
        h.put("uls", "text/iuls");
        h.put("ustar", "application/x-ustar");
        h.put("vcf", "text/x-vcard");
        h.put("vrml", "x-world/x-vrml");
        h.put("wav", "audio/x-wav");
        h.put("wcm", "application/vnd.ms-works");
        h.put("wdb", "application/vnd.ms-works");
        h.put("wks", "application/vnd.ms-works");
        h.put("wmf", "application/x-msmetafile");
        h.put("wps", "application/vnd.ms-works");
        h.put("wri", "application/x-mswrite");
        h.put("wrl", "x-world/x-vrml");
        h.put("wrz", "x-world/x-vrml");
        h.put("xaf", "x-world/x-vrml");
        h.put("xbm", "image/x-xbitmap");
        h.put("xla", "application/vnd.ms-excel");
        h.put("xlc", "application/vnd.ms-excel");
        h.put("xlm", "application/vnd.ms-excel");
        h.put("xls", "application/vnd.ms-excel");
        h.put("xlt", "application/vnd.ms-excel");
        h.put("xlw", "application/vnd.ms-excel");
        h.put("xof", "x-world/x-vrml");
        h.put("xpm", "image/x-xpixmap");
        h.put("xwd", "image/x-xwindowdump");
        h.put("z", "application/x-compress");
        h.put("zip", "application/zip");
    }

    public static String getMimeType(String docType) {
        String mime = h.get(docType);
        if (mime == null) {
            mime = "application/octet-stream";
        }
        return mime;
    }

    public static String parseSuffix(String url) {
        try {
            Matcher matcher = pattern.matcher(url);
            String[] spUrl = url.toString().split("/");
            int len = spUrl.length;
            String endUrl = spUrl[len - 1];
            if (matcher.find()) {
                String[] spEndUrl = endUrl.split("\\?");
                endUrl = spEndUrl[0];
            }
            String[] endUrlArr = endUrl.split("\\.");
            return endUrlArr[endUrlArr.length - 1];
        } catch (Exception e) {
            return "";
        }
    }
}
