/*
 * Copyright 2023 the original author or authors.
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
 *
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.springframework.cli.runtime.engine.spel;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;

import static org.assertj.core.api.Assertions.assertThat;

class SpELConditionTests {

	@Test
	void testSpELExpressionEvaluation() {
		// given
		SpELCondition isAdult = new SpELCondition("#{ ['person'].age > 18 }");
		Map<String, Object> map = new HashMap<>();
		map.put("person", new Person("foo", 20));
		// when
		boolean evaluationResult = isAdult.evaluate(map);

		// then
		assertThat(evaluationResult).isTrue();
	}

	@Test
	void whenDeclaredFactIsNotPresent_thenShouldReturnFalse() {
		// given
		SpELCondition isHot = new SpELCondition("#{ ['temperature'] > 30 }");
		Map<String, Object> map = new HashMap<>();

		// when
		boolean evaluationResult = isHot.evaluate(map);

		// then
		assertThat(evaluationResult).isFalse();
	}

	@Test
	void testSpELConditionWithExpressionAndParserContext() {
		// given
		ParserContext context = new TemplateParserContext("%{", "}"); // custom parser
																		// context
		SpELCondition condition = new SpELCondition("%{ T(java.lang.Integer).MAX_VALUE > 1 }", context);
		Map<String, Object> map = new HashMap<>();

		// when
		boolean evaluationResult = condition.evaluate(map);

		// then
		assertThat(evaluationResult).isTrue();
	}

	@Test
	public void testSpELConditionWithExpressionAndParserContextAndBeanResolver() throws Exception {

		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(MySpringAppConfig.class);
		BeanResolver beanResolver = new SimpleBeanResolver(applicationContext);

		SpELCondition condition = new SpELCondition("#{ @myGreeter.greeting(#person.name) }", beanResolver);

		Map<String, Object> map = new HashMap<>();
		map.put("person", new Person("foo", 20));

		Greeter greeter = (Greeter) applicationContext.getBean("myGreeter");

		int numCalls = greeter.getCalls();
		assertThat(numCalls).isEqualTo(0);

		boolean evaluationResult = condition.evaluate(map);
		assertThat(evaluationResult).isTrue();

		numCalls = greeter.getCalls();
		assertThat(numCalls).isEqualTo(1);

	}

}
