package com.marsplay.scraper.lib;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Clock;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.SystemClock;

import com.google.common.collect.ImmutableList;

public class FluentElementWait extends FluentWait<WebElement> {
	/**
	 * Constructor to initialize Parent class FluentWait
	 * @param element
	 */
	public FluentElementWait(WebElement element) {
		super(element, new SystemClock(), Sleeper.SYSTEM_SLEEPER);
	}

	/**
	 * Sets how long to wait for the evaluated condition to be true. The default
	 * timeout is {@link #FIVE_HUNDRED_MILLIS}.
	 *
	 * @param duration
	 *            The timeout duration.
	 * @param unit
	 *            The unit of time.
	 * @return A self reference.
	 */
	public FluentElementWait withTimeout(long duration, TimeUnit unit) {
		super.withTimeout(duration, unit);
		return this;
	}

	/**
	 * Sets how often the condition should be evaluated.
	 *
	 * <p>
	 * In reality, the interval may be greater as the cost of actually
	 * evaluating a condition function is not factored in. The default polling
	 * interval is {@link #FIVE_HUNDRED_MILLIS}.
	 *
	 * @param duration
	 *            The timeout duration.
	 * @param unit
	 *            The unit of time.
	 * @return A self reference.
	 */
	public FluentElementWait pollingEvery(long duration, TimeUnit unit) {
		super.pollingEvery(duration, unit);
		return this;
	}

	/**
	 * @see #ignoreAll(Collection)
	 * @param firstType
	 *            exception to ignore
	 * @param secondType
	 *            another exception to ignore
	 * @return a self reference
	 */
	public FluentElementWait ignoring(Class<? extends Throwable> firstType,
			Class<? extends Throwable> secondType) {
		ignoreAll(ImmutableList.of(firstType, secondType));
		return this;
	}
}