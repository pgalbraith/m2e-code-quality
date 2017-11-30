package com.basistech.m2e.code.quality.shared;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.embedder.IMaven;

public class AbstractMavenPluginConfigurationTranslator {

	private final IMaven maven;
	private final MavenProject project;
	private final MojoExecution mojoExecution;
	private final IProgressMonitor monitor;
	private final ConfigurationContainer execution;

	public AbstractMavenPluginConfigurationTranslator(final IMaven maven,
	        final MavenProject project, final MojoExecution mojoExecution,
	        final IProgressMonitor monitor) {
		this.maven = maven;
		this.project = project;
		this.mojoExecution = mojoExecution;
		this.monitor = monitor;
		execution = new PluginExecution();
		execution.setConfiguration(mojoExecution.getConfiguration());
	}

	public <T> T getParameterValue(final String parameter,
	        final Class<T> asType) throws CoreException {
		return maven.getMojoParameterValue(project, parameter, asType,
		        mojoExecution.getPlugin(), execution, mojoExecution.getGoal(),
		        monitor);
	}

	public <T> T getParameterValue(final String parameter,
	        final Class<T> asType, final T defaultValue) throws CoreException {
		final T parameterValue = getParameterValue(parameter, asType);
		if (parameterValue == null) {
			return defaultValue;
		}
		return parameterValue;
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getParameterList(final String parameter,
	        @SuppressWarnings("unused") final Class<T> asType)
	        throws CoreException {
		ConfigurationContainer executionClone = execution.clone();
		Xpp3Dom configuration = (Xpp3Dom) executionClone.getConfiguration();
		configuration = configuration != null
		        ? configuration.getChild(parameter) : null;
		if (configuration != null && configuration.getChildCount() > 0) {
			configuration.setAttribute("default-value", "");
		}
		final List<T> list = maven.getMojoParameterValue(project, parameter,
		        List.class, mojoExecution.getPlugin(), executionClone,
		        mojoExecution.getGoal(), monitor);
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}
	protected void copyIfChanged(InputStream input, Path output) throws IOException {
		copyIfChanged(input, output.toFile());
	}

	protected void copyIfChanged(InputStream input, File output) throws IOException {
		byte[] fileContent = IOUtil.toByteArray(input);
		ByteArrayInputStream bufferedInput = new ByteArrayInputStream(fileContent);
		if (output.exists()) {
			// compare content first
			try (InputStream outputContent = new FileInputStream(output)) {
				if (IOUtil.contentEquals(bufferedInput, outputContent)) {
					return;
				}
			}
			// rewind input
			bufferedInput.reset();
		}
		FileUtils.copyStreamToFile(new RawInputStreamFacade(bufferedInput), output);
	}
}
