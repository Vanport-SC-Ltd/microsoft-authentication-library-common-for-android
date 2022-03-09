//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.controllers;

import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.CANCEL_AUTHORIZATION_REQUEST;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.RESULT_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static com.microsoft.identity.common.java.commands.SilentTokenCommand.ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_EXECUTOR_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_START;

import com.microsoft.identity.common.java.BuildConfig;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.commands.ICommandResult;
import com.microsoft.identity.common.java.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.java.commands.SilentTokenCommand;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.UserCancelException;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.marker.CodeMarkerManager;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.FinalizableResultFuture;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;
import com.microsoft.identity.common.java.result.VoidResult;
import com.microsoft.identity.common.java.telemetry.Telemetry;
import com.microsoft.identity.common.java.util.BiConsumer;
import com.microsoft.identity.common.java.util.IPlatformUtil;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.CANCEL_AUTHORIZATION_REQUEST;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.RESULT_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static com.microsoft.identity.common.java.commands.SilentTokenCommand.ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_EXECUTOR_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_START;

public class CommandDispatcher {

    private static final String TAG = CommandDispatcher.class.getSimpleName();
    private static final int SILENT_REQUEST_THREAD_POOL_SIZE = 5;
    private static final ExecutorService sInteractiveExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService sSilentExecutor = Executors.newFixedThreadPool(SILENT_REQUEST_THREAD_POOL_SIZE);
    private static final Object sLock = new Object();
    private static InteractiveTokenCommand sCommand = null;
    private static final CommandResultCache sCommandResultCache = new CommandResultCache();

    private static final Object mapAccessLock = new Object();

    //@GuardedBy("mapAccessLock")
    //Suppressing rawtype warnings due to the generic type BaseCommand
    @SuppressWarnings(WarningType.rawtype_warning)
    private static ConcurrentMap<BaseCommand, FinalizableResultFuture<CommandResult>> sExecutingCommandMap = new ConcurrentHashMap<>();

    /**
     * Remove all keys that are the command reference from the executing command map.  Since if they key has
     * been changed, remove will not work, construct a new map and add all keys that are not identically
     * that key into the new map.  <strong>MUST</strong> only be used under the mapAccessLock.
     *
     * @param command the command whose identity to use to cleanse the map.
     */
    // Suppressing rawtype warnings due to the generic type BaseCommand
    @SuppressWarnings(WarningType.rawtype_warning)
    private static void cleanMap(BaseCommand command) {
        ConcurrentMap<BaseCommand, FinalizableResultFuture<CommandResult>> newMap = new ConcurrentHashMap<>();
        for (Map.Entry<BaseCommand, FinalizableResultFuture<CommandResult>> e : sExecutingCommandMap.entrySet()) {
            if (!(command == e.getKey())) {
                newMap.put(e.getKey(), e.getValue());
            }
        }
        sExecutingCommandMap = newMap;
    }

    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static int outstandingCommands() {
        synchronized (mapAccessLock) {
            return sExecutingCommandMap.size();
        }
    }

    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static boolean isCommandOutstanding(BaseCommand c) {
        synchronized (mapAccessLock) {
            for (Map.Entry<BaseCommand, ?> e : sExecutingCommandMap.entrySet()) {
                if (e.getKey() == c) {
                    System.out.println("Command out there " + c);
                    return true;
                }
            }
            return false;
        }
    }

    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void clearState() throws Exception {
        synchronized (mapAccessLock) {
            sExecutingCommandMap.clear();
        }
        sSilentExecutor.shutdownNow();
        sInteractiveExecutor.shutdownNow();
        Field f = CommandDispatcher.class.getDeclaredField("sSilentExecutor");
        f.setAccessible(true);
        f.set(null, Executors.newFixedThreadPool(SILENT_REQUEST_THREAD_POOL_SIZE));
        f.setAccessible(false);

        f = CommandDispatcher.class.getDeclaredField("sInteractiveExecutor");
        f.setAccessible(true);
        f.set(null, Executors.newSingleThreadExecutor());
        f.setAccessible(false);
    }

    /**
     * submitSilent - Run a command using the silent thread pool.
     *
     * @param command
     */
    public static void submitSilent(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command) {
        submitSilentReturningFuture(command);
    }


    /**
     * Perform acquireTokenSilent command synchronously.
     *
     * @param command {@link SilentTokenCommand}
     * @return ILocalAuthenticationResult
     * @throws BaseException
     * */
    // TODO: If we want this to be generic, we should make the success type of CommandResult to match with type T from BaseCommand<T>
    //       currently, CommandResult from BaseCommand<AcquireTokenResult> stores "ILocalAuthenticationResult".
    public static ILocalAuthenticationResult submitAcquireTokenSilentSync(@NonNull final SilentTokenCommand command)
            throws BaseException {
        final CommandResult commandResult;
        try {
            if (BuildConfig.DISABLE_ACQUIRE_TOKEN_SILENT_TIMEOUT){
                commandResult = submitSilentReturningFuture(command).get();
            } else {
                commandResult = submitSilentReturningFuture(command).get(ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
            }
        } catch (final InterruptedException | ExecutionException | TimeoutException e) {
            throw ExceptionAdapter.baseExceptionFromException(e);
        }

        if (commandResult.getStatus() == ICommandResult.ResultStatus.COMPLETED){
            return (ILocalAuthenticationResult) commandResult.getResult();
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.ERROR){
            throw ExceptionAdapter.baseExceptionFromException((Throwable) commandResult.getResult());
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.CANCEL){
            throw new UserCancelException(ErrorStrings.USER_CANCELLED,
                    "Request cancelled by user");
        } else {
            throw new ClientException(ErrorStrings.UNKNOWN_ERROR, "Unexpected CommandResult status");
        }
    }

    /**
     * submitSilent - Run a command using the silent thread pool, and return the future governing it.
     *
     * @param command
     */
    //@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static FinalizableResultFuture<CommandResult> submitSilentReturningFuture(@SuppressWarnings(WarningType.rawtype_warning)
                                                                                     @NonNull final BaseCommand command) {
        final CodeMarkerManager codeMarkerManager = CodeMarkerManager.getInstance();
        codeMarkerManager.markCode(ACQUIRE_TOKEN_SILENT_START);
        final String methodName = ":submitSilent";

        final CommandParameters commandParameters = command.getParameters();
        final String correlationId = initializeDiagnosticContext(commandParameters.getCorrelationId(),
                commandParameters.getSdkType() == null ? SdkType.UNKNOWN.getProductName() :
                        commandParameters.getSdkType().getProductName(), commandParameters.getSdkVersion());

        // set correlation id on parameters as it may not already be set
        commandParameters.setCorrelationId(correlationId);

        logParameters(TAG + methodName, correlationId, commandParameters, command.getPublicApiId());

        synchronized (mapAccessLock) {
            final FinalizableResultFuture<CommandResult> finalFuture;
            if (command.isEligibleForCaching()) {
                FinalizableResultFuture<CommandResult> future = sExecutingCommandMap.get(command);

                if (null == future) {
                    future = new FinalizableResultFuture<>();
                    final FinalizableResultFuture<CommandResult> putValue = sExecutingCommandMap.putIfAbsent(command, future);

                    if (null == putValue) {
                        // our value was inserted.
                        future.whenComplete(getCommandResultConsumer(command));
                    } else {
                        // Our value was not inserted, grab the one that was and hang a new listener off it
                        putValue.whenComplete(getCommandResultConsumer(command));
                        return putValue;
                    }
                } else {
                    future.whenComplete(getCommandResultConsumer(command));
                    return future;
                }

                finalFuture = future;
            } else {
                finalFuture = new FinalizableResultFuture<>();
                finalFuture.whenComplete(getCommandResultConsumer(command));
            }

            sSilentExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    codeMarkerManager.markCode(ACQUIRE_TOKEN_SILENT_EXECUTOR_START);
                    try {
                        //initializing again since the request is transferred to a different thread pool
                        initializeDiagnosticContext(correlationId, commandParameters.getSdkType() == null ?
                                        SdkType.UNKNOWN.getProductName() : commandParameters.getSdkType().getProductName(),
                                commandParameters.getSdkVersion());

                        initTelemetryForCommand(command);

                        EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                        CommandResult<?> commandResult = null;

                        //Log operation parameters
                        if (command.getParameters() instanceof SilentTokenCommandParameters) {
                            EstsTelemetry.getInstance().emitForceRefresh(((SilentTokenCommandParameters) command.getParameters()).isForceRefresh());
                        }

                        codeMarkerManager.markCode(ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START);
                        try {
                            commandResult = executeCommand(command);
                        } finally {
                            codeMarkerManager.markCode(ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END);
                        }
                        Logger.info(TAG + methodName, "Completed silent request as owner for correlation id : **"
                                + correlationId + ", with the status : " + commandResult.getStatus().getLogStatus()
                                + " is cacheable : " + command.isEligibleForCaching());
                        // TODO 1309671 : change required to stop the LocalAuthenticationResult object from mutating in cases of cached command.
                        EstsTelemetry.getInstance().flush(command, commandResult);
                        finalFuture.setResult(commandResult);
                    } catch (final Throwable t) {
                        Logger.info(TAG + methodName, "Request encountered an exception with correlation id : **" + correlationId);
                        finalFuture.setException(new ExecutionException(t));
                    } finally {
                        synchronized (mapAccessLock) {
                            if (command.isEligibleForCaching()) {
                                final FinalizableResultFuture mapFuture = sExecutingCommandMap.remove(command);
                                if (mapFuture == null) {
                                    // If this has happened, the command that we started with has mutated.  We will
                                    // examine every entry in the map, find the one with the same object identity
                                    // and remove it.
                                    // ADO:TODO:1153495 - Rekey this map with stable string keys.
                                    Logger.error(TAG, "The command in the map has mutated " + command.getClass().getCanonicalName()
                                            + " the calling application was " + command.getParameters().getApplicationName(), null);
                                    cleanMap(command);
                                }
                            }
                            finalFuture.setCleanedUp();
                        }
                        DiagnosticContext.INSTANCE.clear();
                    }
                    codeMarkerManager.markCode(ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END);
                }
            });
            return finalFuture;
        }
    }

    public static void submitAndForget(@NonNull final BaseCommand command){
        submitAndForgetReturningFuture(command);
    }

    //@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static FinalizableResultFuture<CommandResult> submitAndForgetReturningFuture(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command){
        final String methodName = ":submit";

        final CommandParameters commandParameters = command.getParameters();
        final String correlationId = initializeDiagnosticContext(commandParameters.getCorrelationId(),
                commandParameters.getSdkType() == null ? SdkType.UNKNOWN.getProductName() :
                        commandParameters.getSdkType().getProductName(), commandParameters.getSdkVersion());

        // set correlation id on parameters as it may not already be set
        commandParameters.setCorrelationId(correlationId);

        logParameters(TAG + methodName, correlationId, commandParameters, command.getPublicApiId());
        Logger.info(
                TAG,
                "RefreshOnCommand with CorrelationId: "
                        + correlationId
        );

        synchronized (mapAccessLock) {
            final FinalizableResultFuture<CommandResult> finalFuture = new FinalizableResultFuture<>();
            finalFuture.whenComplete(getCommandResultConsumer(command));
            sSilentExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        //initializing again since the request is transferred to a different thread pool
                        initializeDiagnosticContext(correlationId, commandParameters.getSdkType() == null ?
                                        SdkType.UNKNOWN.getProductName() : commandParameters.getSdkType().getProductName(),
                                commandParameters.getSdkVersion());
                        EstsTelemetry.getInstance().initTelemetryForCommand(command);
                        EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                        CommandResult commandResult = executeCommand(command);
                        Logger.info(TAG + methodName, "Completed as owner for correlation id : **"
                                + correlationId + statusMsg(commandResult.getStatus().getLogStatus())
                                + " is cacheable : " + command.isEligibleForCaching());
                        EstsTelemetry.getInstance().flush(command, commandResult);
                        finalFuture.setResult(commandResult);
                    } catch (final Throwable t) {
                        Logger.info(TAG + methodName, "Request encountered an exception with correlation id : **" + correlationId);
                        finalFuture.setException(new ExecutionException(t));
                    } finally {
                        DiagnosticContext.INSTANCE.clear();
                    }

                }
            });
            return finalFuture;
        }
    }

    private static void initTelemetryForCommand(@NonNull final BaseCommand<?> command) {
        EstsTelemetry.getInstance().setUp(
                command.getParameters().getPlatformComponents());
        EstsTelemetry.getInstance().initTelemetryForCommand(command);
    }

    private static void logParameters(@NonNull String tag, @NonNull String correlationId,
                                      @NonNull Object parameters, @Nullable String publicApiId) {
        final String TAG = tag + ":" + parameters.getClass().getSimpleName();

        //TODO:1315871 - conversion of PublicApiId in readable form.
        Logger.info(TAG, DiagnosticContext.INSTANCE.getRequestContext().toJsonString(),
                "Starting request for correlation id : ##" + correlationId
                        + ", with PublicApiId : " + publicApiId);

        if (Logger.isAllowPii()) {
            Logger.infoPII(TAG, ObjectMapper.serializeObjectToJsonString(parameters));
        } else {
            Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(parameters));
        }
    }

    private static BiConsumer<CommandResult, Throwable> getCommandResultConsumer(
            @SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command) {

        final String methodName = ":getCommandResultConsumer";

        return new BiConsumer<CommandResult, Throwable>() {
            @Override
            public void accept(CommandResult result, final Throwable throwable) {
                if (null != throwable) {
                    Logger.info(TAG + methodName, "Request encountered an exception " +
                            "(this maybe a duplicate request which caries the exception encountered by the original request)");
                    command.getParameters().getPlatformComponents().getPlatformUtil().postCommandResult(
                            new Runnable() {
                                @Override
                                public void run() {
                                    commandCallBackOnError(command, throwable);
                                }
                            });
                    return;
                }

                if (!StringUtil.isNullOrEmpty(result.getCorrelationId())
                        && !command.getParameters().getCorrelationId().equals(result.getCorrelationId())) {
                    Logger.info(TAG + methodName,
                            "Completed duplicate request with correlation id : **"
                                    + command.getParameters().getCorrelationId() + ", having the same result as : "
                                    + result.getCorrelationId() + ", with the status : "
                                    + result.getStatus().getLogStatus());
                }
                // Return command result will post() result for us.
                returnCommandResult(command, result);
            }
        };
    }

    // Suppressing unchecked warnings due to casting of Throwable to the generic type of TaskCompletedCallbackWithError
    @SuppressWarnings(WarningType.unchecked_warning)
    private static void commandCallBackOnError(@SuppressWarnings(WarningType.rawtype_warning) @NonNull BaseCommand command, Throwable throwable) {
        command.getCallback().onError(ExceptionAdapter.baseExceptionFromException(throwable));
    }

    public static void clearCommandCache() {
        sCommandResultCache.clear();
    }

    /**
     * We need to inspect the AcquireTokenResult type to determine whether the request was successful, cancelled or encountered an exception
     * <p>
     * Execute the command provided to the command dispatcher
     *
     * @param command
     * @return
     */
    private static CommandResult executeCommand(@SuppressWarnings(WarningType.rawtype_warning) BaseCommand command) {

        Object result = null;
        BaseException baseException = null;
        CommandResult<?> commandResult = null;

        try {
            //Try executing request
            result = command.execute();
        } catch (final Exception e) {
            if (e instanceof BaseException) {
                baseException = (BaseException) e;
            } else {
                baseException = ExceptionAdapter.baseExceptionFromException(e);
            }
        }

        final String correlationId = command.getParameters().getCorrelationId();
        if (baseException != null) {
            if (baseException instanceof UserCancelException) {
                commandResult = CommandResult.ofNull(CommandResult.ResultStatus.CANCEL,
                        correlationId);
            } else {
                //Post On Error
                commandResult = CommandResult.of(CommandResult.ResultStatus.ERROR, baseException,
                        correlationId);
            }
        } else /* baseException == null */ {
            //Handler handler, final BaseCommand command, BaseException baseException, AcquireTokenResult result
            if (result != null && result instanceof AcquireTokenResult) {
                commandResult = getCommandResultFromTokenResult((AcquireTokenResult) result,
                        command.getParameters());
            } else if (result instanceof VoidResult) {
                commandResult = new CommandResult(CommandResult.ResultStatus.VOID, result,
                        command.getParameters().getCorrelationId());
            } else if (result == null) {
                commandResult = CommandResult.ofNull(CommandResult.ResultStatus.COMPLETED, correlationId);
            } else {
                //For commands that don't return an AcquireTokenResult
                commandResult = new CommandResult<>(CommandResult.ResultStatus.COMPLETED, result,
                        correlationId);
            }
        }

        // set correlation id on Local Authentication Result
        setCorrelationIdOnResult(commandResult, correlationId);
        setTelemetryOnResultAndFlush(commandResult, correlationId);
        return commandResult;
    }

    private static void setTelemetryOnResultAndFlush(@NonNull final CommandResult commandResult,
                                                     @NonNull final String correlationId) {
        final List<Map<String, String>> telemetryMap = Telemetry.getInstance().getMap(correlationId);
        commandResult.setTelemetryMap(telemetryMap);

        if (commandResult.getResult() instanceof LocalAuthenticationResult) {
            ((LocalAuthenticationResult) commandResult.getResult()).setTelemetry(telemetryMap);
        } else if (commandResult.getResult() instanceof BaseException) {
            ((BaseException) commandResult.getResult()).setTelemetry(telemetryMap);
        } else if (commandResult.getResult() != null) {
            Logger.verbose(
                    TAG + ":setTelemetryOnResult",
                    "Not setting telemetry on result as result type is " +
                            commandResult.getResult().getClass().getCanonicalName() +
                            " and doesn't support telemetry at this time."
            );
        }

        Telemetry.getInstance().flush(correlationId);
    }

        /**
         * Return the result of the command to the caller via the callback associated with the command
         *
         * @param command
         * @param result
         */
        private static void returnCommandResult (
        @SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command,
        @NonNull final CommandResult result){

            final IPlatformUtil platformUtil = command.getParameters().getPlatformComponents().getPlatformUtil();
            platformUtil.onReturnCommandResult(command);
            platformUtil.postCommandResult(new Runnable() {
                @Override
                public void run() {
                    switch (result.getStatus()) {
                        case ERROR:
                            commandCallbackOnError(command, result);
                            break;
                        case COMPLETED:
                            commandCallbackOnTaskCompleted(command, result);
                            break;
                        case CANCEL:
                            command.getCallback().onCancel();
                            break;
                        default:

                    }
                }
            });
        }


        // Suppressing unchecked warnings due to casting of the result to the generic type of TaskCompletedCallbackWithError
        @SuppressWarnings(WarningType.unchecked_warning)
        private static void commandCallbackOnError (@SuppressWarnings("rawtypes") BaseCommand
        command, CommandResult result){
            command.getCallback().onError(ExceptionAdapter.baseExceptionFromException((Throwable) result.getResult()));
        }

        // Suppressing unchecked warnings due to casting of the result to the generic type of TaskCompletedCallback
        @SuppressWarnings(WarningType.unchecked_warning)
        private static void commandCallbackOnTaskCompleted
        (@SuppressWarnings("rawtypes") BaseCommand command, CommandResult result){
            command.getCallback().onTaskCompleted(result.getResult());
        }

        /**
         * Cache the result of the command (if eligible to do so) in order to protect the service from clients
         * making the requests in a tight loop
         *
         * @param command
         * @param commandResult
         */
        @SuppressWarnings("unused")
        private static void cacheCommandResult
        (@SuppressWarnings(WarningType.rawtype_warning) BaseCommand command,
                CommandResult commandResult){
            if (command.isEligibleForCaching() && eligibleToCache(commandResult)) {
                sCommandResultCache.put(command, commandResult);
            }
        }

    /**
     * Get Commandresult from acquiretokenresult
     *
     * @param result
     */
    private static CommandResult getCommandResultFromTokenResult(@NonNull AcquireTokenResult result, @NonNull CommandParameters commandParameters) {
        //Token Commands
        if (result.getSucceeded()) {
            return new CommandResult<>(CommandResult.ResultStatus.COMPLETED,
                    result.getLocalAuthenticationResult(), commandParameters.getCorrelationId());
        } else {
            //Get MsalException from Authorization and/or Token Error Response
            final BaseException baseException = ExceptionAdapter.exceptionFromAcquireTokenResult(result, commandParameters);
            if (baseException instanceof UserCancelException) {
                return CommandResult.ofNull(CommandResult.ResultStatus.CANCEL, commandParameters.getCorrelationId());
            } else {
                return new CommandResult<>(CommandResult.ResultStatus.ERROR, baseException, commandParameters.getCorrelationId());
            }
        }
    }

        /**
         * Determine if the command result should be cached
         *
         * @param commandResult
         * @return
         */
        private static boolean eligibleToCache ( @NonNull final CommandResult commandResult) {
            final String methodName = ":eligibleToCache";
            switch (commandResult.getStatus()) {
                case ERROR:
                    if (commandResult.getResult() instanceof BaseException) {
                        return ((BaseException) commandResult.getResult()).isCacheable();
                    }
                    Logger.warn(TAG + methodName, "Get status ERROR, but result is not a BaseException");
                    return true;
                case COMPLETED:
                    return true;
                default:
                    return false;
            }
        }

        public static void beginInteractive ( final InteractiveTokenCommand command){
            final String methodName = ":beginInteractive";
            synchronized (sLock) {

                //Cancel interactive request if authorizationInCurrentTask() returns true OR this is a broker request.
                if (LibraryConfiguration.getInstance().isAuthorizationInCurrentTask() || command.getParameters() instanceof BrokerInteractiveTokenCommandParameters) {
                    // Send a broadcast to cancel if any active auth request is present.
                    LocalBroadcaster.INSTANCE.broadcast(CANCEL_AUTHORIZATION_REQUEST, new PropertyBag());
                }

                sInteractiveExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final CommandParameters commandParameters = command.getParameters();
                        final String correlationId = initializeDiagnosticContext(
                                commandParameters.getCorrelationId(),
                                commandParameters.getSdkType() == null ?
                                        SdkType.UNKNOWN.getProductName() : commandParameters.getSdkType().getProductName(),
                                commandParameters.getSdkVersion()
                        );
                        try {
                            // set correlation id on parameters as it may not already be set
                            commandParameters.setCorrelationId(correlationId);

                            logParameters(TAG + methodName, correlationId, commandParameters, command.getPublicApiId());

                            initTelemetryForCommand(command);

                            EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                            final LocalBroadcaster.IReceiverCallback resultReceiver = new LocalBroadcaster.IReceiverCallback() {
                                @Override
                                public void onReceive(@NonNull PropertyBag dataBag) {
                                    completeInteractive(dataBag);
                                }
                            };

                            CommandResult commandResult;

                            LocalBroadcaster.INSTANCE.registerCallback(
                                    RETURN_AUTHORIZATION_REQUEST_RESULT, resultReceiver);

                            sCommand = command;

                            //Try executing request
                            commandResult = executeCommand(command);
                            sCommand = null;

                            LocalBroadcaster.INSTANCE.unregisterCallback(RETURN_AUTHORIZATION_REQUEST_RESULT);

                            Logger.info(TAG + methodName,
                                    "Completed interactive request for correlation id : **" + correlationId +
                                            statusMsg(commandResult.getStatus().getLogStatus()));

                            EstsTelemetry.getInstance().flush(command, commandResult);
                            returnCommandResult(command, commandResult);
                        } finally {
                            DiagnosticContext.INSTANCE.clear();
                        }
                    }
                });
            }
        }

        private static void completeInteractive ( final PropertyBag propertyBag){
            final String methodName = ":completeInteractive";

            int requestCode = propertyBag.<Integer>getOrDefault(REQUEST_CODE, -1);
            int resultCode = propertyBag.<Integer>getOrDefault(RESULT_CODE, -1);

            if (sCommand != null) {
                sCommand.onFinishAuthorizationSession(requestCode, resultCode, propertyBag);
            } else {
                Logger.warn(TAG + methodName, "sCommand is null, No interactive call in progress to complete.");
            }
        }

        public static String initializeDiagnosticContext (
        @Nullable final String requestCorrelationId, final String sdkType, final String sdkVersion){
            final String methodName = ":initializeDiagnosticContext";

            final String correlationId = StringUtil.isNullOrEmpty(requestCorrelationId) ?
                    UUID.randomUUID().toString() :
                    requestCorrelationId;

            final RequestContext rc = new RequestContext();
            rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
            rc.put(PRODUCT, sdkType);
            rc.put(VERSION, sdkVersion);
            DiagnosticContext.INSTANCE.setRequestContext(rc);
            Logger.verbose(
                    TAG + methodName,
                    "Initialized new DiagnosticContext"
            );

            return correlationId;
        }

        public static int getCachedResultCount () {
            return sCommandResultCache.getSize();
        }

        private static void setCorrelationIdOnResult ( @NonNull final CommandResult commandResult,
        @NonNull final String correlationId){
            // set correlation id on Local Authentication Result
            if (commandResult.getResult() != null &&
                    commandResult.getResult() instanceof LocalAuthenticationResult) {
                final LocalAuthenticationResult localAuthenticationResult =
                        (LocalAuthenticationResult) commandResult.getResult();
                localAuthenticationResult.setCorrelationId(correlationId);
            }
        }

        private static String statusMsg (String status){
            return ", with the status : " + status;
        }



}

