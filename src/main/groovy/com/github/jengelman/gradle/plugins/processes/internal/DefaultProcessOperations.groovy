package com.github.jengelman.gradle.plugins.processes.internal

import com.github.jengelman.gradle.plugins.processes.MultipleProcessException
import com.github.jengelman.gradle.plugins.processes.ProcessOperations
import com.github.jengelman.gradle.plugins.processes.ProcessHandle
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.reflect.Instantiator
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecException
import org.gradle.util.ConfigureUtil

class DefaultProcessOperations implements ProcessOperations {

    private final Instantiator instantiator
    private final FileResolver fileResolver

    DefaultProcessOperations(Instantiator instantiator, FileResolver fileResolver) {
        this.instantiator = instantiator
        this.fileResolver = fileResolver
    }

    @Override
    ProcessHandle javafork(Closure cl) {
        JavaForkAction javaForkAction = ConfigureUtil.configure(cl, instantiator.newInstance(DefaultJavaForkAction.class, fileResolver))
        return javaForkAction.fork()
    }

    @Override
    ProcessHandle fork(Closure cl) {
        ForkAction forkAction = ConfigureUtil.configure(cl, instantiator.newInstance(DefaultForkAction.class, fileResolver))
        return forkAction.fork()
    }

    @Override
    ExecResult waitForFinish(ProcessHandle fork) {
        ExecResult result = fork.waitForFinish()
        if (!fork.isIgnoreExitValue()) {
            result.assertNormalExitValue()
        }
        return result
    }

    @Override
    List<ExecResult> waitForFinish(List<ProcessHandle> forks) {
        List<ExecResult> results = new ArrayList<ExecResult>()
        List<Throwable> throwables = new ArrayList<Throwable>()
        forks.each { fork ->
            try {
                ExecResult result = waitForFinish(fork)
                results.add(result)
            } catch (ExecException e) {
                throwables.add(e)
            }
        }
        if (!throwables.isEmpty()) {
            throw new MultipleProcessException(throwables)
        }
        return results
    }
}
