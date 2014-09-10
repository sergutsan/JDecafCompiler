import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class JDecafCompiler
{
	private static final String JDK="C:\\Program Files\\Java\\jdk1.8.0_05";		//set up path to JDK if needed
	private static final long HEADER_LINES=6;
	private static final long FOOTER_LINES=2;

	public static void main(String[] args)
	{
		if(args.length==0)
		{
			System.out.println("No source .jdc files given...");
			return;
		}

		//get references for the files to be compiled
		DecafFile[] files=new DecafFile[args.length];
		
		for(int i=0;i<args.length;i++)
		{
			DecafFile file=new DecafFile(args[i]);
			
			if(args[i].endsWith(".jdc"))
			{
				try
				{
					files[i]=precompile(file);
				}
				catch(Exception e)
				{
					System.out.println("Error 1: "+e.getMessage()+"\nfile:"+file.getAbsoluteFile());
					//e.printStackTrace();
					System.out.println("*** The process will terminate ***");
					return;				
				}
			}
			else
			{
				files[i]=file;
			}
		}
		
		try
		{
			compile(files);
		}
		catch(Exception e)
		{
			System.out.println("Error 2: "+e.getMessage());
			//e.printStackTrace();
			System.out.println("*** The process will terminate ***");
		}
	}

	private static DecafFile precompile(DecafFile file) throws Exception
	{
		if (file.isDirectory())
		{
		  System.out.println(file.getAbsolutePath()+" is a directory");
		  return null;
		}
		else if (!file.isFile())
		{
		  System.out.println(file.getAbsolutePath()+" is cannot be found");
		  return null;
		}

		Precompiler precompiler=new Precompiler();
		file=precompiler.convert(file);
	
		return file;
	}
	
	private static void compile(DecafFile[] files) throws Exception
	{
		//setup the environment if needed
		String javaHome=System.getProperty("java.home");
		System.setProperty("java.home", JDK);

		//get an object to collect diagnostic messages
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		//get a reference to the compiler
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		//get a file manager for the files to be compiled
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		//prepare the compilation unit
		Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));

		//set compiler's classpath to the current directory
		List<String> optionList=new ArrayList<String>();
		optionList.addAll(Arrays.asList("-classpath","."));

		//compile the files
		compiler.getTask(null, fileManager, diagnostics, optionList, null, compilationUnit).call();

		//display any diagnostic messages generated by the compiler
		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
		{
			long lineNo=diagnostic.getLineNumber();
			String fileName=diagnostic.getSource().toUri().toString();
					
			FileProperties properties=getFileProperties(fileName, files);
			
			if(properties!=null)
			{
				if(lineNo > properties.lines+HEADER_LINES)
				{
					lineNo-=FOOTER_LINES;
				}				
			}
			
			lineNo-=HEADER_LINES;

			System.out.format("Error on line %d in %s%n", lineNo,fileName.replace(".java", ".jdc"));
			System.out.format("Problem: %s%n", diagnostic.getMessage(null));
		}

		try
		{
			fileManager.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		//restore environment if needed
		System.setProperty("java.home", javaHome);
	}
	
	private static FileProperties getFileProperties(String fileName, DecafFile[] files)
	{	
		for(DecafFile file: files)
		{
			if(file.getName().equals(fileName.substring(fileName.lastIndexOf('/')+1)))
			{
				return file.properties;
			}
		}
		
		return null;
	}
}

class DecafFile extends File
{
	public FileProperties properties;
	
	public DecafFile(String name)
	{
		super(name);
		properties=new FileProperties();
	}	
}

class FileProperties
{
	public boolean containsClasses;
	public int lines;
}
