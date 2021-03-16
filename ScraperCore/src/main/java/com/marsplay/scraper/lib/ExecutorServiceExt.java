package com.marspipeline.scraper.lib;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorServiceExt implements ExecutorService {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ExecutorServiceExt.class);

	protected final ExecutorService executor;

	public ExecutorServiceExt(ExecutorService target) {
		this.executor = target;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(wrap(task, clientTrace(), Thread.currentThread()
				.getName()));
	}

	private <T> Callable<T> wrap(final Callable<T> task,
			final Exception clientStack, String clientThreadName) {
		return () -> {
			try {
				return task.call();
			} catch (Exception e) {
				LOGGER.error(
						"Exception {} in task submitted from thread '{}'. Client stackTrace:",
						e, clientThreadName, clientStack);
				throw e;
			}
		};
	}

	private Exception clientTrace() {
		return new Exception("Client stack trace");
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return tasks.stream().map(this::submit).collect(toList());
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		List<Callable<T>> wrappedTasks = tasks
				.stream()
				.map((task) -> this.wrap(task, clientTrace(), Thread
						.currentThread().getName()))
				.collect(toList());
		return executor.invokeAll(wrappedTasks, timeout, unit);
	}
	@Override
	public void shutdown() {
		LOGGER.info("ExecutorServiceExt.shutdown() is invoked.");
		try {
			executor.shutdown();
			executor.awaitTermination(12, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println("termination interrupted");
		} finally {
			if (!executor.isTerminated()) {
				System.err.println("killing non-finished tasks");
			}
			executor.shutdownNow();
		}
	}
	
	//////////////////////////////////////////////
	// 	Below this all default implementations	//
	//	All my implementations are only above	//
	//////////////////////////////////////////////
	
	@Override
	public void execute(Runnable command) {
		executor.execute(command);
	}



	@Override
	public List<Runnable> shutdownNow() {
		return executor.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return executor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return executor.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return executor.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return executor.submit(task, result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return executor.submit(task);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return executor.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return executor.invokeAny(tasks, timeout, unit);
	}

}
