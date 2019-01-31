package m3java;

import java.io.PrintWriter;
import java.net.URISyntaxException;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.load.StandardLibraryContributor;
import org.rascalmpl.library.lang.java.m3.internal.EclipseJavaCompiler;
import org.rascalmpl.values.ValueFactoryFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;

public class M3Java {
	private IValueFactory vf = ValueFactoryFactory.getValueFactory();

	/**
	 * Extract the list of method declarations/invocations from a JAR file.
	 *
	 * @param jar An /absolute/path/to/the/Example.jar
	 * @return a multimap mapping each declaration to a list of invocations
	 */
	public Multimap<String, String> extractMethodInvocationsFromJAR(String jar) {
		EclipseJavaCompiler ejc = new EclipseJavaCompiler(vf);
		Evaluator eval = createRascalEvaluator(vf);

		IValue v = ejc.createM3FromJarFile(vf.sourceLocation(jar), eval);
		ISet rel = ((ISet) ((IConstructor) v).asWithKeywordParameters().getParameter("methodInvocation"));

		return convertISetToMultimap(rel);
	}

	/**
	 * Extract the list of method declarations/invocations from an Eclipse project
	 * in the current workspace. Should only be invoked from an Eclipse context, ie.
	 * IWorkspaceRoot should be accessible.
	 *
	 * @param project Simple name of the project in the workspace (ie. "MyProject")
	 * @return a multimap mapping each declaration to a list of invocations
	 * @throws URISyntaxException
	 */
	public Multimap<String, String> extractMethodInvocationsFromEclipseProject(String project)
			throws URISyntaxException {
		org.rascalmpl.eclipse.library.lang.java.jdt.m3.internal.EclipseJavaCompiler ejc =
				new org.rascalmpl.eclipse.library.lang.java.jdt.m3.internal.EclipseJavaCompiler(vf);

		Evaluator eval = createRascalEvaluator(vf);
		ISourceLocation projectLoc = vf.sourceLocation("project", project, "");

		IValue v = ejc.createM3sFromEclipseProject(projectLoc, vf.bool(false), eval);
		ISet rel = ((ISet) ((IConstructor) v).asWithKeywordParameters().getParameter("methodInvocation"));

		return convertISetToMultimap(rel);
	}

	private Multimap<String, String> convertISetToMultimap(ISet set) {
		Multimap<String, String> map = ArrayListMultimap.create();

		set.forEach(e -> {
			ITuple t = (ITuple) e;
			ISourceLocation md = (ISourceLocation) t.get(0);
			ISourceLocation mi = (ISourceLocation) t.get(1);
			map.put(md.toString(), mi.toString());
		});

		return map;
	}

	private Evaluator createRascalEvaluator(IValueFactory vf) {
		GlobalEnvironment heap = new GlobalEnvironment();
		ModuleEnvironment module = new ModuleEnvironment("$m3java$", heap);
		PrintWriter stderr = new PrintWriter(System.err);
		PrintWriter stdout = new PrintWriter(System.out);
		Evaluator eval = new Evaluator(vf, stderr, stdout, module, heap);

		eval.addRascalSearchPathContributor(StandardLibraryContributor.getInstance());

		eval.doImport(null, "lang::java::m3::Core");
		eval.doImport(null, "lang::java::m3::AST");
		eval.doImport(null, "lang::java::m3::TypeSymbol");

		return eval;
	}

	public static void main(String[] args) {
		Multimap<String, String> map = new M3Java().extractMethodInvocationsFromJAR(args[0]);

		map.forEach((decl, invs) -> {
			System.out.println(decl + " -> " + invs);
		});
	}
}
