import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

public class Precompiler
{
	private static final String PRINT = "System.out.print";
	private static final String PRINTLN = "System.out.println";
	private static final String READINT = "scanner.nextInt";
	private static final String READDOUBLE = "scanner.nextDouble";
	private static final String READLINE = "scanner.nextLine";
	private static final String LINESEPARATOR=System.lineSeparator();
	
	public DecafFile convert(DecafFile file) throws Exception
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
				text+=line+LINESEPARATOR;
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
			throw new Exception("file "+file.getAbsoluteFile()+" cannot be found");
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new Exception("read error in file "+file.getAbsoluteFile());
		}

		DocumentTokenizer tokenizer=new DocumentTokenizer(text);

		boolean inClass=false;
		int classes=0;
		int braces=0;

		while(tokenizer.hasMoreTokens())
		{
			Token token=tokenizer.nextToken();

			switch(token.type)
			{
			case TokenTypes.WHITESPACE:
				break;
			case TokenTypes.NULL:
				break;
			case TokenTypes.RESERVED_WORD:
				if(token.getLexeme().equals("class"))
				{
					if(inClass==true)
					{
						throw new Exception("class nesting is not allowed ");
					}

					classes++;
					inClass=true;
				}
			case TokenTypes.FUNCTION:
				if(token.getLexeme().equals("print"))
				{
					Token next=token.getNextToken();
					token.set(PRINT.toCharArray(), 0, PRINT.length()-1, 0, token.type);
					token.setNextToken(next);
				}
				else if(token.getLexeme().equals("println"))
				{
					Token next=token.getNextToken();
					token.set(PRINTLN.toCharArray(), 0, PRINTLN.length()-1, 0, token.type);
					token.setNextToken(next);
				}			
			case TokenTypes.IDENTIFIER:
				if(token.getLexeme().equals("readInt"))
				{
					Token next=token.getNextToken();
					token.set(READINT.toCharArray(), 0, READINT.length()-1, 0, token.type);
					token.setNextToken(next);
				}
				else if(token.getLexeme().equals("readDouble"))
				{
					Token next=token.getNextToken();
					token.set(READDOUBLE.toCharArray(), 0, READDOUBLE.length()-1, 0, token.type);
					token.setNextToken(next);
				}
				else if(token.getLexeme().equals("readLine"))
				{
					Token next=token.getNextToken();
					token.set(READLINE.toCharArray(), 0, READLINE.length()-1, 0, token.type);
					token.setNextToken(next);
				}
			case TokenTypes.DATA_TYPE:
			case TokenTypes.SEPARATOR:
				if(token.getLexeme().equals("{"))
				{
					if(inClass==true)
					{
						braces++;
					}
				}
				else if(token.getLexeme().equals("}"))
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
					throw new Exception("code is not allowed after class definitions ");
				}
				break;
			default:
			}
		}

		String fileName=file.getName();
		final int lastPeriodPos = fileName.lastIndexOf('.');

		fileName=fileName.substring(0, lastPeriodPos);

		text=insertBoilerplateCode(fileName, tokenizer.stringifyPlainCode());
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
			throw new Exception("write error in file "+file.getAbsoluteFile());
		}

		return file;		
	}
		
	private String insertBoilerplateCode(String fileName, String text)
	{
		String code="";
		code+="import java.util.Scanner;"+LINESEPARATOR;
		code+="public class "+fileName+LINESEPARATOR;
		code+="{"+LINESEPARATOR;
		code+="	public static void main(String[] args)"+LINESEPARATOR;
		code+="	{"+LINESEPARATOR;
		code+="		Scanner scanner=new Scanner(System.in);"+LINESEPARATOR;
		code+=text;
		code+="	}"+LINESEPARATOR;
		code+="}"+LINESEPARATOR;
		return code;
	}
}
