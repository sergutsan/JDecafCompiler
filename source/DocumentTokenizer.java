import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.GroovyTokenMaker;

public class DocumentTokenizer
{
	private ArrayList<CodeToken> tokens=new ArrayList<CodeToken>();
	private int position;

	public DocumentTokenizer(String text)
	{
		init(text);
		position=0;
	}
	
	public String[] stringifyMainCode()
	{
		int position=0;
		boolean stop=false;
		boolean method=false;
		boolean methodsStarted=false;
		String mainCode="";
		String methods="";
		int noOfMethods=0;
		String[] code=new String[2];
		
		while(position<tokens.size()&&stop==false)
		{
			CodeToken token=tokens.get(position++);
			
			String nextLexeme="";
			
			if(token.token.type==TokenTypes.NULL)
			{
				nextLexeme="\n";
			}
			else
			{
				String lexeme=token.token.getLexeme();
				
				if(lexeme.equals("class"))
				{
					stop=true;
				}
				else
				{
					nextLexeme=lexeme;
				}
			}

			if(token.isMethodStart==true)
			{
				if(methodsStarted==false)
				{
					methodsStarted=true;
				}
				
				methods+=" private static ";
				method=true;
				noOfMethods++;
			}
			
			if(methodsStarted==true)
			{
				methods+=nextLexeme;
				
				if(token.isMethodStop==true)
				{
					method=false;
				}
			}
			else
			{
				mainCode+=nextLexeme;
			}	
		
			//System.out.println(nextLexeme);
		}
		
		code[0]=mainCode;
		code[1]=methods;

		return code;
	}

	public String stringifyClasses()
	{
		int position=0;
		boolean start=false;
		boolean stop=false;
		String text="";
		
		while(position<tokens.size()&&stop==false)
		{
			CodeToken token=tokens.get(position++);
			
			if(token.token.type==TokenTypes.NULL)
			{
				if(start==true)
				{
					text+='\n';
				}
			}
			else
			{
				String lexeme=token.token.getLexeme();
				
				if(lexeme.equals("class"))
				{
					start=true;
				}
				
				if(start==true)
				{
					text+=lexeme;
				}
			}
		}

		return text;
	}
	
	public String stringify()
	{
		Iterator<CodeToken> iterator=tokens.iterator();
		String text="";
		
		while(iterator.hasNext())
		{
			CodeToken token=iterator.next();
			
			if(token.token.type==TokenTypes.NULL)
			{
				text+="\r\n";
			}
			else
			{
				text+=token.token.getLexeme();
			}
		}
		
		return text;
	}

	public String stringifyFull()
	{
		Iterator<CodeToken> iterator=tokens.iterator();
		String text="";
		
		while(iterator.hasNext())
		{
			text+=iterator.next().token.toString()+"\n";
		}
		
		return text;
	}

	public String toString()
	{
		String text="";
		text+="Object: DocumentTokenizer\r\n";
		text+="No of tokens: "+countTokens()+"\r\n";
		text+="Current position: "+position+"\r\n";
		text+="-------------------------------\r\n";
		text+=stringifyFull();

		return text;
	}
	
	public void reset()
	{
		position=0;
	}
	
	public int countTokens()
	{
		return tokens.size();
	}
	
	public CodeToken nextToken()
	{
		if(hasMoreTokens()==false)
		{
			return null;
		}
		
		return tokens.get(position++);
	}

	public CodeToken peekToken(int position)
	{
		if(position>=tokens.size()||position<0)
		{
			return null;
		}
		
		return tokens.get(position);
	}
	
	public int getCurrentPosition()
	{
		return position;
	}
	
	public boolean hasMoreTokens()
	{
		return position<tokens.size();
	}
	
	private void init(String text)
	{
		Scanner scanner=null;
		
		try
		{
			scanner=new Scanner(text);
			
			while(scanner.hasNextLine())
			{
				String line=scanner.nextLine();
				Segment segment=new Segment(line.toCharArray(),0,line.length());
				GroovyTokenMaker tokenMaker=new GroovyTokenMaker();
				Token token=tokenMaker.getTokenList(segment,TokenTypes.NULL,0);
				CodeToken codeToken=new CodeToken();
				codeToken.token=token;
				
				while(token!=null)
				{
					tokens.add(codeToken);
					codeToken=new CodeToken();
					token=token.getNextToken();
					codeToken.token=token;
				}
			}
		}
		finally
		{
			if(scanner!=null)
			{
				scanner.close();				
			}
		}
	}
}

class CodeToken
{
	public Token token;
	public boolean isMethodStart=false;
	public boolean isMethodStop=false;
}
