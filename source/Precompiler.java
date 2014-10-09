import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

public class Precompiler
{
	private static final String PRINT = "System.out.print";
	private static final String PRINTLN = "System.out.println";
	private static final String READINT = "scanner.nextInt";
	private static final String READDOUBLE = "scanner.nextDouble";
	private static final String READLINE = "scanner.next";
	private static final String READER = "private static Scanner scanner=new Scanner(System.in);";
	private static final String DELIMITER = " static{scanner.useDelimiter(System.getProperty(\"line.separator\"));}";
	private static final String SCANNER = READER+DELIMITER;
	private static final String LINE_SEPARATOR=System.lineSeparator();
	private static final String HEADER1 = "import java.util.Scanner;"+LINE_SEPARATOR+
									"public class ";
	private static final String HEADER2 = LINE_SEPARATOR+
									"{"+LINE_SEPARATOR +
									"		"+SCANNER+LINE_SEPARATOR +
									"	public static void main(String[] args)"+LINE_SEPARATOR +
									"	{"+LINE_SEPARATOR;

	
	public DecafFile convert(DecafFile file) throws IOException
	{
		String line="";
		String text="";
		int lines=0;
		
		BufferedReader r = null;
		
		try
		{
			r=new BufferedReader(new FileReader(file));

			boolean mainCode=true;
			
			while ((line = r.readLine()) != null)
			{
				text+=line+LINE_SEPARATOR;
				mainCode=mainCode&&line.indexOf("class")==-1;
				
				if(mainCode==true)
				{
					lines++;
				}
			}
			
			r.close();
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
			throw new FileNotFoundException("File "+file.getAbsoluteFile()+" cannot be found");
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new IOException("Read error in file "+file.getAbsoluteFile());
		}

		DocumentTokenizer tokenizer=new DocumentTokenizer(text);
		
		boolean inClass=false;
		int classes=0;
		int braces=0;

		while(tokenizer.hasMoreTokens())
		{
			if(isMethod(tokenizer)==true)
			{
				if(classes!=0)
				{
					if(inClass==false)
					{
						throw new JavaDecafException("Stand-alone methods are not allowed after class definitions");
					}
				}
				else
				{
					processMethod(tokenizer);
				}
			}
		
			CodeToken token=tokenizer.nextToken();

			switch(token.token.type)
			{
			case TokenTypes.DATA_TYPE:
				// FIXME: change primitive types to boxed types so we can change == for .equals()
				String type = token.token.getLexeme();
				if (type.equals("int")) {
					substituteToken(tokenizer, token, "Integer");
				} else if (type.equals("long")) {
					substituteToken(tokenizer, token, "Long");
				} else if (type.equals("double")) {
					substituteToken(tokenizer, token, "Double");
				} else if (type.equals("boolean")) {
					substituteToken(tokenizer, token, "Boolean");
				} else if (type.equals("char")) {
					substituteToken(tokenizer, token, "Character");
				} else if (type.equals("byte")) {
					substituteToken(tokenizer, token, "Byte");
				} else if (type.equals("short")) {
					substituteToken(tokenizer, token, "Short");
				} else if (type.equals("float")) {
					substituteToken(tokenizer, token, "Float");
				} else {
					throw new JavaDecafException("Unsupported primitive type: " + type);
 				}
				break;
			case TokenTypes.FUNCTION:
				if(token.token.getLexeme().equals("print"))
				{
					substituteToken(tokenizer, token, PRINT);
				}
				else if(token.token.getLexeme().equals("println"))
				{
					substituteToken(tokenizer, token, PRINTLN);
				}		
				break;
			case TokenTypes.IDENTIFIER:
				if(token.token.getLexeme().equals("readInt"))
				{
					substituteToken(tokenizer, token, READINT);
				}
				else if(token.token.getLexeme().equals("readDouble"))
				{
					substituteToken(tokenizer, token, READDOUBLE);
				}
				else if(token.token.getLexeme().equals("readLine"))
				{
					substituteToken(tokenizer, token, READLINE);
				}
			case TokenTypes.NULL:
				break;
			case TokenTypes.OPERATOR:
				if(token.token.getLexeme().equals("==")) {
					// FIXME: here something must be done to transform == into .equals()	
				}
				break;
			case TokenTypes.RESERVED_WORD:
				if(token.token.getLexeme().equals("class"))
				{
					if(inClass==true)
					{
						throw new JavaDecafException("Class nesting is not allowed ");
					}

					classes++;
					inClass=true;
				}
				break;
			case TokenTypes.SEPARATOR:
				if(token.token.getLexeme().equals("{"))
				{
					if(inClass==true)
					{
						if(braces==0)
						{
							Token next=token.token.getNextToken();
							token.token.set(("{"+SCANNER).toCharArray(), 0, ("{"+SCANNER).length()-1, 0, token.token.type);
							token.token.setNextToken(next);
						}
						
						braces++;
					}
				}
				else if(token.token.getLexeme().equals("}"))
				{
					if(inClass==true)
					{
						braces--;

						if(braces==0)
						{
							inClass=false;
							break;
						}
					}
				}

				if(classes>0 && inClass==false)
				{
					throw new JavaDecafException("Code is not allowed after class definitions ");
				}
				break;
			case TokenTypes.WHITESPACE:
				break;
			default:
			}
		}

		String fileName=file.getName();
		final int lastPeriodPos = fileName.lastIndexOf('.');

		fileName=fileName.substring(0, lastPeriodPos);
		
		text=insertBoilerplateCode(fileName, tokenizer.stringifyMainCode());
		text+="\r\n";
		text+=tokenizer.stringifyClasses();

		String filePath=fileName+".java";
		file=new DecafFile(filePath);
		file.properties.containsClasses = classes>0;
		file.properties.lines=lines;
		
		BufferedWriter w = null;
		
		try
		{
			w=new BufferedWriter(new FileWriter(file));
			w.write(text);
			w.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new IOException("write error in file "+file.getAbsoluteFile());
		}

		return file;		
	}

	/**
	 * Substitutes the given token by another token containing the given string. 
	 * @param tokenizer the tokenizer (containing the current position)
	 * @param token the token to be substituted in the tokenizer
	 * @param newToken the text for the new token
	 */
	private void substituteToken(DocumentTokenizer tokenizer, CodeToken token, String newToken) {
		CodeToken previous=tokenizer.peekToken(tokenizer.getCurrentPosition()-2);	
		if(previous==null||previous.token.isPaintable()==false||previous.token.getLexeme().equals(".")==false)
		{					
			Token next=token.token.getNextToken();
			token.token.set(newToken.toCharArray(), 0, newToken.length()-1, 0, token.token.type);
			token.token.setNextToken(next);
		}
	}

	
	private void processMethod(DocumentTokenizer tokenizer)
	{
		int position=tokenizer.getCurrentPosition();

		CodeToken token=null;
		
		do
		{
			token=tokenizer.peekToken(position++);
			if(token==null)
			{
				throw new JavaDecafException("Stand-alone method not started properly");
			}
		} while(token.token.type==TokenTypes.WHITESPACE);
		
		token.isMethodStart=true;

		do
		{
			token=tokenizer.peekToken(position++);
			if(token==null)
			{
				throw new JavaDecafException("Stand-alone method not started properly");
			}
		} while(token.token.isLeftCurly()==false);
		
		/*
		 * Not sure the rest of this method does anything at all (SG 2014-10-09)
		 */
		int braces=1;

		boolean reachedEndOfMethod=false;
		while(reachedEndOfMethod==false)
		{
			token=tokenizer.peekToken(position++);
			if(token==null)
			{
				throw new JavaDecafException("Stand-alone method not terminated properly");
			}
			else if(token.token.isLeftCurly()==true)
			{
				braces++;
			}
			else if(token.token.isRightCurly()==true)
			{
				braces--;
			}
			if(braces==0)
			{
				reachedEndOfMethod=true;
			}
		}
		
		token.isMethodStop=true;
	}
	
	private boolean isMethod(DocumentTokenizer tokenizer)
	{
		int position=tokenizer.getCurrentPosition();

		CodeToken token=null;
		
		do
		{
			token=tokenizer.peekToken(position++);
			
			if(token==null)
			{
				return false;
			}
			
		} while(token.token.type==TokenTypes.WHITESPACE);

		if(token.token.type!=TokenTypes.DATA_TYPE&&token.token.type!=TokenTypes.IDENTIFIER)
		{
			if(token.token.type==TokenTypes.RESERVED_WORD)
			{
				if(token.token.getLexeme().equals("void")==false)
				{
					return false;
				}
			}
			else if(token.token.type==TokenTypes.FUNCTION) 
			{
				String[] JAVA_LANG_CLASSES = {"Boolean","Byte","Character","Class","ClassLoader","ClassValue","Compiler","Double","Enum","Float","InheritableThreadLocal","Integer","Long","Math","Number","Object","Package","Process","ProcessBuilder","Runtime","RuntimePermission","SecurityManager","Short","StackTraceElement","StrictMath","String","StringBuffer","StringBuilder","System","Thread","ThreadGroup","Throwable","Void"};
				if (!Arrays.asList(JAVA_LANG_CLASSES).contains(token.token.getLexeme())) {
					return false;
				}
			}
			else
			{
				return false;
			}
		}
		else
		{
			if(token.token.getLexeme().equals(";")||token.token.getLexeme().equals("."))
			{
				return false;
			}
			
			token=tokenizer.peekToken(position++);

			if(token==null||token.token.type==TokenTypes.NULL)
			{
				return false;
			}
			
			if(token.token.type!=TokenTypes.WHITESPACE)
			{
				if(token.token.type!=TokenTypes.SEPARATOR&&token.token.getLexeme().equals("[")==false)
				{
					return false;
				}

				token=tokenizer.peekToken(position++);

				if(token==null||token.token.type==TokenTypes.NULL)
				{
					return false;
				}
				
				if(token.token.type!=TokenTypes.WHITESPACE)
				{
					if(token.token.type!=TokenTypes.SEPARATOR&&token.token.getLexeme().equals("]")==false)
					{
						return false;
					}
				}
			}
		}
		
		//Token dataType=token;
		
		do
		{
			token=tokenizer.peekToken(position++);

			if(token==null)
			{
				return false;
			}
			
		} while(token.token.type==TokenTypes.WHITESPACE);
	
		
		if(token.token.type!=TokenTypes.IDENTIFIER)
		{
			return false;
		}

		if(token.token.getLexeme().equals(";")||token.token.getLexeme().equals("."))
		{
			return false;
		}
		
		//Token identifier=token;
		
		do
		{
			token=tokenizer.peekToken(position++);
			
			if(token==null)
			{
				return false;
			}

		} while(token.token.type!=TokenTypes.SEPARATOR&&token.token.type==TokenTypes.WHITESPACE);

		if(token.token.type!=TokenTypes.SEPARATOR||!token.token.getLexeme().equals("("))
		{
			return false;
		}

		//Token separator=token;
		
		return true;
	}

	private String insertBoilerplateCode(String fileName, String[] text)
	{
		String code=HEADER1+fileName+HEADER2;
		code+=text[0];
		code+="}"+text[1];
		code+="}"+LINE_SEPARATOR;
		return code;
	}
	
	/**
	 * Returns the number of lines in the header.
	 * 
	 * Basically, just counts the number of CR/LF. 
	 * 
	 * @return the number of lines in the header.
	 */
	public static int getHeaderLineCount() {
		int result = 0;
		String header = HEADER1 + HEADER2;
		int lineSeparatorLength = LINE_SEPARATOR.length();
		int lastIndex = header.length() - lineSeparatorLength + 1;
		for (int i = 0; i < lastIndex; ++i) {
			if (LINE_SEPARATOR.equals(header.substring(i, i + lineSeparatorLength))) {
				result++;
			}
		}
		return result;
	}
}

class JavaDecafException extends RuntimeException {
	private static final long serialVersionUID = 5535341L;
	
	public JavaDecafException(String s) {
		super(s);
	}
}
