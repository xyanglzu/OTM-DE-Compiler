/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opentravel.schemacompiler.index;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;


/**
 * Process manager that ensures an <code>IndexAgent</code> is running at all times until the manager is shut down. The
 * process manager is also responsible for launching the embedded ActiveMQ broker used by the agent and the OTM
 * repository.
 * 
 * @author S. Livezey
 */
public class IndexProcessManager {

    public static final int FATAL_EXIT_CODE = 69; // service unavailable exit code

    public static final String MANAGER_CONFIG_SYSPROP = "ota2.index.manager.config";
    public static final String MANAGER_JMXPORT_BEANID = "jmxPort";
    public static final String AGENT_CONFIG_SYSPROP = "ota2.index.agent.config";
    public static final String AGENT_JVMOPTS_BEANID = "agentJvmOpts";
    public static final String AMQ_BROKER_BEANID = "amqBroker";

    protected static boolean debugMode = true;

    private static Log log = LogFactory.getLog( IndexProcessManager.class );

    private static BrokerService amqBroker;
    private static boolean shutdownRequested = false;
    private static Thread launcherThread;
    private static AgentLauncher launcher;
    private static String agentJvmOpts;
    private static int jmxPort = -1;
    private static boolean running;

    /**
     * Main method invoked from the command-line.
     * 
     * @param args the command-line arguments (ignored)
     */
    public static void main(String[] args) {
        try {
            running = false;
            initializeContext();
            startActiveMQBroker();
            configureMonitoring();
            running = true;
            log.info( "Indexing process manager started." );

            launcher = new AgentLauncher();
            launcherThread = new Thread( launcher );
            shutdownRequested = false;

            launcherThread.start();
            launcherThread.join();

        } catch (Exception e) {
            log.error( "Error launching index process manager.", e );

        } finally {
            running = false;
        }
    }

    /**
     * Returns the port number where the local host JMX server is running.
     * 
     * @return int
     */
    protected static synchronized int getJmxPort() {
        if (jmxPort < 0) {
            try {
                initializeContext();

            } catch (FileNotFoundException e) {
                log.error( "Error initializing application context (using default JMX port 1099)", e );
                jmxPort = 1099;
            }
        }
        return jmxPort;
    }

    /**
     * Returns true if the process manager is currently running (used for testing purposes).
     * 
     * @return boolean
     */
    public static boolean isRunning() {
        if (!running) {
            try {
                Thread.sleep( 100 );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return running;
    }

    /**
     * Returns a handle to the agent process (used for testing purposes).
     * 
     * @return Process
     */
    public static Process getAgentProcess() {
        return (launcher == null) ? null : launcher.getAgentProcess();
    }

    /**
     * Shuts down the process manager as well as the indexing agent child process.
     */
    public static void shutdown() {
        if ((launcherThread != null) && launcherThread.isAlive()) {
            try {
                shutdownRequested = true;

                if (launcher.getAgentProcess() != null) {
                    launcher.getAgentProcess().destroy();
                }
                launcherThread.interrupt();
                amqBroker.stop();
                amqBroker.waitUntilStopped();
                log.info( "Indexing process manager shut down." );

            } catch (Exception e) {
                log.error( "Error shutting down JMX server", e );
            }
        }
    }

    /**
     * Initializes the Spring application context and all of the properties that are obtained from it. This also has the
     * side-effect of launching the ActiveMQ broker that is configured within the context.
     * 
     * @throws FileNotFoundException thrown if the indexing manager configuration file does not exist in the specified
     *         location
     */
    @SuppressWarnings("resource")
    private static void initializeContext() throws FileNotFoundException {
        String configFileLocation = System.getProperty( MANAGER_CONFIG_SYSPROP );
        File configFile;

        if (configFileLocation == null) {
            throw new FileNotFoundException( "The location of the manager configuration file has not be specified "
                + "(use the 'ota2.index.manager.config' system property)." );
        }
        configFile = new File( configFileLocation );

        if (!configFile.exists() || !configFile.isFile()) {
            throw new FileNotFoundException( "Index manager configuration file not found: " + configFileLocation );
        }
        ApplicationContext context = new FileSystemXmlApplicationContext( configFileLocation );

        agentJvmOpts = (String) context.getBean( AGENT_JVMOPTS_BEANID );
        jmxPort = (Integer) context.getBean( MANAGER_JMXPORT_BEANID );
        amqBroker = (BrokerService) context.getBean( AMQ_BROKER_BEANID );
    }

    /**
     * Starts the embedded ActiveMQ broker that will handle JMS messaging between the indexing agent and the OTM
     * repository server.
     * 
     * @throws IOException thrown if the JMX service cannot be launched
     */
    private static void startActiveMQBroker() throws IOException {
        try {
            amqBroker.start();

        } catch (Exception e) {
            throw new IOException( "Error starting embedded ActiveMQ broker", e );
        }
    }

    /**
     * Configures the MBean used to monitor the server process and exposes the shutdown hook for this manager.
     * 
     * @throws IOException thrown if the JMX service cannot be launched
     */
    private static void configureMonitoring() throws IOException {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName( IndexingManagerStats.MBEAN_NAME );

            if (!mbs.isRegistered( name )) {
                mbs.registerMBean( IndexingManagerStats.getInstance(), name );
            }

        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
            | NotCompliantMBeanException e) {
            throw new IOException( e );
        }

    }

    /**
     * Runner that handles the launching of agent processes.
     */
    private static class AgentLauncher implements Runnable {

        private Process agentProcess;

        /**
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
                boolean agentFatalError = false;
                boolean initialLaunch = true;

                IndexingManagerStats.getInstance().setAvailable( true );

                while (!shutdownRequested) {
                    agentProcess = launchJavaProcess( IndexingAgent.class );
                    IndexingManagerStats.getInstance().agentStarted( !initialLaunch );
                    initialLaunch = false;

                    // Notify all waiting threads when startup is complete
                    synchronized (IndexProcessManager.class) {
                        IndexProcessManager.class.notifyAll();
                    }

                    if (debugMode) {
                        redirectAgentProcessOutout();
                    }
                    int exitCode = agentProcess.waitFor();

                    agentFatalError = (exitCode == FATAL_EXIT_CODE);

                    if (agentFatalError) {
                        log.warn( "The indexing agent encountered a fatal error (see agent log for details)." );
                        shutdown();

                    } else {
                        log.warn( "The indexing agent appears to have crashed - restarting..." );
                    }
                }

            } catch (IOException e) {
                log.fatal( "Fatal exception encountered while trying to launch indexing agent", e );

            } catch (InterruptedException e) {
                log.info( "Indexing agent shut down." );
                Thread.currentThread().interrupt();

            } finally {
                IndexingManagerStats.getInstance().setAvailable( false );
            }
        }

        /**
         * Redirects standard output from the agent sub-process to the standard output of this process.
         */
        @SuppressWarnings("squid:S106")
        private void redirectAgentProcessOutout() {
            try (Reader reader = new InputStreamReader( agentProcess.getInputStream() )) {
                int ch;

                while ((ch = reader.read()) >= 0) {
                    System.out.print( (char) ch );
                }

            } catch (Exception e) {
                log.error( "Error piping sub-process output.", e );
            }
        }

        /**
         * Launches the given Java main class as an external sub-process.
         * 
         * @param mainClass the main class to be executed externally
         * @return Process
         * @throws IOException thrown if the external process cannot be launched
         */
        private static Process launchJavaProcess(Class<?> mainClass) throws IOException {
            String javaCmd =
                System.getProperty( "java.home" ) + File.separatorChar + "bin" + File.separatorChar + "java";
            String agentConfigLocation = System.getProperty( AGENT_CONFIG_SYSPROP );
            String log4jConfig = getAgentLog4jConfiguration();
            List<String> jmxConfig = getAgentJmxConfiguration();
            String oomeOption = getJvmOptionForOutOfMemoryErrors();
            String classpath = System.getProperty( "java.class.path" );

            if (agentConfigLocation == null) {
                throw new FileNotFoundException( "The location of the agent configuration file has not be specified "
                    + "(use the 'ota2.index.agent.config' system property)." );
            }
            agentConfigLocation = "-D" + AGENT_CONFIG_SYSPROP + "=" + agentConfigLocation;

            // For windows, we must wrap all of the path arguments in double quotes in case
            // they contain spaces.
            if (SystemUtils.IS_OS_WINDOWS) {
                javaCmd = "\"" + javaCmd + ".exe\"";
                oomeOption = "\"" + oomeOption + "\"";
                agentConfigLocation = "\"" + agentConfigLocation + "\"";
                log4jConfig = "\"" + log4jConfig + "\"";
                classpath = "\"" + classpath + "\"";

                for (int i = 0; i < jmxConfig.size(); i++) {
                    jmxConfig.set( i, "\"" + jmxConfig.get( i ) + "\"" );
                }
            }

            // Build the list of parameters for the executable command
            List<String> command = new ArrayList<>();

            command.add( javaCmd );

            if (agentJvmOpts != null) {
                command.addAll( Arrays.asList( agentJvmOpts.split( "\\s+" ) ) );
            }
            command.add( oomeOption );
            command.add( agentConfigLocation );
            command.add( log4jConfig );
            command.addAll( jmxConfig );
            command.add( "-cp" );
            command.add( classpath );
            command.add( mainClass.getName() );

            log.info( "Starting indexing agent process..." );
            return new ProcessBuilder().command( command ).redirectOutput( Redirect.PIPE ).start();
        }

        /**
         * Returns the JVM option that will force a shutdown of the JVM if an <code>OutOfMemoryError</code> is
         * encountered in the child process. The options returned by this method reflect the logic implemented in the
         * GemFire open source server.
         * 
         * @return String
         */
        private static String getJvmOptionForOutOfMemoryErrors() {
            String jvmOption = "";

            if (isJVM( "HotSpot" )) {
                if (SystemUtils.IS_OS_WINDOWS) {
                    // ProcessBuilder "on Windows" needs every word (space separated) to be
                    // a different element in the array/list. See #47312. Need to study why!
                    jvmOption = "-XX:OnOutOfMemoryError=taskkill /F /PID %p";

                } else { // All other platforms (Linux, Mac OS X, UNIX, etc)
                    jvmOption = "-XX:OnOutOfMemoryError=kill -KILL %p";
                }

            } else if (SystemUtils.IS_JAVA_9) {
                // NOTE IBM states the following IBM J9 JVM command-line option/switch has
                // side-effects on "performance", as noted in the reference documentation...
                // http://publib.boulder.ibm.com/infocenter/javasdk/v6r0/index.jsp?topic=/com.ibm.java.doc.diagnostics.60/diag/appendixes/cmdline/commands_jvm.html
                jvmOption = "-Xcheck:memory";

            } else if (isJVM( "JRockit" )) {
                // NOTE the following Oracle JRockit JVM documentation was referenced to
                // identify the appropriate JVM option to set when handling OutOfMemoryErrors.
                // http://docs.oracle.com/cd/E13150_01/jrockit_jvm/jrockit/jrdocs/refman/optionXX.html
                jvmOption = "-XXexitOnOutOfMemory";

            } else if (isJVM( "OpenJDK" )) {
                // NOTE this option was added in Java™ SE Development Kit 8, Update 92 (JDK 8u92)
                jvmOption = "-XX:+ExitOnOutOfMemoryError";
            }
            return jvmOption;
        }

        /**
         * @see java.lang.System#getProperty(String) with "java.vm.name".
         */
        private static boolean isJVM(final String expectedJvmName) {
            String jvmName = System.getProperty( "java.vm.name" );
            return (jvmName != null && jvmName.contains( expectedJvmName ));
        }

        /**
         * Returns the location of the log4j configuration file to be used by the indexing agent. If an override has not
         * been provided in the <code>log4j.agent.configuration</code> system property, a default location will be used
         * based on the standard installation directory structure of the indexing service.
         * 
         * @return String
         */
        private static String getAgentLog4jConfiguration() {
            String configFileLocation = System.getProperty( "log4j.agent.configuration" );

            if (configFileLocation == null) {
                configFileLocation = (SystemUtils.IS_OS_WINDOWS ? "file:/" : "file://")
                    + System.getProperty( "user.dir" ) + "/conf/log4j-agent.properties";
            }
            return "-Dlog4j.configuration=" + configFileLocation;
        }

        /**
         * Returns the command line system properties to configure remote monitoring via JMX for the indexing agent
         * process.
         * 
         * @return List&lt;String&gt;
         */
        private static List<String> getAgentJmxConfiguration() {
            String agentJmxPort = System.getProperty( "ota2.index.agent.jmxport" );
            List<String> configProps = new ArrayList<>();

            if (agentJmxPort != null) {
                configProps.add( "-Dcom.sun.management.jmxremote" );
                configProps.add( "-Dcom.sun.management.jmxremote.port=" + agentJmxPort );
                configProps.add( "-Dcom.sun.management.jmxremote.rmi.port=" + agentJmxPort );
                configProps.add( "-Dcom.sun.management.jmxremote.ssl=false" );
                configProps.add( "-Dcom.sun.management.jmxremote.authenticate=false" );
            }
            return configProps;
        }

        /**
         * Returns the handle to the indexing agent process.
         *
         * @return Process
         */
        public Process getAgentProcess() {
            return agentProcess;
        }

    }

}
