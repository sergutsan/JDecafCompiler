import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class JDecafCompiler
{
	private static final long HEADER_LINES=Precompiler.getHeaderLineCount();
	private static final long FOOTER_LINES=2;// FIXME: make this a function of the actual footer in Precompiler.java
	private static final String VERSION = "1.3.1";

	public static void main(String[] args)
	{
		if(args.length==0)
		{
			System.out.println("No source .jdc files given...");
			return;
		} else if (Arrays.asList(args).contains("-version")) {
			System.out.println("Java Decaf v" + VERSION + ".");
		} else {
			JDecafCompiler c = new JDecafCompiler(); 
			c.launch(args);
		}
	}
	
	private void launch(String... args) {
		//get references for the files to be compiled
		DecafFile[] files=new DecafFile[args.length];
		
		boolean commandLineArgumentsOK = processCommandLineArguments(args);
		if (!commandLineArgumentsOK) { 
			return;
		}
		
		for(int i=0;i<args.length;i++)
		{
			DecafFile file=new DecafFile(args[i]);

			if(args[i].endsWith(".jdc"))
			{
				try
				{
					files[i]=precompile(file);
				}
				catch (JavaDecafException e) 
				{
					System.out.println("Problem: "+e.getMessage()+"\nfile:"+file.getAbsoluteFile());
					return;				
				} 
				catch(Exception e)
				{
					System.out.println("Error 1: "+e.getMessage()+"\nfile:"+file.getAbsoluteFile());
					System.out.println("*** The process will terminate ***");
					return;				
				}
			}
			else
			{
				/* * * * * * * * * * * * * * * * * * * * * * * * * 
				 * FIXME: remove this useless try/catch ...
				 *   ...but make sure java files can be used in windows machines, 
				 *   and particularly at the BBK labs
				 * * * * * * * * * * * * * * * * * * * * * * * * */
				try {
					Precompiler precompiler=new Precompiler();
					precompiler.convert(file);
				} catch (Exception e) {
					e.printStackTrace();
				}
				/* * * * * * * * * * */
				files[i]=file;
			}
		}
		
		try
		{
			compile(files);
		}
		catch (ConfigurationException e) 
		{
			System.out.println("Configuration Error: " + e.getMessage());
		}
		catch(Exception e)
		{
			System.out.println("Error 2: " + e.getMessage());
			e.printStackTrace();
			System.out.println("*** The process will terminate ***");
		}
	}

	private boolean processCommandLineArguments(String...args) {
		boolean result = true;
		String nonExistingFilenames = "";
		String directoryFilenames = "";
		for(int i=0;i<args.length;i++) {
			DecafFile file=new DecafFile(args[i]);
			if (file.isDirectory())
			{
				result = false;
				directoryFilenames += file.getName() + " ";
			}
			else if (!file.isFile())
			{
				result = false;
				nonExistingFilenames += file.getName() + " ";
			}
		}
		if (nonExistingFilenames.length() != 0) {
			System.out.println("Could not find files: " + nonExistingFilenames);
		}
		if (directoryFilenames.length() != 0) {
			System.out.println("Cannot process directories: " + directoryFilenames);
		}
		return result;
	}

	private DecafFile precompile(DecafFile file) throws Exception
	{
		Precompiler precompiler=new Precompiler();
		file=precompiler.convert(file);
		return file;
	}
	
	private void compile(DecafFile[] files) throws Exception
	{
		boolean hacked = false;
		String hackedJavaHomeBackup=System.getProperty("java.home");
		//get an object to collect diagnostic messages
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		//get a reference to the compiler
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		
		if (compiler == null) {
			/* * * Hack to make this work on BBK's labs for now * * * */
			//System.out.println("No compiler found... using default...");
			 hacked = true;
			 System.setProperty("java.home", "C:\\Program Files\\Java\\jdk1.8.0_20");
			 compiler = ToolProvider.getSystemJavaCompiler();
			 //System.out.println("Compiler: " + compiler);
			 //for (DecafFile file : Arrays.asList(files)) 
			 //	 System.out.println("File: " + file);
			 /* * * */
			 if (compiler == null) {
			 	 throw new ConfigurationException("No Java compiler available. Please review your local configuration.");
			 }
		}

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
		if (hacked) {
			System.setProperty("java.home", hackedJavaHomeBackup);
		}
	}
	
	private FileProperties getFileProperties(String fileName, DecafFile[] files)
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
	private static final long serialVersionUID = 1233345L;
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

class ConfigurationException extends RuntimeException {
	private static final long serialVersionUID = 221L;
	public ConfigurationException(String s) {
		super(s);
	}
}

