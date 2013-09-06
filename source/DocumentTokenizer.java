import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.GroovyTokenMaker;

public class DocumentTokenizer
{
	private ArrayList<Token> tokens=new ArrayList<Token>();
	private int position;

	public DocumentTokenizer(String text)
	{
		init(text);
		position=0;
	}
	
	public String stringifyPlainCode()
	{
		int position=0;
		boolean stop=false;
		String text="";
		
		while(position<tokens.size()&&stop==false)
		{
			Token token=tokens.get(position++);
			
			if(token.type==TokenTypes.NULL)
			{
				text+='\n';
			}
			else
			{
				String lexeme=token.getLexeme();
				
				if(lexeme.equals("class"))
				{
					stop=true;
				}
				else
				{
					text+=lexeme;
				}
			}
		}

		return text;
	}

	public String stringifyClasses()
	{
		int position=0;
		boolean start=false;
		boolean stop=false;
		String text="";
		
		while(position<tokens.size()&&stop==false)
		{
			Token token=tokens.get(position++);
			
			if(token.type==TokenTypes.NULL)
			{
				if(start==true)
				{
					text+='\n';
				}
			}
			else
			{
				String lexeme=token.getLexeme();
				
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
		Iterator<Token> iterator=tokens.iterator();
		String text="";
		
		while(iterator.hasNext())
		{
			Token token=iterator.next();
			
			if(token.type==TokenTypes.NULL)
			{
				text+="\r\n";
			}
			else
			{
				text+=token.getLexeme();
			}
		}
		
		return text;
	}

	public String stringifyFull()
	{
		Iterator<Token> iterator=tokens.iterator();
		String text="";
		
		while(iterator.hasNext())
		{
			text+=iterator.next().toString();
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
	
	public Token nextToken()
	{
		if(hasMoreTokens()==false)
		{
			return null;
		}
		
		return tokens.get(position++);
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
				
				while(token!=null)
				{
					tokens.add(token);
					token=token.getNextToken();
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
