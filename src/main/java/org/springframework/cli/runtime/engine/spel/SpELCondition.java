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

import java.util.Map;
import java.util.Objects;

import org.springframework.cli.SpringCliException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class SpELCondition {

	private final ExpressionParser parser = new SpelExpressionParser();

	private BeanResolver beanResolver;

	private String expression;

	private ParserContext parserContext;

	public SpELCondition(String expression) {
		this(expression, ParserContext.TEMPLATE_EXPRESSION);
	}
	public SpELCondition(String expression, BeanResolver beanResolver) {
		this(expression, ParserContext.TEMPLATE_EXPRESSION, beanResolver);
	}

	public SpELCondition(String expression, ParserContext parserContext) {
		this(expression, parserContext, null);
	}

	public SpELCondition(String expression, ParserContext parserContext, BeanResolver beanResolver) {
		this.expression = expression;
		this.parserContext = parserContext;
		this.beanResolver = beanResolver;
	}

	public boolean evaluate(Map<String, Object> model) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(model);
		context.setVariables(model);

		if (beanResolver != null) {
			context.setBeanResolver(beanResolver);
		}
		Expression compiledExpression = parser.parseExpression(this.expression, this.parserContext);
		Object expressionValue =  compiledExpression.getValue(context, Object.class);
		if (Objects.isNull(expressionValue)) {
			throw new SpringCliException("'if' expression: '" + this.expression + "' should return boolean.  Instead returned null.");
		}
		if (expressionValue instanceof Boolean boolValue) {
			return boolValue;
		} else {
			throw new SpringCliException("'if' expression: '" + this.expression
					+ "' should return boolean.  Instead returned class = "
					+ expressionValue.getClass() + " with value = " + expressionValue.toString());

		}
	}
}
