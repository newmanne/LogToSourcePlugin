package loginfo;

import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import com.google.common.collect.Lists;

public class Callers {
	
	public static List<Object> execute(IJavaElement method) throws ExecutionException {
		 
		// step 1: Create a search pattern
		// search methods having &quot;abcde&quot; as name
	    SearchPattern pattern = SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES);
 
		// step 2: Create search scope
		// IJavaSearchScope scope = SearchEngine.createJavaSearchScope(packages);
		IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
 
		final List<Object> matches = Lists.newArrayList();
		// step3: define a result collector
		SearchRequestor requestor = new SearchRequestor() {
			public void acceptSearchMatch(SearchMatch match) {
				matches.add(match.getElement());
			}
		};
 
		// step4: start searching
		SearchEngine searchEngine = new SearchEngine();
		try {
			searchEngine
					.search(pattern, new SearchParticipant[] { SearchEngine
							.getDefaultSearchParticipant() }, scope, requestor,
							null /* progress monitor is not used here */);
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return matches;
 	}
}
