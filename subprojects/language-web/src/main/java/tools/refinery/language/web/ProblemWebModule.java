/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * generated by Xtext 2.25.0
 */
package tools.refinery.language.web;

import org.eclipse.xtext.ide.ExecutorServiceProvider;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.model.IWebDocumentProvider;
import org.eclipse.xtext.web.server.model.XtextWebDocumentAccess;
import org.eclipse.xtext.web.server.occurrences.OccurrencesService;
import tools.refinery.language.web.occurrences.ProblemOccurrencesService;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;
import tools.refinery.language.web.xtext.server.push.PushServiceDispatcher;
import tools.refinery.language.web.xtext.server.push.PushWebDocumentAccess;
import tools.refinery.language.web.xtext.server.push.PushWebDocumentProvider;

/**
 * Use this class to register additional components to be used within the web application.
 */
public class ProblemWebModule extends AbstractProblemWebModule {
	public Class<? extends IWebDocumentProvider> bindIWebDocumentProvider() {
		return PushWebDocumentProvider.class;
	}

	public Class<? extends XtextWebDocumentAccess> bindXtextWebDocumentAccess() {
		return PushWebDocumentAccess.class;
	}

	public Class<? extends XtextServiceDispatcher> bindXtextServiceDispatcher() {
		return PushServiceDispatcher.class;
	}

	public Class<? extends OccurrencesService> bindOccurrencesService() {
		return ProblemOccurrencesService.class;
	}

	public Class<? extends ExecutorServiceProvider> bindExecutorServiceProvider() {
		return ThreadPoolExecutorServiceProvider.class;
	}
}
