package webPage ;
/**
 * A <tt>WPLocation</tt> represents a location (line and column
 * number) within a text document (such as a <tt>{@link WebPage}</tt>).
 * <p>
 * Once constructed, a <tt>WPLocation</tt> cannot be changed.
 * <p>
 * Common uses for a <tt>WPLocation</tt> would be as a bookmark, or
 * to provide the result from a search.
 * <p>
 * <i>Units</i> <br>
 * Lines and columns count from zero.  The first character of a document
 * is on line zero at column zero.  A negative value for the line or
 * the column is invalid, and is used to represent a failed search.
 *
 * @author  Ian Shef
 * @version 1.0 28 January 2003
 * @see WebPage
 * @see chargeNumberXRef
 * @since 1.0
 *
 */
public class WPLocation
{
  private int line ;
  private int column ;

  /**
   * No-argument constructor.
   * <p>
   * Returns an
   * invalid <tt>WPLocation</tt> (both the line and
   * the column are -1).
   */
  public WPLocation()
  {
    this(-1,-1) ;
  }


  /**
   * Construct a specified <tt>WPLocation</tt>.
   *
   * @param nline    Line number
   * @param ncolumn  Column number
   *
   */
  public WPLocation(int nline, int ncolumn)
  {
    this.line = nline ;
    this.column = ncolumn ;
  }

  /**
   * Provides the line number of this <tt>WPLocation</tt>.
   *
   * @return The line number.  First line is zero.
   *
   */
  public int getLine()
  {
    return this.line;
  }

  /**
   * Provides the column number of this <tt>WPLocation</tt>.
   *
   * @return The column number.  First column is zero.
   *
   */
  public int getColumn()
  {
    return this.column;
  }

  /**
   * Provides a text version of this <tt>WPLocation</tt>.
   *
   * @return A String representation of this <tt>WPLocation</tt>,
   *         "WPLocation:lxcy", where the x and the y are replaced
   *         by the line and column values of this <tt>WPLocation</tt>.
   *         <br><b>Example</b><br>
   *         If the <tt>WPLocation</tt> represents line 34 column 56
   *         (counting from 0), then <tt>toString()</tt> will return<br>
   *         <tt>WPLocation:l34c56</tt>.
   *
   */
  @Override
public String toString() {
    return ("WPLocation:l"+this.line+"c"+this.column) ;
  }
}