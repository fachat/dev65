/*
 * Copyright (C),2005-2010 Andrew John Jacobs.
 *
 * This program is provided free of charge for educational purposes
 *
 * Redistribution and use in binary form without modification, is permitted
 * provided that the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS 'AS IS' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.co.demon.obelisk.xasm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import uk.co.demon.obelisk.xapp.Application;
import uk.co.demon.obelisk.xobj.Expr;
import uk.co.demon.obelisk.xobj.Extern;
import uk.co.demon.obelisk.xobj.Hex;
import uk.co.demon.obelisk.xobj.Module;
import uk.co.demon.obelisk.xobj.Section;
import uk.co.demon.obelisk.xobj.Value;

/**
 * The <CODE>Assembler</CODE> class implements the framework for a generic
 * assembler customised by derived classes to match specific processors.
 * 
 * @author 	Andrew Jacobs
 * @version	$Id$
 */
public abstract class Assembler extends Application
{
	protected static String		WRN_LABEL_IGNORED
		= "This statement cannot be labelled";

	protected static String		ERR_UNKNOWN_OPCODE
		= "Unknown opcode or directive";

	protected static String		ERR_NO_SECTION
		= "No active section";

	protected static String		ERR_CONSTANT_EXPR
		= "Constant expression required";

	protected static String		ERR_NO_OPEN_IF
		= ".ELSE or .ENDIF with no matching .IF";

	protected static String		ERR_CLOSING_PAREN
		= "Closing parenthesis missing in expression";

	protected static String		ERR_NO_GLOBAL
		= "A local label must be preceded by normal label";

	protected static String		ERR_UNDEF_SYMBOL
		= "Undefined symbol: ";
	
	protected static String		ERR_LABEL_REDEFINED
		= "Label as already been defined: ";
	
	protected static String		ERR_FAILED_TO_FIND_FILE
		= "Failed to find specified file";
	
	protected static String		ERR_EXPECTED_QUOTED_FILENAME
		= "Expected quoted filename";
	
	protected static String		ERR_INSERT_IO_ERROR
		= "I/O error while inserting binary data";
	
	protected static String		ERR_INVALID_EXPRESSION
		= "Invalid expression";

	/**
	 * A <CODE>TokenKind</CODE> representing mathematical operators.
	 */
	protected static final TokenKind	OPERATOR
		= new TokenKind ("OPERATOR");
	
	/**
	 * A <CODE>TokenKind</CODE> representing symbols (e.g. non-keywords).
	 */
	protected static final TokenKind	SYMBOL
		= new TokenKind ("SYMBOL");
	
	/**
	 * A <CODE>TokenKind</CODE> representing keywords (e.g. directives, opcodes).
	 */
	protected static final TokenKind	KEYWORD
		= new TokenKind ("KEYWORD");
	
	/**
	 * A <CODE>TokenKind</CODE> representing numerical values.
	 */
	protected static final TokenKind	NUMBER
		= new TokenKind	("NUMBER");
	
	/**
	 * A <CODE>TokenKind</CODE> representing string values.
	 */
	protected static final TokenKind	STRING
		= new TokenKind	("STRING");
		
	/**
	 * A <CODE>TokenKind</CODE> representing unclassifiable tokens.
	 */
	protected static final TokenKind	UNKNOWN	
		= new TokenKind ("UNKNOWN");

	/**
	 * A <CODE>Token</CODE> representing white-space.
	 */
	protected final Token 		WS		= new Token (UNKNOWN, "#SPACE");
	
	/**
	 * A <CODE>Token</CODE> representing the end-of-line.
	 */
	protected final Opcode 		EOL		= new Opcode (UNKNOWN, "#EOL")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			return (true);
		}
	};
	
	/**
	 * A <CODE>Token</CODE> representing the origin (e.g. $ or @).
	 */
	protected final Token 		ORIGIN	= new Token (KEYWORD, "ORIGIN");

	/**
	 * A <CODE>Token</CODE> representing a comma.
	 */
	protected final Token 		COMMA	= new Token (KEYWORD, ",");

	/**
	 * A <CODE>Token</CODE> representing a colon.
	 */
	protected final Token 		COLON	= new Token (KEYWORD, ":");

	/**
	 * A <CODE>Token</CODE> representing addition.
	 */
	protected final Token 		PLUS
		= new Token (OPERATOR, "+");
	
	/**
	 * A <CODE>Token</CODE> representing subtraction.
	 */
	protected final Token 		MINUS
		= new Token (OPERATOR, "-");
	
	/**
	 * A <CODE>Token</CODE> representing multiply.
	 */
	protected final Token 		TIMES
		= new Token (OPERATOR, "*");
	
	/**
	 * A <CODE>Token</CODE> representing divide.
	 */
	protected final Token 		DIVIDE
		= new Token (OPERATOR, "/");
	
	/**
	 * A <CODE>Token</CODE> representing modulo.
	 */
	protected final Token 		MODULO
		= new Token (OPERATOR, "%");

	/**
	 * A <CODE>Token</CODE> representing complement.
	 */
	protected final Token 		COMPLEMENT
		= new Token (OPERATOR, "~");
	
	/**
	 * A <CODE>Token</CODE> representing binary and.
	 */
	protected final Token 		BINARYAND
		= new Token (OPERATOR, "&");
	
	/**
	 * A <CODE>Token</CODE> representing binary or.
	 */
	protected final Token 		BINARYOR
		= new Token (OPERATOR, "|");
	
	/**
	 * A <CODE>Token</CODE> representing binary xor.
	 */
	protected final Token 		BINARYXOR
		= new Token (OPERATOR, "^");

	/**
	 * A <CODE>Token</CODE> representing logical not.
	 */
	protected final Token 		LOGICALNOT
		= new Token (OPERATOR, "!");
	
	/**
	 * A <CODE>Token</CODE> representing logical and.
	 */
	protected final Token 		LOGICALAND
		= new Token (OPERATOR, "&&");
	
	/**
	 * A <CODE>Token</CODE> representing logical or.
	 */
	protected final Token 		LOGICALOR
		= new Token (OPERATOR, "||");

	/**
	 * A <CODE>Token</CODE> representing equal.
	 */
	protected final Token 		EQ
		= new Token (OPERATOR, "=");
	
	/**
	 * A <CODE>Token</CODE> representing not equal.
	 */
	protected final Token 		NE
		= new Token (OPERATOR, "!=");
	
	/**
	 * A <CODE>Token</CODE> representing less than.
	 */
	protected final Token 		LT
		= new Token (OPERATOR, "<");
	
	/**
	 * A <CODE>Token</CODE> representing less than or equal.
	 */
	protected final Token 		LE
		= new Token (OPERATOR, "<=");
	
	/**
	 * A <CODE>Token</CODE> representing greater than.
	 */
	protected final Token 		GT
		= new Token (OPERATOR, ">");
	
	/**
	 * A <CODE>Token</CODE> representing greater than or equal.
	 */
	protected final Token 		GE
		= new Token (OPERATOR, ">=");

	/**
	 * A <CODE>Token</CODE> representing a left shift.
	 */
	protected final Token 		LSHIFT
		= new Token (OPERATOR, "<<");
	
	/**
	 * A <CODE>Token</CODE> representing a right shift.
	 */
	protected final Token 		RSHIFT
		= new Token (OPERATOR, ">>");

	/**
	 * A <CODE>Token</CODE> representing an opening parenthesis.
	 */
	protected final Token 		LPAREN
		= new Token (OPERATOR, "(");
	
	/**
	 * A <CODE>Token</CODE> representing a closing parenthesis.
	 */
	protected final Token 		RPAREN
		= new Token (OPERATOR, ")");

	/**
	 * A <CODE>Token</CODE> representing the LO function.
	 */
	protected final Token 		LO
		= new Token (KEYWORD, "LO");
	
	/**
	 * A <CODE>Token</CODE> representing the HI function.
	 */
	protected final Token 		HI
		= new Token (KEYWORD, "HI");
	
	/**
	 * A <CODE>Token</CODE> representing the BANK function.
	 */
	protected final Token 		BANK
		= new Token (KEYWORD, "BANK");
	
	/**
	 * An <CODE>Opcode</CODE> that handles .INCLUDE directives
	 */
	protected final Token 		INCLUDE	= new Opcode (KEYWORD, ".INCLUDE")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			token = nextRealToken ();
			if (token.getKind () == STRING) {
				String		filename = token.getText();
				FileReader	reader 	 = findFile (filename, true);
				
				if (reader != null)
					sources.push (new FileSource (filename, reader));
				else
					error (ERR_FAILED_TO_FIND_FILE);
			}
			else
				error (ERR_EXPECTED_QUOTED_FILENAME);
			
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .APPEND directives
	 */
	protected final Token		APPEND	= new Opcode (KEYWORD, ".APPEND")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (token.getKind () == STRING) {
				String		filename = token.getText();
				FileReader	reader 	 = findFile (filename, false);
				
				if (reader != null) {
					sources.pop ();
					sources.push (new FileSource (filename, reader));
				}
				else
					error (ERR_FAILED_TO_FIND_FILE);
			}
			else
				error (ERR_EXPECTED_QUOTED_FILENAME);
			
			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .INSERT directives
	 */
	protected final Token		INSERT	= new Opcode (KEYWORD, ".INSERT")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (token.getKind () == STRING) {
				String		filename = token.getText();
				FileReader	reader 	 = findFile (filename, false);
				
				if (reader != null) {
					BufferedReader	buffer = new BufferedReader (reader);
					
					try {
						for (int ch; (ch = buffer.read ()) != -1;)
							addByte (ch);
			
						buffer.close ();
					}
					catch (IOException error) {
						error (ERR_INSERT_IO_ERROR);
					}
				}
				else
					error (ERR_FAILED_TO_FIND_FILE);
			}
			else
				error (ERR_EXPECTED_QUOTED_FILENAME);
		
			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .END directives
	 */
	protected final Opcode 		END		= new Opcode (KEYWORD, ".END")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			sources.clear ();
			
			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .EQU directives
	 */
	protected final Opcode 		EQU		= new Opcode (KEYWORD, ".EQU")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			token = nextRealToken ();
			addr  = parseExpr ();
			
			if (pass == Pass.FIRST) {

				if (label.getText().charAt (0) != '.') {
					if (!variable.contains (label.getText ())) {
						if (!symbols.containsKey (label.getText ()))
							symbols.put (label.getText (), addr);
						else
							error (ERR_LABEL_REDEFINED);
					}
					else
						error ("Symbol has already been defined with .SET");
				}
				else
					error ("Equate symbols may not start with a '.'");
			}			
			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .SET directives
	 */
	protected final Opcode 		SET		= new Opcode (KEYWORD, ".SET")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			token = nextRealToken ();
			addr  = parseExpr ();

			if (label.getText().charAt (0) != '.') {
				doSet (label.getText(), addr);
			}	
			else
				error ("Variable symbols may not start with a '.'");

			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .SPACE directives
	 */
	protected final Token 		SPACE	= new Opcode (KEYWORD, ".SPACE")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			token = nextRealToken ();
			Expr expr = parseExpr ();
		
			if (expr.isAbsolute ()) {
				int		value = expr.resolve (null, null);
				
				for (int index = 0; index < value; ++index)
					addByte (0);
			}
			else
				error (ERR_CONSTANT_EXPR);
			
			return (true);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .BYTE directives
	 */
	protected final Opcode 		BYTE	= new Opcode (KEYWORD, ".BYTE")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			do {
				token = nextRealToken ();
				if (token.getKind () == STRING) {
					String value = token.getText();
					
					for (int index = 0; index < value.length (); ++index)
						addByte (value.charAt (index));
					
					token = nextRealToken ();
				}
				else {
					Expr expr = parseExpr ();
					
					if (expr != null)
						addByte (expr);
					else
						error (ERR_INVALID_EXPRESSION);
				}
			} while (token == COMMA);
			
			if (token != EOL) error (ERR_INVALID_EXPRESSION); 
			return (true);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .DBYTE directives
	 */
	protected final Opcode 		DBYTE	= new Opcode (KEYWORD, ".DBYTE")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			do {
				token = nextRealToken ();
				Expr expr = parseExpr ();
								
				if (expr != null) {
					addByte (Expr.and (Expr.shr (expr, EIGHT), MASK));
					addByte (Expr.and (expr, MASK));		
				}
				else
					error (ERR_INVALID_EXPRESSION);
			} while (token == COMMA);
			
			if (token != EOL) error (ERR_INVALID_EXPRESSION); 
			return (true);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .WORD directives
	 */
	protected final Opcode 		WORD	= new Opcode (KEYWORD, ".WORD")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			do {
				token = nextRealToken ();
				Expr expr = parseExpr ();
				
				if (expr != null)
					addWord (expr);		
				else
					error (ERR_INVALID_EXPRESSION);
			} while (token == COMMA);
			
			if (token != EOL) error (ERR_INVALID_EXPRESSION); 
			return (true);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .LONG directives
	 */
	protected final Opcode 		LONG	= new Opcode (KEYWORD, ".LONG")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			do {
				token = nextRealToken ();
				Expr expr = parseExpr ();
				
				if (expr != null)
					addLong (expr);
				else
					error (ERR_INVALID_EXPRESSION);
			} while (token == COMMA);
			
			if (token != EOL) error (ERR_INVALID_EXPRESSION); 
			return (true);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .IF directives
	 */
	protected final Opcode 		IF		= new Opcode (KEYWORD, ".IF", true)
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (isActive ()) {
				token = nextRealToken ();
				
				Expr	expr = parseExpr ();
			
				if (expr.isAbsolute ()) {
					boolean state = expr.resolve (null, null) != 0;
					status.push ((isActive () && state) ? Boolean.TRUE : Boolean.FALSE);
				}
				else
					error (ERR_CONSTANT_EXPR);
			}
			else
				status.push (Boolean.FALSE);
			
			return (false);	
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .IFABS directives
	 */
	protected final Opcode 		IFABS		= new Opcode (KEYWORD, ".IFABS", true)
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (isActive ()) {
				token = nextRealToken ();
				
				Expr	expr = parseExpr ();
			
				if (expr.isAbsolute ())
					status.push (Boolean.TRUE);
				else
					status.push (Boolean.FALSE);
			}
			else
				status.push (Boolean.FALSE);
			
			return (false);	
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .IFNABS directives
	 */
	protected final Opcode 		IFNABS		= new Opcode (KEYWORD, ".IFNABS", true)
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (isActive ()) {
				token = nextRealToken ();
				
				Expr	expr = parseExpr ();
			
				if (expr.isAbsolute ())
					status.push (Boolean.FALSE);
				else
					status.push (Boolean.TRUE);
			}
			else
				status.push (Boolean.FALSE);
			
			return (false);	
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .IFREL directives
	 */
	protected final Opcode 		IFREL		= new Opcode (KEYWORD, ".IFREL", true)
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (isActive ()) {
				token = nextRealToken ();
				
				Expr	expr = parseExpr ();
			
				if (expr.isRelative ())
					status.push (Boolean.TRUE);
				else
					status.push (Boolean.FALSE);
			}
			else
				status.push (Boolean.FALSE);
			
			return (false);	
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .IFNABS directives
	 */
	protected final Opcode 		IFNREL		= new Opcode (KEYWORD, ".IFNREL", true)
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (isActive ()) {
				token = nextRealToken ();
				
				Expr	expr = parseExpr ();
			
				if (expr.isRelative ())
					status.push (Boolean.FALSE);
				else
					status.push (Boolean.TRUE);
			}
			else
				status.push (Boolean.FALSE);
			
			return (false);	
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .IF directives
	 */
	protected final Opcode 		ELSE		= new Opcode (KEYWORD, ".ELSE", true)
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (!status.empty ()) {
				boolean	state = ((Boolean)(status.pop ())).booleanValue ();
				status.push ((isActive () && !state) ? Boolean.TRUE : Boolean.FALSE);
			}
			else
				error (ERR_NO_OPEN_IF);

			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .IF directives
	 */
	protected final Opcode 		ENDIF		= new Opcode (KEYWORD, ".ENDIF", true)
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (!status.empty ())
				status.pop ();
			else
				error (ERR_NO_OPEN_IF);

			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .MACROS directives
	 */
	protected final Token 		MACRO		= new Opcode (KEYWORD, ".MACRO")
	{
		public boolean compile ()
		{
			if ((macroName = label.getText ()) != null) {
				Vector		arguments	= new Vector ();
			
				for (;;) {
					if ((token = nextRealToken ()) == EOL) break;

					if ((token.getKind () == SYMBOL)||(token.getKind () == KEYWORD))
						arguments.add (token.getText ());
					else {
						error ("Illegal macro argument");
						break;
					}
					
					if ((token = nextRealToken ()) == EOL) break;
					
					if (token != COMMA) {					
						error ("Unexpected token after macro argument");
						break;
					}
				}
				
				savedLines = new MacroSource (arguments);
			}
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .ENDM directives
	 */
	protected final Token 		ENDM		= new Opcode (KEYWORD, ".ENDM")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			macros.put (macroName, savedLines);
			savedLines = null;
			
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .EXITM directives
	 */
	protected final Token 		EXITM		= new Opcode (KEYWORD, ".EXITM")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			while (sources.peek () instanceof MacroSource)
				sources.pop ();
			
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .REPEAT directives
	 */
	protected final Token 		REPEAT		= new Opcode (KEYWORD, ".REPEAT")
	{
		public boolean compile ()
		{
			token = nextRealToken ();
			Expr expr = parseExpr ();
			
			if (expr.isAbsolute ()) {
				savedLines = new RepeatSource (expr.resolve(null, null));
			}
			else
				error (ERR_CONSTANT_EXPR);
			
			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .ENDR directives
	 */
	protected final Token 		ENDR		= new Opcode (KEYWORD, ".ENDR")
	{
		public boolean compile ()
		{
			sources.push (savedLines);
			savedLines = null;
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .CODE directives
	 */
	protected final Opcode		CODE		= new Opcode (KEYWORD, ".CODE")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			section = code;
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .DATA directives
	 */
	protected final Opcode		DATA		= new Opcode (KEYWORD, ".DATA")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			section = data;
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .BSS directives
	 */
	protected final Opcode		BSS			= new Opcode (KEYWORD, ".BSS")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			section = bss;
			return (false);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .ORG directives
	 */
	protected final Opcode		ORG			= new Opcode (KEYWORD, ".ORG")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			token = nextRealToken ();
			Expr expr = parseExpr ();
			
			if (expr.isAbsolute ()) {
				section = section.setOrigin (expr.resolve (null, null));
			}
			else
				error (ERR_CONSTANT_EXPR);
			
			return (true);
		}
	};
	
	/**
	 * An <CODE>Opcode</CODE> that handles .EXTERN directives
	 */
	protected final Opcode		EXTERN		= new Opcode (KEYWORD, ".EXTERN")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (pass == Pass.FIRST) {
				do {
					token = nextRealToken ();
					if (token.getKind () != SYMBOL) {
						error ("Expected a list of symbols");
						return (false);
					}
					
					String name = token.getText ();
					externs.add (name);
					if (!symbols.containsKey (name))
						symbols.put (name, new Extern (name));
					token = nextRealToken ();
				} while (token == COMMA);
			}
			return (false);
		}
	};

	/**
	 * An <CODE>Opcode</CODE> that handles .GLOBAL directives
	 */
	protected final Opcode		GLOBAL		= new Opcode (KEYWORD, ".GLOBAL")
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean compile ()
		{
			if (pass == Pass.FIRST) {
				do {
					token = nextRealToken ();
					if (token.getKind () != SYMBOL) {
						error ("Expected a list of symbols");
						return (false);
					}
					
					String name = token.getText ();
					globals.add (name);

					token = nextRealToken ();
				} while (token == COMMA);
			}
			return (false);
		}
	};
	
	protected final Opcode		LIST		= new Opcode (KEYWORD, ".LIST")
	{
		/**
		 * {@inheritDoc
		 */
		public boolean compile ()
		{
			listing = true;
			return (false);
		}
	};
	
	protected final Opcode		NOLIST		= new Opcode (KEYWORD, ".NOLIST")
	{
		/**
		 * {@inheritDoc
		 */
		public boolean compile ()
		{
			listing = false;
			return (false);
		}
	};
	
	protected final Opcode		PAGE		= new Opcode (KEYWORD, ".PAGE")
	{
		/**
		 * {@inheritDoc
		 */
		public boolean compile ()
		{
			throwPage = true;
			return (false);
		}
	};
	
	protected final Opcode		TITLE		= new Opcode (KEYWORD, ".TITLE")
	{
		/**
		 * {@inheritDoc
		 */
		public boolean compile ()
		{
			token = nextRealToken ();
			
			title = token.getText ();
			return (false);
		}
	};
	
	/**
	 * The current <CODE>Token</CODE> under consideration.
	 */
	protected Token				token;
	
	/**
	 * The type of line we are compiling (for the listing).
	 */
	protected char				lineType;
	
	/**
	 * The address of the line.
	 */
	protected Expr				addr;
	
	/**
	 * The first 9 bytes of generated code.
	 */
	protected byte []			bytes		= new byte [9];
	
	/**
	 * The number of bytes generated so far.
	 */
	protected int				byteCount;
	
	/**
	 * Title string for listing output
	 */
	protected String			title;
	
	/**
	 * Constructs an <CODE>Assembler</CODE> that adds code to the given module.
	 * 
	 * @param 	module			The object module.
	 */
	protected Assembler (Module module)
	{
		this.module = module;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void startUp ()
	{
		super.startUp ();
		
		switch (getArguments ().length) {
		case 0:		System.err.println ("Error: No source file name provided");
					setFinished (true);
					break;
					
		case 1:		break;
		
		default:	System.err.println ("Error: Only one source file may be given");
					setFinished (true);
					break;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected String describeArguments ()
	{
		return (" <source file>");
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void execute ()
	{
		assemble (getArguments()[0]);
		setFinished (true);
	}
	
	/**
	 * {@inheritDoc} 
	 */
	protected void cleanUp ()
	{
		if (errors > 0) System.exit (1);
	}
	
	/**
	 * Determines if the <CODE>Assembler</CODE> supports the given pass.
	 * 
	 * @param 	pass			The assembler pass.
	 * @return	<CODE>true</CODE> if the pass is supported,
	 * 			<CODE>false</CODE> otherwise.
	 */
	protected abstract boolean isSupportedPass (final Pass pass);
	
	/**
	 * This method is called at the start of each pass to allow variables
	 * to be initialised.
	 */
	protected void startPass ()
	{
		listing   = true;
		title     = "";
		pageCount = 1;
		lineCount = 0;
		throwPage = false;
		
		code = module.findSection (".code");
		data = module.findSection (".data");
		bss  = module.findSection (".bss");
	}
	
	/**
	 * This method is called at the end of each pass to allow final
	 * actions to take place.
	 */
	protected void endPass ()
	{ }
	
	/**
	 * Formats the source line and generated code into a printable string.
	 * 
	 * @return	The string to add to the listing.
	 */
	protected abstract String formatListing ();

	/**
	 * Processes each line from the input source until the end of all
	 * files is reached.
	 */
	protected final void process ()
	{
		Line			line;
		
		while (!(sources.empty())) {
			if ((line = getNextLine ()) == null) {
				sources.pop ();
				continue;
			}
			process (line);
			
			if (pass == Pass.FINAL) {
				paginate (formatListing () + expandText ());
			}
		}
	}
	
	/**
	 * Output a line of text to the listing and 
	 * @param text
	 */
	protected final void paginate (final String text)
	{
		if ((listFile != null) && listing) {
			if (lineCount == 0) {
				listFile.println ();
				listFile.println (title);
				listFile.println ();
				
				lineCount += 3;
			}
			
			listFile.println (text);
			
			if ((throwPage) || (++lineCount == (linesPerPage - 3))) {
				listFile.print ('\f');
				++pageCount;
				lineCount = 0;
				throwPage = false;
			}
		}
	}
	
	/**
	 * Provides access to the output module.
	 * 
	 * @return	The current module.
	 */
	protected final Module getModule ()
	{
		return (module);
	}
	
	/**
	 * Provides access to the active label.
	 * 
	 * @return 	The label <CODE>Token</CODE> or <CODE>null</CODE>.
	 */
	protected final Token getLabel ()
	{
		return (label);
	}
	
	/**
	 * Fetches the next source line from the current source.
	 * 
	 * @return	The next source line.
	 */
	protected final Line getNextLine ()
	{
		if (sources.empty ())
			return (null);
		
		return (((Source) sources.peek ()).nextLine ());
	}
	
	protected final Section getSection ()
	{
		return (section);
	}
	
	/**
	 * Allows a derived class to modify the active section.
	 * 
	 * @param 	value			The section to activate.
	 */
	protected final void setSection (Section value)
	{
		section = value;
		
	}
	
	/**
	 * Executes the assembly process for the given file.
	 * 
	 * @param 	fileName		The name of the file to process.
	 * @return	<CODE>true</CODE> if assembly succeeded with no errors. 
	 */
	protected boolean assemble (final String fileName)
	{
		if (!assemble (Pass.FIRST, fileName)) return (false);
		if (!assemble (Pass.INTERMEDIATE, fileName)) return (false);
		if (!assemble (Pass.FINAL, fileName)) return (false);
	
		// Add globally define symbols to the object module. 
		for (Iterator cursor = globals.iterator (); cursor.hasNext();) {
			String name = (String) cursor.next ();
			Expr   expr = (Expr) symbols.get (name);
			
			if (expr != null)
				module.addGlobal (name, expr);
			else
				error ("Undefined global symbol: " + name);
		}
		
		// Write the object module
		if (errors == 0) {
			try {
				String objectName = getObjectFile (fileName);
				
				module.setName (new File (objectName).getName ());
				
				PrintStream		stream
					= new PrintStream (new FileOutputStream (objectName));
				
				stream.println ("<?xml version='1.0'?>" + module);
				stream.close ();
			}
			catch (Exception error) {
				System.err.println ("Error: Could not write object module");
				System.exit (1);
			}
		}
		
		// Dump symbol table
		if (lineCount != 0) {
			throwPage = true;
			paginate ("");
		}

		paginate ("Symbol Table");
		paginate ("");
		
		Object [] keys = symbols.keySet().toArray();
		Arrays.sort (keys);
		
		for (int index = 0; index < keys.length; ++index) {
			String	name  = (String) keys [index];
			Expr	expr  = (Expr) symbols.get (name);
			int		value = expr.resolve (null, null);
			
			name = (name + "                                ").substring (0, 32);
			
			if (expr.isAbsolute ())
				paginate (name + " " + Hex.toHex (value, 8));
			else
				paginate (name + " " + Hex.toHex (value, 8) + "'");
		}
		
		if (listFile != null) {
			listFile.close();
		}
		
		return (errors == 0);
	}

	/**
	 * Initialises the tokeniser to process the given line. This method is
	 * overloaded in derived classes.
	 * 
	 * @param 	line			The next <CODE>Line</CODE> to be processed.
	 */
	protected void process (final Line line)
	{
		if (sources.peek () instanceof TextSource)
			lineType = '+';
		else
			lineType = ' ';
		
		byteCount   = 0;
		label		= null;
		this.line   = line;
		this.text   = line.getText ().toCharArray ();
		this.offset = 0;
		
		addr = origin = (section != null) ? section.getOrigin () : null;
	
		if ((token = nextToken ()) == EOL) return;
		
		// Extract and save the labels
		if (token != WS) {
			label = token;
			if ((token = nextToken ()) == COLON)
				token = nextToken ();
		}
		
		if (token == WS) token = nextRealToken ();
		
		// Map = to .EQU when used as an opcode
		if (token == EQ)
			token = EQU;
		
		// Compile directives and opcodes
		if (token instanceof Opcode) {
			Opcode			opcode = (Opcode) token;
			
			if (opcode.isAlwaysActive () || isActive ()) {
				// If we are saving text then
				if (savedLines != null) {
					if (savedLines instanceof RepeatSource) {
						if (opcode == ENDR) {
							if (--repeatDepth == 0) {
								opcode.compile ();
								return;
							}
						}
						if (opcode == REPEAT)
							repeatDepth++;
					}
					if (savedLines instanceof MacroSource) {
						if (opcode == ENDM) {
							if (--macroDepth == 0) {
								opcode.compile ();
								return;
							}
						}
						if (opcode == MACRO)
							macroDepth++;
					}
					
					savedLines.addLine (line);
					return;
				}
								
				// Handle directives that cache source lines
				if (opcode == MACRO) {
					if (macroDepth++ == 0) {
						opcode.compile ();
						return;
					}
				}
				if (opcode == REPEAT) {
					if (repeatDepth++ == 0) {
						opcode.compile ();
						return;
					}
				}

				// Handle SET and EQU
				if ((opcode == EQU) || (opcode == SET)) {
					opcode.compile ();
					lineType = '=';
					return;
				}
	
				// Handle anything else
				if (opcode.compile ()) {
					if (byteCount > 0) {
						if (sources.peek () instanceof TextSource)
							lineType = '+';
						else
							lineType = ':';
					}
					
					if (label != null) {
						if (origin != null) {
							if (label.getText().charAt(0) == '.') {
								if (lastLabel != null)
									setLabel (lastLabel + label.getText(), origin);
								else
									error (ERR_NO_GLOBAL);
							}
							else {
								lastLabel = label.getText ();
								setLabel (lastLabel, origin);
							}
						}
						else
							error (ERR_NO_SECTION);

						if (lineType == ' ') lineType = ':';
					}
				}
				else
					if (label != null) warning (WRN_LABEL_IGNORED);
			}
			else
				lineType = '-';
			
			return;
		}

		// Are we saving lines for later?
		if (savedLines != null) {
			savedLines.addLine (line);
			return;
		}
		
		// Handle macro calls
		MacroSource source = (MacroSource) macros.get (token.getText ());
		if (source != null) {
			Vector			values = new Vector ();
			int				start;
			int				end;
			
			// Skip any leading whitespace
			do {
				start = offset;
				token = nextToken ();
			} while (token == WS);
			
			while (token != EOL) {
				do {
					end = offset;
					if ((token = nextRealToken ()) == EOL) break;
				} while (token != COMMA);
				
				values.add (new String (text, start, end - start));
				start = offset;
			}
			
			if (label != null) {
				if (origin != null) {
					if (label.getText().charAt(0) == '.') {
						if (lastLabel != null)
							setLabel (lastLabel + label.getText(), origin);
						else
							error (ERR_NO_GLOBAL);
					}
					else {
						lastLabel = label.getText ();
						setLabel (lastLabel, origin);
					}
				}
				else
					error (ERR_NO_SECTION);
			}

			sources.push (source.invoke (++instance, values));
			return;
		}
			
		// Handle label by itself
		if (label != null) {
			if (origin != null) {
				if (label.getText().charAt(0) == '.') {
					if (lastLabel != null)
						setLabel (lastLabel + label.getText(), origin);
					else
						error (ERR_NO_GLOBAL);
				}
				else {
					lastLabel = label.getText ();
					setLabel (lastLabel, origin);
				}
			}
			else
				error (ERR_NO_SECTION);
			
			if (lineType == ' ') lineType = ':';
		}
	
		error (ERR_UNKNOWN_OPCODE);
	}

	/**
	 * Returns the current pass.
	 * 
	 * @return	The current pass.
	 */
	protected final Pass getPass ()
	{
		return (pass);
	}
	
	/**
	 * Derives the name of the listing file from the source file.
	 * 
	 * @param 	fileName		The source file name
	 * @return	The name of the list file.
	 */
	protected String getListingFile (final String fileName)
	{
		return (fileName.substring (0, fileName.lastIndexOf ('.')) + ".lst");
	}
	
	/**
	 * Derives the name of the object module from the source file.
	 * 
	 * @param 	fileName		The source file name
	 * @return	The name of the corresponding object module.
	 */
	protected String getObjectFile (final String fileName)
	{
		return (fileName.substring (0, fileName.lastIndexOf ('.')) + ".obj");		
	}
	
	/**
	 * Determines if source lines are to be translated or skipped over
	 * depending on the current conditional compilation state.
	 * 
	 * @return <CODE>true</CODE> if source lines should be processed.
	 */
	protected boolean isActive ()
	{
		if (status.empty ())
			return (true);
		else
			return (((Boolean)(status.peek ())).booleanValue ());
	}
					
	/**
	 * Parses an expression.
	 */
	protected Expr parseExpr ()
	{
		try {
			return (parseLogical ());
		}
		catch (Exception error) {
			error ("Invalid expression");
		}
		return (ZERO);
	}

	/**
	 * Fetches the next <CODE>Token</CODE> skipping any pseudo whitespace.
	 * 
	 * @return	The next <CODE>Token</CODE> to be processed.
	 */
	protected final Token nextRealToken ()
	{
		Token			token = nextToken ();
		
		while (token == WS)
			token = nextToken ();
		
		return (token);
	}
	
	/**
	 * Fetches the next <CODE>Token</CODE> consuming any that have been
	 * pushed back first.
	 * 
	 * @return	The next <CODE>Token</CODE> to be processed.
	 */
	protected final Token nextToken ()
	{
		if (!(tokens.empty ()))
			return ((Token) tokens.pop ());
		
		return (readToken ());
	}
	
	/**
	 * Extracts a <CODE>Token</CODE> by processing characters from the
	 * source line.
	 * 
	 * @return	The next <CODE>Token</CODE> extracted from the source.
	 */
	protected abstract Token readToken ();
	
	/**
	 * Pushes a <CODE>Token</CODE> on the stack so that it can be reread.
	 * 
	 * @param 	token			The <CODE>Token</CODE> to be reprocessed.
	 */
	protected final void pushToken (final Token token)
	{
		tokens.push (token);
	}
	
	/**
	 * Gets and consumes the next character on the source line.
	 * 
	 * @return	The next character on the line.
	 */
	protected final char nextChar ()
	{
		char			ch = peekChar ();
		
		if (ch != '\0') ++offset;
		return (ch);
	}
	
	/**
	 * Gets the next character for the source line without consuming it.
	 * 
	 * @return	The next character on the line.
	 */
	protected final char peekChar ()
	{
		return ((offset < text.length) ? text [offset] : '\0');
	}

	/**
	 * Print an error message.
	 * 
	 * @param 	text			Then text for message.
	 */
	protected void error (final String text)
	{
		String		msg = "Error: " + line.getFileName() + " (" + line.getLineNumber() + ") " + text;
		
		System.err.println (msg);
		if (pass == Pass.FINAL)
			paginate (msg);
		
		++errors;
	}
	
	/**
	 * Print an warning message.
	 * 
	 * @param 	text			Then text for message.
	 */
	protected void warning (final String text)
	{
		String		msg = "Warning: " + line.getFileName() + " (" + line.getLineNumber() + ") " + text;

		System.err.println (msg);
		if (pass == Pass.FINAL)
			paginate (msg);
		
		++warnings;
	}

	/**
	 * Returns the current section origin,
	 * 
	 * @return	The origin for the current line.
	 */
	protected final Value getOrigin ()
	{
		return (origin);
	}
	
	/**
	 * Determines if a character is whitespace.
	 * 
	 * @param ch				The character to be tested.
	 * @return 	<CODE>true</CODE> if the character is whitespace,
	 * 			<CODE>false</CODE> otherwise.
	 */
	protected static boolean isSpace (char ch)
	{
		return ((ch == ' ') || (ch == '\t'));
	}
	
	/**
	 * Determines if a character is binary digit.
	 * 
	 * @param ch				The character to be tested.
	 * @return 	<CODE>true</CODE> if the character is a digit,
	 * 			<CODE>false</CODE> otherwise.
	 */
	protected static boolean isBinary (char ch)
	{
		return ((ch == '0') || (ch == '1'));
	}
	
	/**
	 * Determines if a character is an octal digit.
	 * 
	 * @param 	ch				The character to be tested.
	 * @return 	<CODE>true</CODE> if the character is a digit,
	 * 			<CODE>false</CODE> otherwise.
	 */
	protected static boolean isOctal (char ch)
	{
		return ((ch >= '0') && (ch <= '7'));
	}
	
	/**
	 * Determines if a character is a decimal digit.
	 * 
	 * @param 	ch				The character to be tested.
	 * @return 	<CODE>true</CODE> if the character is a digit,
	 * 			<CODE>false</CODE> otherwise.
	 */	
	protected static boolean isDecimal (char ch)
	{
		return ((ch >= '0') && (ch <= '9'));
	}
	
	/**
	 * Determines if a character is a hexadecimal digit.
	 * 
	 * @param 	ch				The character to be tested.
	 * @return 	<CODE>true</CODE> if the character is a digit,
	 * 			<CODE>false</CODE> otherwise.
	 */
	protected static boolean isHexadecimal (char ch)
	{
		return (((ch >= '0') && (ch <= '9')) ||
				((ch >= 'A') && (ch <= 'F')) ||
				((ch >= 'a') && (ch <= 'f')));
	}
	
	/**
	 * Determines if a character is a letter.
	 * 
	 * @param 	ch				The character to be tested.
	 * @return 	<CODE>true</CODE> if the character is a letter,
	 * 			<CODE>false</CODE> otherwise.
	 */
	protected static boolean isAlpha (char ch)
	{
		return (((ch >= 'A') && (ch <= 'Z')) ||
				((ch >= 'a') && (ch <= 'z')));
	}

	/**
	 * Determines if a character is alphanumeric.
	 * 
	 * @param 	ch				The character to be tested.
	 * @return 	<CODE>true</CODE> if the character is alphanumeric,
	 * 			<CODE>false</CODE> otherwise.
	 */
	protected static boolean isAlphanumeric (char ch)
	{
		return (((ch >= '0') && (ch <= '9')) ||
				((ch >= 'A') && (ch <= 'Z')) ||
				((ch >= 'a') && (ch <= 'z')));
	}
	
	/**
	 * Adds a byte to the code and captures it for the listing.
	 *  
	 * @param 	expr			The code value. 
	 */
	protected void addByte (final Expr expr)
	{
		if (expr.isRelative ()) {
			if (section != null) {
				section.addByte (expr);
				if (byteCount < bytes.length)
					bytes [byteCount++] = 0;
			}
			else
				error (ERR_NO_SECTION);
		}
		else
			addByte (expr.resolve (null, null));
	}
	
	/**
	 * Adds a word to the code and captures it for the listing.
	 *  
	 * @param 	expr			The code value. 
	 */
	protected void addWord (final Expr expr)
	{
		if (expr.isRelative ()) {
			if (section != null) {
				section.addWord (expr);
				if (byteCount < bytes.length)
					bytes [byteCount++] = 0;
				if (byteCount < bytes.length)
					bytes [byteCount++] = 0;
			}
			else
				error (ERR_NO_SECTION);
		}
		else
			addWord (expr.resolve (null, null));
	}
	
	/**
	 * Adds a long to the code and captures it for the listing.
	 *  
	 * @param 	expr			The code value. 
	 */
	protected void addLong (final Expr expr)
	{
		if (expr.isRelative ()) {
			if (section != null) {
				section.addLong (expr);
				if (byteCount < bytes.length)
					bytes [byteCount++] = 0;
				if (byteCount < bytes.length)
					bytes [byteCount++] = 0;
				if (byteCount < bytes.length)
					bytes [byteCount++] = 0;
				if (byteCount < bytes.length)
					bytes [byteCount++] = 0;
			}
			else
				error (ERR_NO_SECTION);
		}
		else
			addLong (expr.resolve (null, null));
	}
	
	/**
	 * Adds a byte to the code and captures it for the listing.
	 *  
	 * @param 	value			The code value. 
	 */
	protected void addByte (int value)
	{
		if (section != null) {
			section.addByte (value);
			if (byteCount < bytes.length)
				bytes [byteCount++] = (byte)(value & 0xff);
		}
		else
			error (ERR_NO_SECTION);
	}
	
	/**
	 * Adds a word to the code and captures it for the listing.
	 *  
	 * @param 	value			The code value. 
	 */
	protected void addWord (int value)
	{
		if (section != null) {
			section.addWord (value);
			if (module.isBigEndian()) {
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 8) & 0xff);
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 0) & 0xff);				
			}
			else {
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 0) & 0xff);
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 8) & 0xff);
			}
		}
		else
			error (ERR_NO_SECTION);
	}
	
	/**
	 * Adds a long to the code and captures it for the listing.
	 *  
	 * @param 	value			The code value. 
	 */
	protected void addLong (int value)
	{
		if (section != null) {
			section.addLong (value);
			if (module.isBigEndian()) {
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 24) & 0xff);
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 16) & 0xff);				
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 8) & 0xff);
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 0) & 0xff);				
			}
			else {
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 0) & 0xff);
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 8) & 0xff);
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 16) & 0xff);
				if (byteCount < bytes.length)
					bytes [byteCount++] = (byte)((value >> 24) & 0xff);				
			}
		}
		else
			error (ERR_NO_SECTION);
	}
	
	/**
	 * Sets the value of an symbol to the given expression value.
	 * 
	 * @param 	label			The symbol name.
	 * @param 	value			The associated value.
	 */
	protected void doSet (String label, Expr value)
	{
		if (symbols.containsKey (label)) {
			if (variable.contains (label))
				symbols.put (label, value);
			else
				error ("Symbol has already been defined.");
		}
		else {
			symbols.put(label, value);
			variable.add (label);
		}
	}
	
	/**
	 * Mask constant used to extract LSB.
	 */
	private static final Value	MASK	= new Value (null, 0xFF);
	
	/**
	 * Constant value used in shifts
	 */
	private static final Value	SIXTEEN	= new Value (null, 16);
	
	/**
	 * Constant value used in shifts.
	 */
	private static final Value	EIGHT	= new Value (null, 8);
	
	/*
	 * Constant value used here and there.
	 */
	private static final Value	ZERO	= new Value (null, 0);
	
	/**
	 * General string buffer area.
	 */
	private static StringBuffer	buffer	= new StringBuffer ();
	
	/**
	 * Tab expansion size.
	 */
	private int					tabSize	= 8;

	/**
	 * Flag determining listing on/off state
	 */
	private boolean				listing;
	
	/**
	 * The current output page number
	 */
	private int					pageCount;
	
	/**
	 * The current output line count
	 */
	private int					lineCount;
	
	/**
	 * The number of lines on a page (A4 = 60)
	 */
	private int					linesPerPage	= 60;
	
	/**
	 * Writer assigned to listing file in final pass.
	 */
	private PrintWriter			listFile 		= null;
	
	/**
	 * Indicates that a page should be throw after the next output line.
	 */
	private boolean				throwPage;
	
	/**
	 * The module being generated.
	 */
	private Module				module;

	/**
	 * The current sections.
	 */
	private Section				section;
	
	/**
	 * The default code section.
	 */
	private Section				code;
	
	/**
	 * The default data section.
	 */
	private Section				data;
	
	/**
	 * The default bss section.
	 */
	private Section				bss;
	
	/**
	 * The current pass.
	 */
	private Pass				pass;
	
	/**
	 * Holds the origin of the current instruction.
	 */
	private Value				origin;
	
	/**
	 * The last global label name.
	 */
	private String				lastLabel;
	
	/**
	 * The current label (if any).
	 */
	private Token				label;
	
	/**
	 * A <CODE>Stack</CODE> used to store the active code sources.
	 */
	private Stack				sources		= new Stack ();
	
	/**
	 * A <CODE>Stack</CODE> used to store previously processed tokens
	 */
	private Stack				tokens		= new Stack ();
	
	/**
	 * A <CODE>Stack</CODE> used record conditional status
	 */
	private Stack				status		= new Stack ();
	/**
	 * The current line being assembled.
	 */
	private Line				line;
	
	/**
	 * The characters comprising the line being assembled.
	 */
	private char []				text;
	
	/**
	 * The offset of the next character in the current line.
	 */
	private int					offset;
	
	/**
	 * The number of errors seen during the current pass.
	 */
	private int					errors;
	
	/**
	 * The number of warnings seen during the current pass.
	 */
	private int					warnings;
	
	/**
	 * The set of all symbols defined in this module.
	 */
	private Hashtable			symbols		= new Hashtable ();
	
	/**
	 * The subset of symbols that may be redefined.
	 */
	private HashSet				variable	= new HashSet ();
	
	/**
	 * The set of symbols which will be exported.
	 */
	private HashSet				globals		= new HashSet ();
	
	/**
	 * The set of symbol which have been imported.
	 */
	private HashSet				externs		= new HashSet ();
	
	/**
	 * The set of defined macros.
	 */
	private Hashtable			macros		= new Hashtable ();
	
	/**
	 * The name of the current macro
	 */
	private String				macroName	= null;
	
	/**
	 * The <CODE>TextSource</CODE> used to capture macro or repeat lines. 
	 */
	private TextSource			savedLines	= null;
	
	/**
	 * Count macro definition depth.
	 */
	private int					macroDepth	= 0;
	
	/**
	 * Counts repeat section depth.
	 */
	private int					repeatDepth = 0;
	
	/**
	 * Macro instance counter.
	 */
	private int					instance    = 0;
	
	/**
	 * Configures the source stack to read from the given file and initiates
	 * the processing for the given pass.
	 * 
	 * @param 	pass			The assembler pass.
	 * @param 	fileName		The initial source filename
	 * @return 	<CODE>true</CODE> if no errors were found during the pass.
	 */
	private boolean assemble (final Pass pass, final String fileName)
	{
		if (!isSupportedPass (this.pass = pass)) return (true);
		
		startPass ();
		
		module.clear ();
		
		errors 	  	= 0;
		warnings  	= 0;
		section	  	= code;
		lastLabel 	= null;
		
		savedLines 	= null;
		repeatDepth	= 0;
		macroDepth	= 0;
		instance    = 0;
		
		try {
			if (pass == Pass.FINAL) {
				listFile = new PrintWriter (new FileWriter (getListingFile (fileName)));
			}
			
			sources.push (new FileSource (fileName, new FileReader (fileName)));
			process ();
		}
		catch (FileNotFoundException error) {
			error ("Source file not found");
		}
		catch (IOException error) {
			error ("Could not create listing file");
		}
		
		endPass ();
		
		return (errors == 0);
	}

	private void setLabel (final String name, Value value)
	{
		if ((pass == Pass.FIRST) && symbols.containsKey (name))
			error (ERR_LABEL_REDEFINED + name);
		else
			symbols.put (name, value);
	}
	
	/**
	 * Parse an logical expression.
	 * <PRE>{logical} := {binary} [ ( '&amp;&amp;' | '||' ) {binary} ]*</PRE>
	 * 
	 * @return	A compiled expression.
	 */
	private Expr parseLogical ()
	{
		Expr		expr = parseBinary ();
	
		while ((token == LOGICALAND) || (token == LOGICALOR)) {
			if (token == LOGICALAND) {
				token = nextRealToken ();
				expr = Expr.land (expr, parseBinary ());
			}
			else {
				token = nextRealToken ();
				expr = Expr.lor (expr, parseBinary ());
			}
		}
		return (expr);
	}
	
	/**
	 * Parse a binary expression.
	 * <PRE>{binary} := {equal} [ ( '&amp;' | '|' | '^' ) {equal} ]*</PRE>
	 * 
	 * @return	A compiled expression.
	 */
	private Expr parseBinary ()
	{
		Expr		expr = parseEquality ();
		
		while ((token == BINARYAND) || (token == BINARYOR) || (token == BINARYXOR)) {
			if (token == BINARYAND) {
				token = nextRealToken ();
				expr = Expr.and (expr, parseEquality ());
			}
			else if (token == BINARYOR) {
				token = nextRealToken ();
				expr = Expr.or (expr, parseEquality ());
			}
			else {
				token = nextRealToken ();
				expr = Expr.xor (expr, parseEquality ());
			}
		}
		return (expr);
	}
	
	/**
	 * Parse an equality expression.
	 * <PRE>{equal} := {ineq} [ ( '==' | '!=' ) {ineq} ]*</PRE>
	 * 
	 * @return	A compiled expression.
	 */
	private Expr parseEquality ()
	{
		Expr		expr = parseInequality ();
		
		while ((token == EQ) || (token == NE)) {
			if (token == EQ) {
				token = nextRealToken ();
				expr = Expr.eq (expr, parseEquality ());
			}
			else {
				token = nextRealToken ();
				expr = Expr.ne (expr, parseEquality ());
			}
		}
		return (expr);
	}
	
	/**
	 * Parse an inequality expression.
	 * <PRE>{ineq} := {shift} [ ( '&lt;' | '&lt;=' | '&gt;' | '&gt;=' ) {shift} ]*</PRE>
	 * 
	 * @return	A compiled expression.
	 */
	private Expr parseInequality ()
	{
		Expr 		expr = parseShift ();
		
		while ((token == LT) || (token == LE) || (token == GT) || (token == GE)) {
			if (token == LT) {
				token = nextRealToken ();
				expr = Expr.lt (expr, parseShift ());
			}
			else if (token == LE) {
				token = nextRealToken ();
				expr = Expr.le (expr, parseShift ());
			}
			else if (token == GT) {
				token = nextRealToken ();
				expr = Expr.gt (expr, parseShift ());
			}
			else {
				token = nextRealToken ();
				expr = Expr.ge (expr, parseShift ());
			}
		}
		return (expr);
	}
	
	/**
	 * Parse a shift expression.
	 * <PRE>{shift} := {addsub} [ ( '<<' | '>>' ) {addsub} ]*</PRE>
	 * 
	 * @return	A compiled expression.
	 */
	private Expr parseShift ()
	{
		Expr		expr = parseAddSub ();
		
		while ((token == RSHIFT) || (token == LSHIFT)) {
			if (token == RSHIFT) {
				token = nextRealToken ();
				expr = Expr.shr (expr, parseAddSub ());
			}
			else {
				token = nextRealToken ();
				expr = Expr.shl (expr, parseAddSub ());
			}
		}
		return (expr);		
	}
	
	/**
	 * Parse an additive expression.
	 * <PRE>{addsub} := {muldiv} [ ( '+' | '-' ) {muldiv} ]*</PRE>
	 * 
	 * @return	A compiled expression.
	 */
	private Expr parseAddSub ()
	{
		Expr		expr = parseMulDiv ();
		
		while ((token == PLUS) || (token == MINUS)) {
			if (token == PLUS) {
				token = nextRealToken ();
				expr = Expr.add (expr, parseMulDiv ());
			}
			else {
				token = nextRealToken ();
				expr = Expr.sub (expr, parseMulDiv ());
			}
		}
		return (expr);
	}
	
	/**
	 * Parse a multiplicative expression.
	 * <PRE>{muldiv} := {unary} [ ( '*' | '/' | '%' ) {unary} ]*</PRE>
	 * 
	 * @return	A compiled expression.
	 */
	private Expr parseMulDiv ()
	{
		Expr		expr = parseUnary ();
		
		while ((token == TIMES) || (token == DIVIDE) || (token == MODULO)) {
			if (token == TIMES) {
				token = nextRealToken ();
				expr = Expr.mul (expr, parseUnary ());
			}
			else if (token == DIVIDE) {
				token = nextRealToken ();
				expr = Expr.div (expr, parseUnary ());
			}
			else {
				token = nextRealToken ();
				expr = Expr.mod (expr, parseUnary ());
			}
		}
		return (expr);
	}
	
	/**
	 * Parse a unary expression
	 * <PRE>{unary} := [ '-' | '+' | '~' | '!' | 'LO' | 'HI' | 'BANK' ] {value}</PRE>
	 * 
	 * @return	A compiled expression.	
	 */
	private Expr parseUnary ()
	{
		if (token == MINUS) {
			token = nextRealToken ();
			return (Expr.neg (parseValue ()));
		}
		else if (token == PLUS) {
			token = nextRealToken ();
			return (parseExpr ());
		}
		else if (token == COMPLEMENT) {
			token = nextRealToken ();
			return (Expr.cpl (parseValue ()));			
		}
		else if (token == LOGICALNOT) {
			token = nextRealToken ();
			return (Expr.lnot (parseValue ()));			
		}
		else if (token == LO) {
			token = nextRealToken ();
			return (Expr.and (parseValue (), MASK));
		}
		else if (token == HI) {
			token = nextRealToken ();
			return (Expr.and (Expr.shr (parseValue (), EIGHT), MASK));
		}
		else if (token == BANK) {
			token = nextRealToken ();
			return (Expr.shr (parseValue (), SIXTEEN));
		}
		
		return (parseValue ());
	}
	
	/**
	 * Parse part of an expression that should result in a value.
	 * <PRE>{value} := {origin} | '(' {expr} ')' | {number} | {symbol}</PRE>
	 *  
	 * @return	A compiled expression.
	 */
	private Expr parseValue ()
	{
		Expr		expr = null;
		
		if ((token == ORIGIN) || (token == TIMES)) {
			expr = origin;
			token = nextRealToken ();
		}
		else if (token == LPAREN) {
			token = nextRealToken ();
			expr = parseExpr ();
			if (token != RPAREN)
				error (ERR_CLOSING_PAREN);
			else
				token = nextRealToken ();
		}
		else if (token.getKind () == NUMBER) {
			expr = new Value (null, ((Integer)(token.getValue ())).intValue ());
			token = nextRealToken ();
		}
		else if (token.getKind () == SYMBOL) {
			if (token.getText ().charAt (0) == '.') {
				if (lastLabel != null)
					expr = (Expr) symbols.get (lastLabel + token.getText ());
				else
					error (ERR_NO_GLOBAL);
			}
			else
				expr = (Expr) symbols.get (token.getText ());
			
			if (expr == null) {
				if (pass == Pass.FINAL)
					error (ERR_UNDEF_SYMBOL + token.getText ());
				expr = ZERO;
			}
			
			token = nextRealToken ();
		}
	
		return (expr);
	}
	
	/**
	 * Creates a string from the current source line with tabs expanded into
	 * spaces.
	 * 
	 * @return	The expanded source text line.
	 */
	private String expandText ()
	{
		buffer.setLength (0);
		for (int index = 0; index < text.length; ++index) {
			if (text [index] == '\t') {
				do {
					buffer.append (' ');
				} while (buffer.length () % tabSize != 0);
			}
			else
				buffer.append (text [index]);
		}
		return (buffer.toString ());
	}
	
	/**
	 * Locates a file with the given name, optionally searching the include
	 * path for it.
	 * 
	 * @param 	filename		The required filename.
	 * @param 	search			The search indicator.
	 * @return	A <CODE>FileReader</CODE> attached to the file or <CODE>null</CODE>.
	 */
	private FileReader findFile (final String filename, boolean search)
	{
		FileReader		reader	= null;
		
		try {
			reader = new FileReader (filename);
		}
		catch (FileNotFoundException error) {
			if (search) {
				; // TODO Implement search
			}
			error ("Could not find the specified file");
		}
		return (reader);
	}
}