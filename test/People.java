import java.util.Scanner;
public class People
{
		private static Scanner scanner=new Scanner(System.in); static{scanner.useDelimiter(System.getProperty("line.separator"));}
	public static void main(String[] args)
	{
Person p=new Person();
p.id=3;
p.name="mario";
System.out.println(p.id+" "+p.name);

} private static Person[] getPerson()
{
	return new Person[5];
}

}

class Persons
{private static Scanner scanner=new Scanner(System.in); static{scanner.useDelimiter(System.getProperty("line.separator"));}
	public Person p;
	
	public Persons(Person p)
	{
		this.p=p;
	}
	
	public Person getPerson()
	{
		return p;
	}
}
