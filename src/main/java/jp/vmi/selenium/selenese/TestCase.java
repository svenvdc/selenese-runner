package jp.vmi.selenium.selenese;

import org.apache.commons.io.FilenameUtils;

import jp.vmi.junit.result.ITestCase;
import jp.vmi.selenium.selenese.command.Command;
import jp.vmi.selenium.selenese.command.CommandList;
import jp.vmi.selenium.selenese.command.CommandListIterator;
import jp.vmi.selenium.selenese.command.ICommand;
import jp.vmi.selenium.selenese.command.ICommandFactory;
import jp.vmi.selenium.selenese.command.Label;
import jp.vmi.selenium.selenese.inject.DoCommand;
import jp.vmi.selenium.selenese.inject.ExecuteTestCase;
import jp.vmi.selenium.selenese.result.Error;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.subcommand.SubCommandMap;
import jp.vmi.selenium.selenese.utils.LogRecorder;
import jp.vmi.selenium.selenese.utils.StopWatch;

import static jp.vmi.selenium.selenese.result.Success.*;
import static jp.vmi.selenium.selenese.result.Unexecuted.*;

/**
 * test-case object for execution.
 * <p>
 * Supports Selenium IDE flow control plugin.
 * </p>
 * @see <a href="https://github.com/davehunt/selenium-ide-flowcontrol">A flow control plugin for Selenium IDE</a>
 */
public class TestCase implements Selenese, ITestCase {

    private String filename = null;
    private String baseName = "nofile";
    private String name = null;
    private String baseURL = null;

    private final CommandList commandList = new CommandList();

    private final StopWatch stopWatch = new StopWatch();
    private final LogRecorder logRecorder = new LogRecorder();
    private Result result = UNEXECUTED;

    @Deprecated
    private Context context = null;

    /**
     * Initialize after constructed.
     *
     * @param filename selenese script filename. (This base name is used for generating screenshot file)
     * @param name test-case name.
     * @param runner Runner instance.
     * @param baseURL effective base URL.
     * @return this.
     */
    @Deprecated
    public TestCase initialize(String filename, String name, Runner runner, String baseURL) {
        TestCase testCase = initialize(filename, name, baseURL);
        testCase.setContext(runner);
        return testCase;
    }

    /**
     * Initialize after constructed.
     *
     * @param filename selenese script filename. (This base name is used for generating screenshot file)
     * @param name test-case name.
     * @param baseURL effective base URL.
     * @return this.
     */
    public TestCase initialize(String filename, String name, String baseURL) {
        this.filename = filename;
        if (filename != null)
            this.baseName = FilenameUtils.getBaseName(filename);
        this.name = name;
        this.baseURL = baseURL.replaceFirst("/+$", ""); // remove trailing "/".
        return this;
    }

    /**
     * Set Selenese Runner context for backward compatibility.
     *
     * @param context Selenese Runner context.
     */
    @Deprecated
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public Type getType() {
        return Type.TEST_CASE;
    }

    @Override
    public boolean isError() {
        return false;
    }

    /**
     * Get filename of test-case.
     *
     * @return filename.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Get base name for screenshot file name.
     *
     * @return base name
     */
    public String getBaseName() {
        return baseName;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Get base URL in the test-case.
     *
     * @return base URL.
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Set SubCommandMap instance for backward compatibility.
     * 
     * @param proc SubCommandMap intance. 
     */
    @Deprecated
    public void setProc(SubCommandMap proc) {
        this.context = proc.getContext();
    }

    /**
     * Get SubCommandMap instance generated at initialize.
     *
     * @return SubCommandMap instance.
     */
    @Deprecated
    public SubCommandMap getProc() {
        return context.getSubCommandMap();
    }

    /**
     * Get command list.
     * 
     * @return command list.
     */
    public CommandList getCommandList() {
        return commandList;
    }

    /**
     * Get stop watch.
     *
     * @return stop watch.
     */
    @Override
    public StopWatch getStopWatch() {
        return stopWatch;
    }

    /**
     * Get log recorder.
     *
     * @return log recorder.
     */
    @Override
    public LogRecorder getLogRecorder() {
        return logRecorder;
    }

    /**
     * Get test-case result.
     *
     * @return test-case result.
     */
    public Result getResult() {
        return result;
    }

    /**
     * Create new collection (FIFO).
     *
     * @param collectionName collection name.
     */
    @Deprecated
    public void addCollection(String collectionName) {
        context.getCollectionMap().addCollection(collectionName);
    }

    /**
     * Add value to collection.
     *
     * @param collectionName collection name.
     * @param value value.
     */
    @Deprecated
    public void addToCollection(String collectionName, String value) {
        context.getCollectionMap().addToCollection(collectionName, value);
    }

    /**
     * Poll value from collection.
     *
     * @param collectionName collection name.
     * @return value.
     */
    @Deprecated
    public String pollFromCollection(String collectionName) {
        return context.getCollectionMap().pollFromCollection(collectionName);
    }

    /**
     * Register label command.
     *
     * @param labelCommand label command.
     */
    @Deprecated
    public void setLabelCommand(Label labelCommand) {
        // no opertion
    }

    /**
     * Add command to command list.
     *
     * @param command command.
     */
    @Deprecated
    public void addCommand(Command command) {
        commandList.add(command);
    }

    /**
     * Add command to command list.
     *
     * @param command command.
     */
    public void addCommand(ICommand command) {
        commandList.add(command);
    }

    /**
     * Add command to command list.
     * 
     * @param commandFactory command factory.
     * @param name command name.
     * @param args command arguments.
     */
    public void addCommand(ICommandFactory commandFactory, String name, String... args) {
        int i = commandList.size();
        ICommand command = commandFactory.newCommand(i, name, args);
        addCommand(command);
    }

    @DoCommand
    protected Result doCommand(Context context, ICommand command, String... curArgs) {
        try {
            return command.execute(context, curArgs);
        } catch (Exception e) {
            return new Error(e);
        }
    }

    @ExecuteTestCase
    @Override
    public Result execute(Selenese parent, Context context) {
        if (commandList.isEmpty())
            return result = SUCCESS;
        context.setCurrentTestCase(this);
        context.getCollectionMap().clear();
        CommandListIterator commandListIterator = commandList.iterator();
        context.setCommandListIterator(commandListIterator);
        while (commandListIterator.hasNext()) {
            ICommand command = commandListIterator.next();
            String[] curArgs = context.getVarsMap().replaceVarsForArray(command.getArguments());
            result = result.update(doCommand(context, command, curArgs));
            if (result.isAborted())
                break;
            context.waitSpeed();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("TestCase[").append(name).append("]");
        if (filename != null)
            s.append(" (").append(filename).append(")");
        return s.toString();
    }
}
