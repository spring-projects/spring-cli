/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.cli.runtime.engine.actions;

import java.util.Map;

public class ActionResult {

	private Map<String, String> outputs;

	private Throwable error;

	/**
	 * The result of a completed step after continue-on-error is applied.
	 * Possible values are success, failure, or skipped.
	 * When a continue-on-error action fails, the outcome is failure,
	 * but the final conclusion is success.
	 */
	private ActionStatus conclusion;

	/**
	 * The result of a completed step before continue-on-error is applied.
	 * Possible values are success, failure, or skipped.
	 * When a continue-on-error action fails, the outcome is failure,
	 * but the final conclusion is success.
	 */
	private ActionStatus outcome;
}
