package org.eclipse.imp.parser;

import java.util.List;

/**
 * A subtype of IParseController that maintains a list of problem
 * marker types.  These types are presumed to correspond to errors
 * reported by the implementing parser.
 * 
 * There is no assumption that the parser itself determines these
 * marker types or creates markers of these types.  Rather, marker
 * types are presumed typically to be defined by other tools, such
 * as builders or compilers, that may make use of the parser and 
 * create problem markers in response to parse errors.
 * 
 * The information on marker types is made avaliable to and through
 * the parser for tools that use the parser, such as editors, that
 * may not have access to other tools that use the parser and that
 * may create problem markers based on parse errors.
 * 
 * This information can be used by an editor, for example, to
 * compare parse-error annotations with problem-marker annotations
 * made by a compiler and to update its representation of the the
 * marker annotations if they become out-of-date with respect to
 * the parse-error annotations.
 * 
 * As a means of enabling an editor to know which problem marker
 * annotations correspond to which parse-error annotations, this
 * mechanism is probably only temporary.  We expect that in the
 * future this will probably be supported by a metadata facility.
 * 
 * @author Stan Sutton (suttons@us.ibm.com)
 * @since May 1, 2007
 *
 */
public interface IParseControllerWithMarkerTypes extends IParseController {

	/**
	 * Add a type of problem marker to the list of problem marker
	 * types to which errors generated by this parser may correspond.
	 * 
	 * @param problemMarkerType	The name of the problem marker type
	 */
	public void addProblemMarkerType(String problemMarkerType);
	
	
	/**
	 * Provide the list of problem marker types to which errors
	 * generated by this parser may correspond.
	 * 
	 * @return	The list of problem marker types
	 */
	public List getProblemMarkerTypes();

	
	/**
	 * Remove a type of problem marker from the list of problem marker
	 * types to which errors generated by this parser may correspond.
	 * 
	 * @param problemMarkerType	The name of the problem marker type
	 */
	public void removeProblemMarkerType(String problemMarkerType);

}
