package org.reflections.maven.plugin;

import com.google.common.collect.Sets;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.jfrog.jade.plugins.common.injectable.MvnInjectableMojoSupport;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.serializers.JavaCodeSerializer;
import org.reflections.serializers.Serializer;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.reflections.util.Utils.isEmpty;

/** maven plugin for Reflections
 * <p> use it by configuring the pom with:
 * <pre>
 * &#60;build>
 *       &#60;plugins>
 *           &#60;plugin>
 *               &#60;groupId>org.reflections&#60;/groupId>
 *               &#60;artifactId>reflections-maven&#60;/artifactId>
 *               &#60;version>0.9.5&#60;/version>
 *               &#60;executions>
 *                   &#60;execution>
 *                       &#60;goals>
 *                           &#60;goal>reflections&#60;/goal>
 *                       &#60;/goals>
 *                       &#60;phase>process-classes&#60;/phase>
 *                   &#60;/execution>
 *               &#60;/executions>
 *               &#60;configuration>
 *                  <... optional configuration here>
 *               &#60;/configuration>
 *           &#60;/plugin>
 *       &#60;/plugins>
 *   &#60;/build>
 * </pre>
 * <ul>configurations:
 * <li>{@link org.reflections.maven.plugin.ReflectionsMojo#scanners} - a comma separated list of scanner classes names (fully qualified names or simple names).
 * defaults to {@link org.reflections.scanners.TypeAnnotationsScanner}, {@link org.reflections.scanners.SubTypesScanner}
 * <li>{@link org.reflections.maven.plugin.ReflectionsMojo#includeExclude} - a comma separated list of include exclude filters,
 * to be used with {@link org.reflections.util.FilterBuilder} to filter the inputs of all scanners.
 * defaults to "-java\..*, -javax\..*, -sun\..*, -com\.sun\..*"
 * <li>{@link org.reflections.maven.plugin.ReflectionsMojo#destinations} - destination path to save metadata to.
 * defaults to ${project.build.outputDirectory}/META-INF/reflections/${project.artifactId}-reflections.xml
 * <li>{@link org.reflections.maven.plugin.ReflectionsMojo#serializer} - serializer class name to be used for saving (fully qualified names or simple names).
 * defaults to {@link org.reflections.serializers.XmlSerializer}
 * <li>{@link org.reflections.maven.plugin.ReflectionsMojo#parallel} - indicates whether to use parallel scanning of classes, using j.u.c FixedThreadPool,
 * defaults to false
 * <li>{@link org.reflections.maven.plugin.ReflectionsMojo#tests} - If set to true, the mojo will generate the metadata for the test classes as well
 * */
@MojoGoal("reflections")
@MojoPhase("process-classes")
public class ReflectionsMojo extends MvnInjectableMojoSupport {

    @MojoParameter(description = "a comma separated list of scanner classes names (fully qualified names or simple names)." +
            "defaults to {@link org.reflections.scanners.TypeAnnotationsScanner}, {@link org.reflections.scanners.SubTypesScanner}")
    private String scanners;

    private static final String DEFAULT_INCLUDE_EXCLUDE = "-java\\..*, -javax\\..*, -sun\\..*, -com\\.sun\\..*";
    @MojoParameter(description = "a comma separated list of include exclude filters, to be used with {@link org.reflections.util.FilterBuilder} to filter the inputs of all scanners." +
            "defaults to " + DEFAULT_INCLUDE_EXCLUDE
            , defaultValue = DEFAULT_INCLUDE_EXCLUDE)
    private String includeExclude;

    @MojoParameter(description = "destination path to save metadata to." +
            "defaults to ${project.build.outputDirectory/testOutputDirectory}/META-INF/reflections/${project.artifactId}-reflections.xml")
    private String destinations;

    @MojoParameter(description = "serializer class name to be used for saving (fully qualified names or simple names)." +
            "defaults to {@link org.reflections.serializers.XmlSerializer}")
    private String serializer;

    @MojoParameter(description = "indicates whether to use parallel scanning of classes, using j.u.c FixedThreadPool." +
            "default to false"
            , defaultValue = "false")
    private Boolean parallel;

    @MojoParameter(description = "If set to true, the mojo will generate the metadata for the test classes as well", defaultValue = "false")
    private boolean tests;

    public void execute() throws MojoExecutionException, MojoFailureException {
        //
        if (StringUtils.isEmpty(destinations)) {
            destinations = resolveOutputDirectory() + "/META-INF/reflections/" + getProject().getArtifactId() + "-reflections.xml";
        }

        String outputDirectory = resolveOutputDirectory();
        if (!new File(outputDirectory).exists()) {
            getLog().warn(String.format("Reflections plugin is skipping because %s was not found", outputDirectory));
            return;
        }

        //
        ConfigurationBuilder config = new ConfigurationBuilder();

        config.setUrls(parseUrls());

        if (!isEmpty(includeExclude)) {
            config.filterInputsBy(FilterBuilder.parse(includeExclude));
        }

        config.setScanners(!isEmpty(scanners) ? parseScanners() : new Scanner[]{new SubTypesScanner(), new TypeAnnotationsScanner()});

        if (!isEmpty(serializer)) {
            try {
                Serializer serializerInstance = (Serializer) forName(serializer, "org.reflections.serializers").newInstance();
                config.setSerializer(serializerInstance);

                if (serializerInstance instanceof JavaCodeSerializer) {
                    int size = config.getScanners().size();
                    config.addScanners(new TypeElementsScanner());
                    if (size != config.getScanners().size()) {
                        getLog().info("added type scanners for JavaCodeSerializer");
                    }
                }
            } catch (Exception ex) {
                throw new ReflectionsException("could not create serializer instance", ex);
            }
        }

        if (parallel != null && parallel.equals(Boolean.TRUE)) {
            config.useParallelExecutor();
        }

        //
        if (Reflections.log == null) {
            try {
                Reflections.log = new MavenLogAdapter(getLog());
            } catch (Error e) {
                //ignore
            }
        }
        Reflections reflections = new Reflections(config);

        reflections.save(destinations.trim());
    }

    private Set<URL> parseUrls() throws MojoExecutionException {
        final Set<URL> urls = Sets.newHashSet();
        urls.add(parseOutputDirUrl());

        if (!isEmpty(includeExclude)) {
            for (String string : includeExclude.split(",")) {
                String trimmed = string.trim();
                char prefix = trimmed.charAt(0);
                String pattern = trimmed.substring(1);
                if (prefix == '+') {
                    urls.addAll(ClasspathHelper.forPackage(pattern));
                }
            }
        }

        return urls;
    }

    private Scanner[] parseScanners() throws MojoExecutionException {
        Set<Scanner> scannersSet = new HashSet<Scanner>(0);

        if (StringUtils.isNotEmpty(scanners)) {
            String[] scannerClasses = scanners.split(",");
            for (String scannerClass : scannerClasses) {
                try {
                    scannersSet.add((Scanner) forName(scannerClass.trim(), "org.reflections.scanners").newInstance());
                } catch (Exception e) {
                    throw new MojoExecutionException(String.format("error getting scanner %s or org.reflections.scanners.%s", scannerClass.trim(), scannerClass.trim()), e);
                }
            }
        }

        return scannersSet.toArray(new Scanner[scannersSet.size()]);
    }

    @SuppressWarnings({"unchecked"})
    private static <T> Class<T> forName(String name, String... prefixes) throws ClassNotFoundException {
        try {
            return (Class<T>) Class.forName(name.trim());
        } catch (Exception e) {
            if (prefixes != null) {
                for (String prefix : prefixes) {
                    try { return (Class<T>) Class.forName(prefix + "." + name.trim()); }
                    catch (Exception e1) { /*ignore*/ }
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    private URL parseOutputDirUrl() throws MojoExecutionException {
        try {
            File outputDirectoryFile = new File(resolveOutputDirectory() + '/');
            return outputDirectoryFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String resolveOutputDirectory() {
        return tests ? getProject().getBuild().getTestOutputDirectory() : getProject().getBuild().getOutputDirectory();
    }
}
