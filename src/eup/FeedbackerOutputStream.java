package eup;

import java.io.OutputStream;

public class FeedbackerOutputStream extends OutputStream {

    private static final String ampersand = "&" ;
    private static final String HTMLAmpersand = "&amp;" ;
    private static final String eol = System.getProperty("line.separator");
    private static final String HTMLBreak = "<br />";
    private static final String tab = "\t";
    private static final String HTMLTab = "&nbsp;&nbsp;&nbsp;&nbsp;"
	    + "&nbsp;&nbsp;&nbsp;&nbsp;";
    private static final String lessThan = "<" ;
    private static final String HTMLLessThan = "&lt;" ;
    private static final byte[] emptyByteArray = new byte[0];
    private static final int dest = Feedbacker.TO_FILE + Feedbacker.TO_GUI ;

    private StringBuilder sb = new StringBuilder(80);
    private final Feedbacker fb;
    private final String prefix;

    public FeedbackerOutputStream(Feedbacker fb, 
	    String prefix) {
	this.fb = fb;
	this.prefix = prefix;
    }

    @Override
    public void close() {
	/* do nothing */
    }

    @Override
    public void flush() {
	/* do nothing */
    }

    @Override
    public String toString() {
	return this.getClass().getName();
    }

    @Override
    public void write(byte[] b, int off, int len) {
	String s = new String(b, off, len);
	String s1 = s.replace(ampersand, HTMLAmpersand) ;
	String s2 = s1.replace(lessThan, HTMLLessThan) ;
	String s3 = prefix 
		+ s2.replace(eol, HTMLBreak)
		+ FeedbackerImplementation.getDefaultlogfont();
	String s4 = s3.replace(tab, HTMLTab);
	fb.logBare(s4, dest + Feedbacker.FLUSH);
    }

    @Override
    public void write(byte[] b) {
	write(b, 0, b.length);
    }

    @Override
    public void write(int arg0) {
	byte[] b = new byte[1];
	b[0] = (byte) (arg0 & 255);
	String s = new String(b);
	if (0 == sb.length()) {
	    fb.logBare(prefix, dest);
	}
	fb.logBare(s, dest);

	sb.append(s);
	if (sb.toString().endsWith(eol)) {
	    write(emptyByteArray, 0, 0);// Causes output of end of line to log.
	    fb.logBare(FeedbackerImplementation.getDefaultlogfont(), dest);
	    sb = new StringBuilder(80);
	}
    }

}